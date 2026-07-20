package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class UninstallContentPackageUseCaseTest {

    @Test
    fun `removes package and unregisters it from catalog`() {
        val packageId = PackageId("english-package")
        val catalogId = PackageCatalogId("installed-packages")
        val libraryId = ContentLibraryId("english-library")
        val contentId = ContentId("hello-content")
        val meaningItemId = LearningItemId("hello-meaning")
        val listeningItemId = LearningItemId("hello-listening")
        val contentLibraryRepository = InMemoryContentLibraryRepository()
        val contentRepository = InMemoryContentRepository()
        val learningItemRepository = InMemoryLearningItemRepository()
        val contentPackageRepository = InMemoryContentPackageRepository()
        val packageCatalogRepository = InMemoryPackageCatalogRepository()

        contentLibraryRepository.save(
            ContentLibrary(
                id = libraryId,
                descriptor = LibraryDescriptor(
                    name = "English Library"
                ),
                contentIds = setOf(contentId)
            )
        )

        contentRepository.save(
            Content(
                id = contentId,
                type = ContentType.SENTENCE,
                text = ContentText(
                    primaryText = "Hello.",
                    translatedText = "Xin chào."
                )
            )
        )

        learningItemRepository.save(
            LearningItem(
                id = meaningItemId,
                contentId = contentId,
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        learningItemRepository.save(
            LearningItem(
                id = listeningItemId,
                contentId = contentId,
                mode = LearningMode.LISTENING_RECOGNITION
            )
        )

        contentPackageRepository.save(
            ContentPackage(
                id = packageId,
                descriptor = PackageDescriptor(
                    name = "English Elementary",
                    version = "1.0.0",
                    format = "OPD3"
                ),
                libraryIds = setOf(libraryId)
            )
        )

        packageCatalogRepository.save(
            PackageCatalog(
                id = catalogId,
                packageIds = setOf(packageId)
            )
        )

        val useCase = UninstallContentPackageUseCase(
            uninstallOperation = PackageUninstallOperation(
                contentLibraryRepository = contentLibraryRepository,
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                contentPackageRepository = contentPackageRepository,
                packageCatalogRepository = packageCatalogRepository
            ),
            transactionRunner = InMemoryTransactionRunner()
        )

        useCase.execute(
            UninstallContentPackageCommand(
                catalogId = catalogId,
                packageId = packageId
            )
        )

        assertNull(learningItemRepository.findById(meaningItemId))
        assertNull(learningItemRepository.findById(listeningItemId))
        assertEquals(0, learningItemRepository.count())

        assertNull(contentRepository.findById(contentId))
        assertEquals(0, contentRepository.count())

        assertNull(contentLibraryRepository.findById(libraryId))
        assertEquals(0, contentLibraryRepository.count())

        assertNull(contentPackageRepository.findById(packageId))
        assertEquals(0, contentPackageRepository.count())

        val updatedCatalog =
            assertNotNull(packageCatalogRepository.findById(catalogId))

        assertFalse(updatedCatalog.contains(packageId))
        assertEquals(0, updatedCatalog.packageCount)
        assertEquals(1, packageCatalogRepository.count())
    }
}
