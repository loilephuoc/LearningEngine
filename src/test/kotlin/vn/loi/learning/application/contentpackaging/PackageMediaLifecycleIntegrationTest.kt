package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Comparator
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.contentpackaging.BundlePackageReader
import vn.loi.learning.infrastructure.contentpackaging.ContentPackageImportFactory
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3ArchiveReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3EntryReader
import vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor
import vn.loi.learning.infrastructure.contentpackaging.PackageBundleImporter
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageMediaLifecycleIntegrationTest {

    @Test
    fun `uninstall package removes metadata and physical media namespace then reimport succeeds`() {
        val tempDir = Files.createTempDirectory("uninstall-reimport-test-")
        var app: vn.loi.learning.infrastructure.LearningApplicationContext? = null
        try {
            val dataDir = tempDir.resolve("data")
            val mediaDir = dataDir.resolve("media")
            app = LearningApplicationFactory.createPersisted(dataDir, false)

            // Step 1: Create and import Package B
            val packageBFile = tempDir.resolve("PackageB.opd3")
            createOpd3(
                packageBFile,
                name = "PackageB",
                mediaFiles = mapOf("media/PackageB/audio.mp3" to "AUDIO_B_BYTES", "media/PackageB/image.jpg" to "IMAGE_B_BYTES")
            )

            val importer = ContentPackageImportFactory.createContentImporter(mediaDir, app.installedPackageRepository)
            val imported = importer.importContent(PackageScanCandidate(packageBFile.toString()))
            imported.onCommit?.invoke()

            // Verify B media exists on disk
            val mediaStorage = JvmContentMediaStorage(mediaDir)
            assertTrue(mediaStorage.exists("PackageB/audio.mp3"))
            assertTrue(mediaStorage.exists("PackageB/image.jpg"))

            // Step 2: Uninstall Package B
            app.uninstallContentPackage!!.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("installed-packages"),
                    packageId = PackageId("PackageB")
                )
            )

            // Verify physical media namespace removed
            assertFalse(mediaStorage.exists("PackageB/audio.mp3"))
            assertFalse(Files.exists(mediaDir.resolve("PackageB")))

            // Step 3: Re-import Package B succeeds cleanly
            val reimported = importer.importContent(PackageScanCandidate(packageBFile.toString()))
            reimported.onCommit?.invoke()

            assertTrue(mediaStorage.exists("PackageB/audio.mp3"))
            assertTrue(mediaStorage.exists("PackageB/image.jpg"))
            assertEquals("AUDIO_B_BYTES", Files.readString(mediaStorage.resolve("PackageB/audio.mp3")!!))
        } finally {
            app?.close()
            deleteTree(tempDir)
        }
    }

    @Test
    fun `reimporting package with different bytes safely replaces orphan namespace and removes stale files`() {
        val tempDir = Files.createTempDirectory("different-bytes-test-")
        try {
            val mediaDir = tempDir.resolve("media")
            val mediaStorage = JvmContentMediaStorage(mediaDir)

            // Setup: Orphan namespace with 3 files (representing old Elementary installation)
            mediaStorage.store("Vocabulary_In_Use_Elementary", "images-001.jpg", "OLD_BYTE_VERSION_1".toByteArray())
            mediaStorage.store("Vocabulary_In_Use_Elementary", "audio-001.mp3", "OLD_AUDIO_1".toByteArray())
            mediaStorage.store("Vocabulary_In_Use_Elementary", "stale-orphan-file.jpg", "STALE_ORPHAN".toByteArray())

            val installedRepo = InMemoryInstalledPackageRepository()
            // Elementary is NOT installed

            val packageFile = tempDir.resolve("Elementary.opd3")
            createOpd3(
                packageFile,
                name = "Vocabulary_In_Use_Elementary",
                mediaFiles = mapOf(
                    "media/Vocabulary_In_Use_Elementary/images-001.jpg" to "NEW_COMPRESSED_VERSION_2",
                    "media/Vocabulary_In_Use_Elementary/audio-001.mp3" to "OLD_AUDIO_1"
                )
            )

            val importer = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = Opd3BundleMediaExtractor(archiveReader = JvmOpd3ArchiveReader(), mediaStorage = mediaStorage),
                installedPackageRepository = installedRepo
            )

            val imported = importer.importContent(PackageScanCandidate(packageFile.toString()))
            imported.onCommit?.invoke()

            // Assertions
            assertEquals("NEW_COMPRESSED_VERSION_2", Files.readString(mediaStorage.resolve("Vocabulary_In_Use_Elementary/images-001.jpg")!!))
            assertEquals("OLD_AUDIO_1", Files.readString(mediaStorage.resolve("Vocabulary_In_Use_Elementary/audio-001.mp3")!!))
            // Stale orphan file is gone
            assertNull(mediaStorage.resolve("Vocabulary_In_Use_Elementary/stale-orphan-file.jpg"))
        } finally {
            deleteTree(tempDir)
        }
    }

    @Test
    fun `active package collision protection refuses overwrite with different bytes`() {
        val tempDir = Files.createTempDirectory("active-protection-test-")
        try {
            val mediaDir = tempDir.resolve("media")
            val mediaStorage = JvmContentMediaStorage(mediaDir)
            mediaStorage.store("ActivePackage", "hero.jpg", "AUTHORITATIVE_ACTIVE_BYTES".toByteArray())

            val installedRepo = InMemoryInstalledPackageRepository()
            installedRepo.save(
                InstalledPackage(
                    id = InstalledPackageId("active-1"),
                    packageId = PackageId("ActivePackage"),
                    name = PackageName("ActivePackage"),
                    version = PackageVersion("1.0.0"),
                    state = PackageState.ACTIVE,
                    installedAt = Instant.now(),
                    libraryId = LibraryId("lib-1"),
                    topicId = TopicId("topic-1"),
                    contentCount = 1,
                    learningItemCount = 1
                )
            )

            val packageFile = tempDir.resolve("colliding.opd3")
            createOpd3(
                packageFile,
                name = "ActivePackage",
                mediaFiles = mapOf("media/ActivePackage/hero.jpg" to "COLLIDING_BYTES")
            )

            val importer = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = Opd3BundleMediaExtractor(archiveReader = JvmOpd3ArchiveReader(), mediaStorage = mediaStorage),
                installedPackageRepository = installedRepo
            )

            val imported = importer.importContent(PackageScanCandidate(packageFile.toString()))
            assertFailsWith<IllegalArgumentException> {
                imported.onCommit?.invoke()
            }

            // Original active bytes protected
            assertEquals("AUTHORITATIVE_ACTIVE_BYTES", Files.readString(mediaStorage.resolve("ActivePackage/hero.jpg")!!))
        } finally {
            deleteTree(tempDir)
        }
    }

    @Test
    fun `real production import workflow replaces orphan media namespace through full PackageImportService pipeline`() {
        val tempDir = Files.createTempDirectory("prod-orphan-import-test-")
        var app: vn.loi.learning.infrastructure.LearningApplicationContext? = null
        try {
            val dataDir = tempDir.resolve("data")
            val mediaDir = dataDir.resolve("media")
            val importDir = tempDir.resolve("imports")
            Files.createDirectories(importDir)

            app = LearningApplicationFactory.createPersisted(dataDir, false)

            // Setup: Intermediate is installed in repository
            val intermediatePkg = InstalledPackage(
                id = InstalledPackageId("package-22f82134f24d0aeb6c5b9e95"),
                packageId = PackageId("package-22f82134f24d0aeb6c5b9e95"),
                name = PackageName("Vocabulary in Use Intermediate"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                libraryId = LibraryId("default-library"),
                topicId = TopicId("topic-intermediate"),
                contentCount = 1,
                learningItemCount = 1
            )
            app.installedPackageRepository!!.save(intermediatePkg)

            // Setup: Physical disk has orphan Elementary directory with old bytes
            val mediaStorage = JvmContentMediaStorage(mediaDir)
            mediaStorage.store("Vocabulary_In_Use_Elementary", "images-00335ee5eab092f3e516d38b7d6482743adbe075.jpg", "OLD_ORPHAN_BYTES_ON_DISK".toByteArray())
            mediaStorage.store("Vocabulary_In_Use_Elementary", "stale-file.jpg", "STALE".toByteArray())

            // Setup: Incoming OPD3 package has the SAME filename but DIFFERENT bytes
            val packageFile = importDir.resolve("Vocabulary_In_Use_Elementary.opd3")
            createOpd3(
                packageFile,
                name = "Vocabulary_In_Use_Elementary",
                mediaFiles = mapOf(
                    "media/Vocabulary_In_Use_Elementary/images-00335ee5eab092f3e516d38b7d6482743adbe075.jpg" to "NEW_STREAMED_BYTES_FROM_ARCHIVE"
                )
            )

            // Execute import through the EXACT production entry point used by Android
            val importService = app.packageImporter(importDir)
            val batchResult = importService.importAllDetailed(PackageCatalogId("android-imports"))

            // Assert import succeeded without collision error
            assertTrue(batchResult.failures.isEmpty(), "Expected 0 failures, got: ${batchResult.failures.map { it.message }}")
            assertEquals(1, batchResult.successfulImports.size)

            // Assert final physical file has new bytes and stale file is gone
            val finalResolved = mediaStorage.resolve("Vocabulary_In_Use_Elementary/images-00335ee5eab092f3e516d38b7d6482743adbe075.jpg")
            assertNotNull(finalResolved)
            assertEquals("NEW_STREAMED_BYTES_FROM_ARCHIVE", Files.readString(finalResolved))
            assertNull(mediaStorage.resolve("Vocabulary_In_Use_Elementary/stale-file.jpg"))
        } finally {
            app?.close()
            deleteTree(tempDir)
        }
    }

    @Test
    fun `negative control active installed package collision is rejected through full production import pipeline`() {
        val tempDir = Files.createTempDirectory("prod-active-collision-test-")
        var app: vn.loi.learning.infrastructure.LearningApplicationContext? = null
        try {
            val dataDir = tempDir.resolve("data")
            val mediaDir = dataDir.resolve("media")
            val importDir = tempDir.resolve("imports")
            Files.createDirectories(importDir)

            app = LearningApplicationFactory.createPersisted(dataDir, false)

            // Setup: Elementary is actively installed
            val elementaryPkg = InstalledPackage(
                id = InstalledPackageId("package-elementary"),
                packageId = PackageId("Vocabulary_In_Use_Elementary"),
                name = PackageName("Vocabulary In Use Elementary"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                libraryId = LibraryId("default-library"),
                topicId = TopicId("topic-elementary"),
                contentCount = 1,
                learningItemCount = 1
            )
            app.installedPackageRepository!!.save(elementaryPkg)

            // Setup: Physical disk has active Elementary directory
            val mediaStorage = JvmContentMediaStorage(mediaDir)
            mediaStorage.store("Vocabulary_In_Use_Elementary", "images-00335ee5eab092f3e516d38b7d6482743adbe075.jpg", "ACTIVE_CANONICAL_BYTES".toByteArray())

            // Setup: Incoming OPD3 package has colliding filename with different bytes
            val packageFile = importDir.resolve("Vocabulary_In_Use_Elementary.opd3")
            createOpd3(
                packageFile,
                name = "Vocabulary_In_Use_Elementary",
                mediaFiles = mapOf(
                    "media/Vocabulary_In_Use_Elementary/images-00335ee5eab092f3e516d38b7d6482743adbe075.jpg" to "COLLIDING_BYTES"
                )
            )

            // Execute import through the production entry point
            val importService = app.packageImporter(importDir)
            val batchResult = importService.importAllDetailed(PackageCatalogId("android-imports"))

            // Assert failure due to collision protection
            assertEquals(1, batchResult.failures.size)
            assertTrue(batchResult.failures.first().message.contains("Media destination collision has different bytes"))

            // Assert active media is preserved
            val resolved = mediaStorage.resolve("Vocabulary_In_Use_Elementary/images-00335ee5eab092f3e516d38b7d6482743adbe075.jpg")
            assertNotNull(resolved)
            assertEquals("ACTIVE_CANONICAL_BYTES", Files.readString(resolved))
        } finally {
            app?.close()
            deleteTree(tempDir)
        }
    }

    @Test
    fun `cross package isolation guarantees Package A media is 100 percent untouched during Package B operations`() {
        val tempDir = Files.createTempDirectory("package-isolation-test-")
        try {
            val mediaDir = tempDir.resolve("media")
            val mediaStorage = JvmContentMediaStorage(mediaDir)

            // Setup Package A
            mediaStorage.store("PackageA", "audio/a1.mp3", "PACKAGE_A_AUDIO_1".toByteArray())
            mediaStorage.store("PackageA", "image/a1.jpg", "PACKAGE_A_IMAGE_1".toByteArray())

            // Setup Package B orphan
            mediaStorage.store("PackageB", "audio/b1.mp3", "OLD_B_AUDIO".toByteArray())

            val installedRepo = InMemoryInstalledPackageRepository()
            installedRepo.save(
                InstalledPackage(
                    id = InstalledPackageId("pkg-a-id"),
                    packageId = PackageId("PackageA"),
                    name = PackageName("PackageA"),
                    version = PackageVersion("1.0.0"),
                    state = PackageState.ACTIVE,
                    installedAt = Instant.now(),
                    libraryId = LibraryId("lib-a"),
                    topicId = TopicId("topic-a"),
                    contentCount = 1,
                    learningItemCount = 1
                )
            )

            val packageBFile = tempDir.resolve("PackageB.opd3")
            createOpd3(
                packageBFile,
                name = "PackageB",
                mediaFiles = mapOf("media/PackageB/audio/b1.mp3" to "NEW_B_AUDIO")
            )

            val importer = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = Opd3BundleMediaExtractor(archiveReader = JvmOpd3ArchiveReader(), mediaStorage = mediaStorage),
                installedPackageRepository = installedRepo
            )

            val imported = importer.importContent(PackageScanCandidate(packageBFile.toString()))
            imported.onCommit?.invoke()

            // Package A is 100% byte-for-byte identical
            assertEquals("PACKAGE_A_AUDIO_1", Files.readString(mediaStorage.resolve("PackageA/audio/a1.mp3")!!))
            assertEquals("PACKAGE_A_IMAGE_1", Files.readString(mediaStorage.resolve("PackageA/image/a1.jpg")!!))

            // Package B was updated
            assertEquals("NEW_B_AUDIO", Files.readString(mediaStorage.resolve("PackageB/audio/b1.mp3")!!))
        } finally {
            deleteTree(tempDir)
        }
    }

    private fun createOpd3(
        targetPath: Path,
        name: String,
        mediaFiles: Map<String, String>
    ) {
        ZipOutputStream(Files.newOutputStream(targetPath)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"name": "$name", "version": "1.0.0", "format": "OPD3", "contentCount": 0, "learningItemCount": 0}""".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("contents.json"))
            zip.write("""{"contents":[]}""".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("learning-items.json"))
            zip.write("""{"learningItems":[]}""".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write("""{"name": "$name"}""".toByteArray())
            zip.closeEntry()

            mediaFiles.forEach { (entryPath, content) ->
                zip.putNextEntry(ZipEntry(entryPath))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun deleteTree(path: Path) {
        if (Files.notExists(path)) return
        path.toFile().deleteRecursively()
    }
}
