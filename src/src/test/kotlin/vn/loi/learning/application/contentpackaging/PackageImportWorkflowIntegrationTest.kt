package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.infrastructure.persistence.PersistedLearningPlatformFactory
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.contentpackaging.JvmDirectoryPackageScanner
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3ArchiveReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3EntryReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3PackageDescriptorReader
import vn.loi.learning.infrastructure.contentpackaging.JvmPackageContentImporter
import vn.loi.learning.infrastructure.contentpackaging.Sha256PackageIdGenerator
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentLibraryStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentStore
import vn.loi.learning.infrastructure.persistence.json.JsonLearningItemStore
import vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository

class PackageImportWorkflowIntegrationTest {

    @Test
    fun `imports opd3 package end to end`() {
        val directory = Files.createTempDirectory("opd3-test")

        val packageFile = directory.resolve("english.opd3")

        try {
            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write("""
                {
                  "name":"English Elementary",
                  "version":"1.0.0",
                  "format":"OPD3"
                }
                """.trimIndent().toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("content.json"))
                zip.write("""
                [
                  {
                    "group":"Vocabulary",
                    "section":"Section 1",
                    "lesson":"Lesson 1",
                    "en":"apple",
                    "vi":"qua tao",
                    "audio":"apple.mp3"
                  }
                ]
                """.trimIndent().toByteArray())
                zip.closeEntry()
            }

            val contentRepository = InMemoryContentRepository()
            val learningItemRepository = InMemoryLearningItemRepository()
            val packageRepository = InMemoryContentPackageRepository()
            val catalogRepository = InMemoryPackageCatalogRepository()

            val service = PackageImportService(
                packageScanner = JvmDirectoryPackageScanner(directory),
                packageInstaller = DefaultPackageInstaller(
                    descriptorReader = JvmOpd3PackageDescriptorReader(
                        archiveReader = JvmOpd3ArchiveReader(),
                        entryReader = JvmOpd3EntryReader()
                    ),
                    packageIdGenerator = Sha256PackageIdGenerator()
                ),
                packageContentImporter = JvmPackageContentImporter(
                    archiveReader = JvmOpd3ArchiveReader(),
                    entryReader = JvmOpd3EntryReader()
                ),
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                packageRegistrationOperation = PackageRegistrationOperation(
                    contentPackageRepository = packageRepository,
                    packageCatalogRepository = catalogRepository
                ),
                transactionRunner = InMemoryTransactionRunner()
            )

            val results = service.importAll(
                PackageCatalogId("installed-packages")
            )

            assertEquals(1, results.size)
            assertEquals(1, contentRepository.count())
            assertEquals(5, learningItemRepository.count())
            assertEquals(1, packageRepository.count())
            assertNotNull(catalogRepository.findById(PackageCatalogId("installed-packages")))
        } finally {
            Files.deleteIfExists(packageFile)
            Files.deleteIfExists(directory)
        }
    }
    @Test
    fun `persisted import writes package data to json files`() {
        val rootDirectory = Files.createTempDirectory("opd3-persisted-test")
        val packageDirectory = Files.createDirectory(rootDirectory.resolve("packages"))
        val persistenceDirectory = rootDirectory.resolve("persistence")
        val packageFile = packageDirectory.resolve("english.opd3")

        try {
            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write("""{"name":"English Elementary","version":"1.0.0","format":"OPD3"}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("content.json"))
                zip.write("""[{"group":"Vocabulary","section":"Section 1","lesson":"Lesson 1","en":"apple","vi":"qua tao","audio":"apple.mp3"}]""".toByteArray())
                zip.closeEntry()
            }

            val service = PersistedLearningPlatformFactory.createPersisted(
                persistenceDirectory = persistenceDirectory,
                packageScanner = JvmDirectoryPackageScanner(packageDirectory),
                packageInstaller = DefaultPackageInstaller(
                    descriptorReader = JvmOpd3PackageDescriptorReader(
                        archiveReader = JvmOpd3ArchiveReader(),
                        entryReader = JvmOpd3EntryReader()
                    ),
                    packageIdGenerator = Sha256PackageIdGenerator()
                ),
                packageContentImporter = JvmPackageContentImporter(
                    archiveReader = JvmOpd3ArchiveReader(),
                    entryReader = JvmOpd3EntryReader()
                )
            )

            service.importAll(PackageCatalogId("installed-packages"))

            assertTrue(Files.exists(persistenceDirectory.resolve("contents.json")))
            assertTrue(Files.exists(persistenceDirectory.resolve("learning-items.json")))
            assertTrue(Files.exists(persistenceDirectory.resolve("content-packages.json")))
            assertTrue(Files.exists(persistenceDirectory.resolve("package-catalogs.json")))

            val reopenedContentRepository = vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository(vn.loi.learning.infrastructure.persistence.json.JsonContentStore(persistenceDirectory.resolve("contents.json")))
            val reopenedLearningItemRepository = vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository(vn.loi.learning.infrastructure.persistence.json.JsonLearningItemStore(persistenceDirectory.resolve("learning-items.json")))
            val reopenedContentPackageRepository = vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository(vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore(persistenceDirectory.resolve("content-packages.json")))
            val reopenedPackageCatalogRepository = vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository(vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore(persistenceDirectory.resolve("package-catalogs.json")))

            assertEquals(1, reopenedContentRepository.findAll().size)
            assertEquals(5, reopenedLearningItemRepository.findAllEnabled().size)
            assertEquals(1, reopenedContentPackageRepository.findAll().size)
            assertNotNull(reopenedPackageCatalogRepository.findById(PackageCatalogId("installed-packages")))

            service.importAll(PackageCatalogId("installed-packages"))

            assertEquals(1, reopenedContentRepository.findAll().size)
            assertEquals(5, reopenedLearningItemRepository.findAllEnabled().size)
            assertEquals(1, reopenedContentPackageRepository.findAll().size)
            assertEquals(1, assertNotNull(reopenedPackageCatalogRepository.findById(PackageCatalogId("installed-packages"))).packageCount)
        } finally {
            rootDirectory.toFile().deleteRecursively()
        }
    }
    @Test
    fun `persisted import rolls back all json files when catalog persistence fails`() {
        val rootDirectory = Files.createTempDirectory("opd3-rollback-test")
        val packageDirectory = Files.createDirectory(rootDirectory.resolve("packages"))
        val persistenceDirectory = rootDirectory.resolve("persistence")
        val packageFile = packageDirectory.resolve("english.opd3")
        val contentPath = persistenceDirectory.resolve("contents.json")
        val learningItemPath = persistenceDirectory.resolve("learning-items.json")
        val contentPackagePath = persistenceDirectory.resolve("content-packages.json")
        val packageCatalogPath = persistenceDirectory.resolve("package-catalogs.json")

        try {
            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write("""{"name":"English Elementary","version":"1.0.0","format":"OPD3"}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("content.json"))
                zip.write("""[{"group":"Vocabulary","section":"Section 1","lesson":"Lesson 1","en":"apple","vi":"qua tao","audio":"apple.mp3"}]""".toByteArray())
                zip.closeEntry()
            }

            val service = PersistedLearningPlatformFactory.create(
                packageScanner = JvmDirectoryPackageScanner(packageDirectory),
                packageInstaller = DefaultPackageInstaller(
                    descriptorReader = JvmOpd3PackageDescriptorReader(
                        archiveReader = JvmOpd3ArchiveReader(),
                        entryReader = JvmOpd3EntryReader()
                    ),
                    packageIdGenerator = Sha256PackageIdGenerator()
                ),
                packageContentImporter = JvmPackageContentImporter(
                    archiveReader = JvmOpd3ArchiveReader(),
                    entryReader = JvmOpd3EntryReader()
                ),
                contentLibraryRepository = StoreBackedContentLibraryRepository(JsonContentLibraryStore(rootDirectory.resolve("content-libraries.json"))),
                contentRepository = StoreBackedContentRepository(JsonContentStore(contentPath)),
                learningItemRepository = StoreBackedLearningItemRepository(JsonLearningItemStore(learningItemPath)),
                contentPackageRepository = StoreBackedContentPackageRepository(JsonContentPackageStore(contentPackagePath)),
                packageCatalogRepository = FailingPackageCatalogRepository(StoreBackedPackageCatalogRepository(JsonPackageCatalogStore(packageCatalogPath))),
                transactionRunner = JsonFileTransactionRunner(listOf(contentPath, learningItemPath, contentPackagePath, packageCatalogPath))
            )

            assertFailsWith<IllegalStateException> {
                service.importAll(PackageCatalogId("installed-packages"))
            }

            assertTrue(!Files.exists(contentPath))
            assertTrue(!Files.exists(learningItemPath))
            assertTrue(!Files.exists(contentPackagePath))
            assertTrue(!Files.exists(packageCatalogPath))
        } finally {
            rootDirectory.toFile().deleteRecursively()
        }
    }
}









