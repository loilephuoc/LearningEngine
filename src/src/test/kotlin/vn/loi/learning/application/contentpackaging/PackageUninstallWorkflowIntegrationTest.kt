package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
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
import vn.loi.learning.infrastructure.persistence.PersistedLearningPlatformFactory
import vn.loi.learning.infrastructure.persistence.json.JsonContentLibraryStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentStore
import vn.loi.learning.infrastructure.persistence.json.JsonLearningItemStore
import vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository

class PackageUninstallWorkflowIntegrationTest {

    @Test
    fun `persisted uninstall removes package data from json files`() {
        val directory = Files.createTempDirectory("persisted-uninstall-test")

        try {
            val libraryRepository = StoreBackedContentLibraryRepository(JsonContentLibraryStore(directory.resolve("content-libraries.json")))
            val contentRepository = StoreBackedContentRepository(JsonContentStore(directory.resolve("contents.json")))
            val learningItemRepository = StoreBackedLearningItemRepository(JsonLearningItemStore(directory.resolve("learning-items.json")))
            val packageRepository = StoreBackedContentPackageRepository(JsonContentPackageStore(directory.resolve("content-packages.json")))
            val catalogRepository = StoreBackedPackageCatalogRepository(JsonPackageCatalogStore(directory.resolve("package-catalogs.json")))

            val catalogId = PackageCatalogId("installed-packages")
            val packageId = PackageId("english-package")
            val libraryId = ContentLibraryId("english-library")
            val contentId = ContentId("hello-content")
            val itemId = LearningItemId("hello-item")

            contentRepository.save(Content(id = contentId, type = ContentType.SENTENCE, text = ContentText(primaryText = "Hello.", translatedText = "Xin chao.")))
            learningItemRepository.save(LearningItem(id = itemId, contentId = contentId, mode = LearningMode.MEANING_RECOGNITION))
            libraryRepository.save(ContentLibrary(id = libraryId, descriptor = LibraryDescriptor(name = "English Library"), contentIds = setOf(contentId)))
            packageRepository.save(ContentPackage(id = packageId, descriptor = PackageDescriptor(name = "English Package", version = "1.0.0", format = "OPD3"), libraryIds = setOf(libraryId)))
            catalogRepository.save(PackageCatalog(id = catalogId, packageIds = setOf(packageId)))

            PersistedLearningPlatformFactory.createPersistedUninstaller(directory).execute(UninstallContentPackageCommand(catalogId = catalogId, packageId = packageId))

            val reopenedLibraryRepository = StoreBackedContentLibraryRepository(JsonContentLibraryStore(directory.resolve("content-libraries.json")))
            val reopenedContentRepository = StoreBackedContentRepository(JsonContentStore(directory.resolve("contents.json")))
            val reopenedLearningItemRepository = StoreBackedLearningItemRepository(JsonLearningItemStore(directory.resolve("learning-items.json")))
            val reopenedPackageRepository = StoreBackedContentPackageRepository(JsonContentPackageStore(directory.resolve("content-packages.json")))
            val reopenedCatalogRepository = StoreBackedPackageCatalogRepository(JsonPackageCatalogStore(directory.resolve("package-catalogs.json")))

            assertNull(reopenedLearningItemRepository.findById(itemId))
            assertNull(reopenedContentRepository.findById(contentId))
            assertNull(reopenedLibraryRepository.findById(libraryId))
            assertNull(reopenedPackageRepository.findById(packageId))
            assertEquals(0, assertNotNull(reopenedCatalogRepository.findById(catalogId)).packageCount)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
