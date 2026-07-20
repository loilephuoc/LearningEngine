package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageImportServiceTest {

    @Test
    fun `import all scans imports persists and registers package`() {
        val candidate = PackageScanCandidate(
            source = "C:/packages/english.opd3"
        )

        val contentPackage = ContentPackage(
            id = PackageId("package-english"),
            descriptor = PackageDescriptor(
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD3"
            )
        )

        val content = Content(
            id = ContentId("content-aunt"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "aunt",
                translatedText = "co, di"
            )
        )

        val learningItem = LearningItem(
            id = LearningItemId("item-aunt-recognition"),
            contentId = content.id,
            mode = LearningMode.MEANING_RECOGNITION
        )

        val library = ContentLibrary(
            id = ContentLibraryId("library-english"),
            descriptor = LibraryDescriptor("English Library"),
            contentIds = setOf(content.id)
        )

        val contentRepository = InMemoryContentRepository()
        val learningItemRepository = InMemoryLearningItemRepository()
        val contentPackageRepository = InMemoryContentPackageRepository()
        val packageCatalogRepository = InMemoryPackageCatalogRepository()

        var scannerCallCount = 0
        var installedCandidate: PackageScanCandidate? = null
        var importedCandidate: PackageScanCandidate? = null

        val service = PackageImportService(
            packageScanner = PackageScanner {
                scannerCallCount += 1
                listOf(candidate)
            },
            packageInstaller = PackageInstaller { receivedCandidate ->
                installedCandidate = receivedCandidate
                contentPackage
            },
            packageContentImporter = PackageContentImporter { receivedCandidate ->
                importedCandidate = receivedCandidate
                ImportedPackageContent(
                    contents = listOf(content),
                    learningItems = listOf(learningItem),
                    libraries = listOf(library),
                    warnings = listOf("Missing optional example audio.")
                )
            },
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            packageRegistrationOperation = PackageRegistrationOperation(
                contentPackageRepository = contentPackageRepository,
                packageCatalogRepository = packageCatalogRepository
            ),
            transactionRunner = InMemoryTransactionRunner()
        )

        val catalogId = PackageCatalogId("installed-packages")
        val results = service.importAll(catalogId)

        assertEquals(1, scannerCallCount)
        assertEquals(candidate, installedCandidate)
        assertEquals(candidate, importedCandidate)

        assertEquals(content, contentRepository.findById(content.id))
        assertEquals(1, contentRepository.count())

        assertEquals(learningItem, learningItemRepository.findById(learningItem.id))
        assertEquals(1, learningItemRepository.count())

        val persistedPackage = assertNotNull(contentPackageRepository.findById(contentPackage.id))
        assertEquals(contentPackage.id, persistedPackage.id)
        assertEquals(1, persistedPackage.libraryCount)
        assertEquals(1, contentPackageRepository.count())

        val catalog = packageCatalogRepository.findById(catalogId)
        assertNotNull(catalog)
        assertTrue(catalog.contains(contentPackage.id))
        assertEquals(1, catalog.packageCount)

        assertEquals(1, results.size)

        val result = results.single()
        assertEquals(contentPackage.id, result.contentPackage.id)
        assertEquals(1, result.contentPackage.libraryCount)
        assertEquals(1, result.importedLibraryCount)
        assertEquals(1, result.importedContentCount)
        assertEquals(1, result.importedLearningItemCount)
        assertEquals(
            listOf("Missing optional example audio."),
            result.warnings
        )
    }
}



