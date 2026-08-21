package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import vn.loi.learning.infrastructure.contentpackaging.BundlePackageReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3ArchiveReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3EntryReader
import vn.loi.learning.infrastructure.contentpackaging.PackageBundleImporter

class PackageBundleImporterTest {

    private val importer =
        PackageBundleImporter(
            BundlePackageReader(
                JvmOpd3ArchiveReader(),
                JvmOpd3EntryReader()
            )
        )

    @Test
    fun `imports bundle with multiple contents and learning items`() {
        val packageFile =
            Files.createTempFile(
                "multi-content-bundle-",
                ".opd3"
            )

        try {
            createBundle(
                packageFile,
                contentsJson =
                    """
                    {
                      "contents": [
                        {
                          "id": "content-1",
                          "type": "SENTENCE",
                          "primaryText": "Hello world",
                          "translatedText": "Xin chao",
                          "tags": [],
                          "customFields": {}
                        },
                        {
                          "id": "content-2",
                          "type": "SENTENCE",
                          "primaryText": "Good morning",
                          "translatedText": "Chao buoi sang",
                          "tags": [],
                          "customFields": {}
                        }
                      ]
                    }
                    """,
                learningItemsJson =
                    """
                    {
                      "learningItems": [
                        {
                          "id": "item-1",
                          "contentId": "content-1",
                          "mode": "MEANING_RECOGNITION",
                          "isEnabled": true
                        },
                        {
                          "id": "item-2",
                          "contentId": "content-2",
                          "mode": "LISTENING_RECOGNITION",
                          "isEnabled": true
                        }
                      ]
                    }
                    """,
                contentCount = 2,
                learningItemCount = 2
            )

            val result =
                importer.importContent(
                    PackageScanCandidate(
                        source =
                            packageFile.toString()
                    )
                )

            assertNotNull(result)

            assertEquals(
                2,
                result.contents.size
            )

            assertEquals(
                2,
                result.learningItems.size
            )

        } finally {
            Files.deleteIfExists(packageFile)
        }
    }


    @Test
    fun `rejects bundle missing required learning items file`() {
        val packageFile =
            Files.createTempFile(
                "missing-learning-items-",
                ".opd3"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(packageFile)
            ).use { zip ->

                writeEntry(
                    zip,
                    "manifest.json",
                    manifest()
                )

                writeEntry(
                    zip,
                    "contents.json",
                    """
                    {
                      "contents": []
                    }
                    """
                )

                writeEntry(
                    zip,
                    "metadata.json",
                    """
                    {
                      "name": "Broken Bundle"
                    }
                    """
                )
            }

            assertFailsWith<Exception> {
                importer.importContent(
                    PackageScanCandidate(
                        source =
                            packageFile.toString()
                    )
                )
            }

        } finally {
            Files.deleteIfExists(packageFile)
        }
    }


    private fun createBundle(
        file: java.nio.file.Path,
        contentsJson: String,
        learningItemsJson: String,
        contentCount: Int,
        learningItemCount: Int
    ) {
        ZipOutputStream(
            Files.newOutputStream(file)
        ).use { zip ->

            writeEntry(
                zip,
                "manifest.json",
                manifest(
                    contentCount,
                    learningItemCount
                )
            )

            writeEntry(
                zip,
                "contents.json",
                contentsJson
            )

            writeEntry(
                zip,
                "learning-items.json",
                learningItemsJson
            )

            writeEntry(
                zip,
                "metadata.json",
                """
                {
                  "name": "Test Bundle"
                }
                """
            )
        }
    }

    @Test
    fun `extracts media assets under media folder when mediaExtractor is configured`() {
        val tempDir = Files.createTempDirectory("bundle-media-test-")
        val packageFile = tempDir.resolve("media-bundle.opd3")
        val mediaDir = tempDir.resolve("media")

        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir)
            val bundleImporter = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor(
                    archiveReader = JvmOpd3ArchiveReader(),
                    mediaStorage = mediaStorage
                )
            )

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeEntry(zip, "manifest.json", manifest(0, 0))
                writeEntry(zip, "contents.json", """{"contents":[]}""")
                writeEntry(zip, "learning-items.json", """{"learningItems":[]}""")
                writeEntry(zip, "metadata.json", """{"name": "Test Bundle"}""")

                zip.putNextEntry(ZipEntry("media/TestPackage/sample.mp3"))
                zip.write("fake-audio-bytes".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("media/TestPackage/sample.jpg"))
                zip.write("fake-image-bytes".toByteArray())
                zip.closeEntry()
            }

            val result = bundleImporter.importContent(PackageScanCandidate(packageFile.toString()))
            assertNotNull(result)
            assertEquals(null, mediaStorage.resolve("TestPackage/sample.mp3"))
            result.onCommit?.invoke()

            val resolvedAudio = mediaStorage.resolve("TestPackage/sample.mp3")
            assertNotNull(resolvedAudio)

            val resolvedImage = mediaStorage.resolve("TestPackage/sample.jpg")
            assertNotNull(resolvedImage)
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `rejects media entries with path traversal`() {
        val tempDir = Files.createTempDirectory("traversal-media-test-")
        val packageFile = tempDir.resolve("traversal-bundle.opd3")
        val mediaDir = tempDir.resolve("media")

        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir)
            val bundleImporter = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor(
                    archiveReader = JvmOpd3ArchiveReader(),
                    mediaStorage = mediaStorage
                )
            )

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeEntry(zip, "manifest.json", manifest(0, 0))
                writeEntry(zip, "contents.json", """{"contents":[]}""")
                writeEntry(zip, "learning-items.json", """{"learningItems":[]}""")
                writeEntry(zip, "metadata.json", """{"name": "Test Bundle"}""")

                zip.putNextEntry(ZipEntry("media/../evil.mp3"))
                zip.write("bad-bytes".toByteArray())
                zip.closeEntry()
            }

            assertFailsWith<IllegalArgumentException> {
                bundleImporter.importContent(PackageScanCandidate(packageFile.toString()))
            }
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `replaces orphan media namespace when importing package with different bytes`() {
        val tempDir = Files.createTempDirectory("orphan-replace-test-")
        val packageFile = tempDir.resolve("elementary.opd3")
        val mediaDir = tempDir.resolve("media")

        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir)
            mediaStorage.store("Vocabulary_In_Use_Elementary", "images-sample.jpg", "OLD_BYTES".toByteArray())
            mediaStorage.store("Vocabulary_In_Use_Elementary", "stale-file.jpg", "STALE_BYTES".toByteArray())
            assertEquals("OLD_BYTES", Files.readString(mediaStorage.resolve("Vocabulary_In_Use_Elementary/images-sample.jpg")!!))

            val installedRepo = vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository()

            val bundleImporter = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor(
                    archiveReader = JvmOpd3ArchiveReader(),
                    mediaStorage = mediaStorage
                ),
                installedPackageRepository = installedRepo
            )

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeEntry(zip, "manifest.json", manifestNamed("Vocabulary_In_Use_Elementary"))
                writeEntry(zip, "contents.json", """{"contents":[]}""")
                writeEntry(zip, "learning-items.json", """{"learningItems":[]}""")
                writeEntry(zip, "metadata.json", """{"name": "Vocabulary_In_Use_Elementary"}""")

                zip.putNextEntry(ZipEntry("media/Vocabulary_In_Use_Elementary/images-sample.jpg"))
                zip.write("NEW_BYTES".toByteArray())
                zip.closeEntry()
            }

            val result = bundleImporter.importContent(PackageScanCandidate(packageFile.toString()))
            result.onCommit?.invoke()

            val resolved = mediaStorage.resolve("Vocabulary_In_Use_Elementary/images-sample.jpg")
            assertNotNull(resolved)
            assertEquals("NEW_BYTES", Files.readString(resolved))
            assertEquals(null, mediaStorage.resolve("Vocabulary_In_Use_Elementary/stale-file.jpg"))
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `reuses media when orphan namespace has exact same bytes`() {
        val tempDir = Files.createTempDirectory("orphan-reuse-test-")
        val packageFile = tempDir.resolve("same-bytes.opd3")
        val mediaDir = tempDir.resolve("media")

        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir)
            mediaStorage.store("TestPackage", "sample.jpg", "SAME_BYTES".toByteArray())

            val bundleImporter = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor(
                    archiveReader = JvmOpd3ArchiveReader(),
                    mediaStorage = mediaStorage
                ),
                installedPackageRepository = vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository()
            )

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeEntry(zip, "manifest.json", manifestNamed("TestPackage"))
                writeEntry(zip, "contents.json", """{"contents":[]}""")
                writeEntry(zip, "learning-items.json", """{"learningItems":[]}""")
                writeEntry(zip, "metadata.json", """{"name": "TestPackage"}""")

                zip.putNextEntry(ZipEntry("media/TestPackage/sample.jpg"))
                zip.write("SAME_BYTES".toByteArray())
                zip.closeEntry()
            }

            val result = bundleImporter.importContent(PackageScanCandidate(packageFile.toString()))
            result.onCommit?.invoke()

            val resolved = mediaStorage.resolve("TestPackage/sample.jpg")
            assertNotNull(resolved)
            assertEquals("SAME_BYTES", Files.readString(resolved))
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `throws collision error when active installed package has different bytes for same relative path`() {
        val tempDir = Files.createTempDirectory("active-collision-test-")
        val packageFile = tempDir.resolve("active-pkg.opd3")
        val mediaDir = tempDir.resolve("media")

        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir)
            mediaStorage.store("ActivePackage", "sample.jpg", "EXISTING_ACTIVE_BYTES".toByteArray())

            val installedRepo = vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository()
            installedRepo.save(
                vn.loi.learning.domain.library.model.InstalledPackage(
                    id = vn.loi.learning.domain.library.model.InstalledPackageId("active-pkg-1"),
                    packageId = vn.loi.learning.domain.content.packaging.model.PackageId("ActivePackage"),
                    name = vn.loi.learning.domain.library.model.PackageName("ActivePackage"),
                    version = vn.loi.learning.domain.library.model.PackageVersion("1.0.0"),
                    state = vn.loi.learning.domain.library.model.PackageState.ACTIVE,
                    installedAt = java.time.Instant.now(),
                    libraryId = vn.loi.learning.domain.library.model.LibraryId("lib-1"),
                    topicId = vn.loi.learning.domain.content.topic.model.TopicId("topic-1"),
                    contentCount = 1,
                    learningItemCount = 1
                )
            )

            val bundleImporter = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor(
                    archiveReader = JvmOpd3ArchiveReader(),
                    mediaStorage = mediaStorage
                ),
                installedPackageRepository = installedRepo
            )

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeEntry(zip, "manifest.json", manifestNamed("ActivePackage"))
                writeEntry(zip, "contents.json", """{"contents":[]}""")
                writeEntry(zip, "learning-items.json", """{"learningItems":[]}""")
                writeEntry(zip, "metadata.json", """{"name": "ActivePackage"}""")

                zip.putNextEntry(ZipEntry("media/ActivePackage/sample.jpg"))
                zip.write("DIFFERENT_INCOMING_BYTES".toByteArray())
                zip.closeEntry()
            }

            val result = bundleImporter.importContent(PackageScanCandidate(packageFile.toString()))
            assertFailsWith<IllegalArgumentException> {
                result.onCommit?.invoke()
            }

            val resolved = mediaStorage.resolve("ActivePackage/sample.jpg")
            assertNotNull(resolved)
            assertEquals("EXISTING_ACTIVE_BYTES", Files.readString(resolved))
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `preserves unrelated package media when importing package`() {
        val tempDir = Files.createTempDirectory("unrelated-pkg-test-")
        val packageFile = tempDir.resolve("pkg-b.opd3")
        val mediaDir = tempDir.resolve("media")

        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir)
            mediaStorage.store("PackageA", "audio-a.mp3", "PACKAGE_A_AUDIO".toByteArray())
            mediaStorage.store("PackageA", "image-a.jpg", "PACKAGE_A_IMAGE".toByteArray())

            val bundleImporter = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor(
                    archiveReader = JvmOpd3ArchiveReader(),
                    mediaStorage = mediaStorage
                ),
                installedPackageRepository = vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository()
            )

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeEntry(zip, "manifest.json", manifestNamed("PackageB"))
                writeEntry(zip, "contents.json", """{"contents":[]}""")
                writeEntry(zip, "learning-items.json", """{"learningItems":[]}""")
                writeEntry(zip, "metadata.json", """{"name": "PackageB"}""")

                zip.putNextEntry(ZipEntry("media/PackageB/audio-b.mp3"))
                zip.write("PACKAGE_B_AUDIO".toByteArray())
                zip.closeEntry()
            }

            val result = bundleImporter.importContent(PackageScanCandidate(packageFile.toString()))
            result.onCommit?.invoke()

            assertEquals("PACKAGE_A_AUDIO", Files.readString(mediaStorage.resolve("PackageA/audio-a.mp3")!!))
            assertEquals("PACKAGE_A_IMAGE", Files.readString(mediaStorage.resolve("PackageA/image-a.jpg")!!))
            assertEquals("PACKAGE_B_AUDIO", Files.readString(mediaStorage.resolve("PackageB/audio-b.mp3")!!))
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `restores displaced orphan namespace if commit fails and rolls back`() {
        val tempDir = Files.createTempDirectory("orphan-rollback-test-")
        val packageFile = tempDir.resolve("faulty.opd3")
        val mediaDir = tempDir.resolve("media")

        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir)
            mediaStorage.store("FaultyPackage", "old-orphan.jpg", "ORIGINAL_ORPHAN_CONTENT".toByteArray())

            val bundleImporter = PackageBundleImporter(
                bundleReader = BundlePackageReader(JvmOpd3ArchiveReader(), JvmOpd3EntryReader()),
                mediaExtractor = vn.loi.learning.infrastructure.contentpackaging.Opd3BundleMediaExtractor(
                    archiveReader = JvmOpd3ArchiveReader(),
                    mediaStorage = mediaStorage
                ),
                installedPackageRepository = vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository()
            )

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeEntry(zip, "manifest.json", manifestNamed("FaultyPackage"))
                writeEntry(zip, "contents.json", """{"contents":[]}""")
                writeEntry(zip, "learning-items.json", """{"learningItems":[]}""")
                writeEntry(zip, "metadata.json", """{"name": "FaultyPackage"}""")

                zip.putNextEntry(ZipEntry("media/FaultyPackage/new-file.jpg"))
                zip.write("NEW_CONTENT".toByteArray())
                zip.closeEntry()
            }

            val result = bundleImporter.importContent(PackageScanCandidate(packageFile.toString()))
            result.onRollback?.invoke()

            val resolved = mediaStorage.resolve("FaultyPackage/old-orphan.jpg")
            assertNotNull(resolved)
            assertEquals("ORIGINAL_ORPHAN_CONTENT", Files.readString(resolved))
            assertEquals(null, mediaStorage.resolve("FaultyPackage/new-file.jpg"))
        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private fun manifest(
        contentCount: Int = 0,
        learningItemCount: Int = 0,
        name: String = "Test Bundle"
    ): String =
        """
        {
          "name": "$name",
          "version": "1.0.0",
          "format": "OPD3",
          "contentCount": $contentCount,
          "learningItemCount": $learningItemCount
        }
        """

    private fun manifestNamed(
        name: String,
        contentCount: Int = 0,
        learningItemCount: Int = 0
    ): String = manifest(contentCount, learningItemCount, name)

    private fun writeEntry(
        zip: ZipOutputStream,
        name: String,
        content: String
    ) {
        zip.putNextEntry(
            ZipEntry(name)
        )

        zip.write(
            content.trimIndent().toByteArray()
        )

        zip.closeEntry()
    }
}
