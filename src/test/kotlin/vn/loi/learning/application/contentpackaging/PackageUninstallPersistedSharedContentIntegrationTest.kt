package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
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

class PackageUninstallPersistedSharedContentIntegrationTest {

    @Test
    fun `persisted uninstall preserves content referenced by remaining library`() {
        val directory =
            Files.createTempDirectory(
                "persisted-shared-content-uninstall"
            )

        try {
            val repositories =
                createRepositories(
                    directory
                )

            val catalogId =
                PackageCatalogId(
                    "installed-packages"
                )

            val removedPackageId =
                PackageId(
                    "removed-package"
                )

            val preservedPackageId =
                PackageId(
                    "preserved-package"
                )

            val removedLibraryId =
                ContentLibraryId(
                    "removed-library"
                )

            val preservedLibraryId =
                ContentLibraryId(
                    "preserved-library"
                )

            val sharedContentId =
                ContentId(
                    "shared-content"
                )

            val removedOnlyContentId =
                ContentId(
                    "removed-only-content"
                )

            val sharedLearningItemId =
                LearningItemId(
                    "shared-learning-item"
                )

            val removedLearningItemId =
                LearningItemId(
                    "removed-learning-item"
                )

            val sharedContent =
                createContent(
                    id =
                        sharedContentId,
                    primaryText =
                        "Shared content"
                )

            val sharedLearningItem =
                createLearningItem(
                    id =
                        sharedLearningItemId,
                    contentId =
                        sharedContentId
                )

            val preservedLibrary =
                createLibrary(
                    id =
                        preservedLibraryId,
                    name =
                        "Preserved Library",
                    contentIds =
                        setOf(
                            sharedContentId
                        )
                )

            repositories.contentRepository.saveAll(
                listOf(
                    sharedContent,
                    createContent(
                        id =
                            removedOnlyContentId,
                        primaryText =
                            "Removed content"
                    )
                )
            )

            repositories.learningItemRepository.saveAll(
                listOf(
                    sharedLearningItem,
                    createLearningItem(
                        id =
                            removedLearningItemId,
                        contentId =
                            removedOnlyContentId
                    )
                )
            )

            repositories.libraryRepository.saveAll(
                listOf(
                    createLibrary(
                        id =
                            removedLibraryId,
                        name =
                            "Removed Library",
                        contentIds =
                            setOf(
                                sharedContentId,
                                removedOnlyContentId
                            )
                    ),
                    preservedLibrary
                )
            )

            listOf(
                createPackage(
                    id =
                        removedPackageId,
                    name =
                        "Removed Package",
                    libraryIds =
                        setOf(
                            removedLibraryId
                        )
                ),
                createPackage(
                    id =
                        preservedPackageId,
                    name =
                        "Preserved Package",
                    libraryIds =
                        setOf(
                            preservedLibraryId
                        )
                )
            ).forEach(
                repositories.packageRepository::save
            )

            repositories.catalogRepository.save(
                PackageCatalog(
                    id =
                        catalogId,
                    packageIds =
                        setOf(
                            removedPackageId,
                            preservedPackageId
                        )
                )
            )

            createUninstaller(
                directory
            ).execute(
                UninstallContentPackageCommand(
                    catalogId =
                        catalogId,
                    packageId =
                        removedPackageId
                )
            )

            val reopened =
                createRepositories(
                    directory
                )

            assertEquals(
                sharedContent,
                reopened.contentRepository.findById(
                    sharedContentId
                )
            )

            assertEquals(
                sharedLearningItem,
                reopened.learningItemRepository.findById(
                    sharedLearningItemId
                )
            )

            assertNull(
                reopened.contentRepository.findById(
                    removedOnlyContentId
                )
            )

            assertNull(
                reopened.learningItemRepository.findById(
                    removedLearningItemId
                )
            )

            assertNull(
                reopened.libraryRepository.findById(
                    removedLibraryId
                )
            )

            assertEquals(
                preservedLibrary,
                reopened.libraryRepository.findById(
                    preservedLibraryId
                )
            )

            assertNull(
                reopened.packageRepository.findById(
                    removedPackageId
                )
            )

            assertNotNull(
                reopened.packageRepository.findById(
                    preservedPackageId
                )
            )

            assertEquals(
                setOf(
                    preservedPackageId
                ),
                assertNotNull(
                    reopened.catalogRepository.findById(
                        catalogId
                    )
                ).packageIds
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `persisted uninstall preserves library shared by another package`() {
        val directory =
            Files.createTempDirectory(
                "persisted-shared-library-uninstall"
            )

        try {
            val repositories =
                createRepositories(
                    directory
                )

            val catalogId =
                PackageCatalogId(
                    "installed-packages"
                )

            val removedPackageId =
                PackageId(
                    "removed-package"
                )

            val preservedPackageId =
                PackageId(
                    "preserved-package"
                )

            val sharedLibraryId =
                ContentLibraryId(
                    "shared-library"
                )

            val sharedContentId =
                ContentId(
                    "shared-content"
                )

            val sharedLearningItemId =
                LearningItemId(
                    "shared-learning-item"
                )

            val sharedContent =
                createContent(
                    id =
                        sharedContentId,
                    primaryText =
                        "Shared content"
                )

            val sharedLearningItem =
                createLearningItem(
                    id =
                        sharedLearningItemId,
                    contentId =
                        sharedContentId
                )

            val sharedLibrary =
                createLibrary(
                    id =
                        sharedLibraryId,
                    name =
                        "Shared Library",
                    contentIds =
                        setOf(
                            sharedContentId
                        )
                )

            val preservedPackage =
                createPackage(
                    id =
                        preservedPackageId,
                    name =
                        "Preserved Package",
                    libraryIds =
                        setOf(
                            sharedLibraryId
                        )
                )

            repositories.contentRepository.save(
                sharedContent
            )

            repositories.learningItemRepository.save(
                sharedLearningItem
            )

            repositories.libraryRepository.save(
                sharedLibrary
            )

            listOf(
                createPackage(
                    id =
                        removedPackageId,
                    name =
                        "Removed Package",
                    libraryIds =
                        setOf(
                            sharedLibraryId
                        )
                ),
                preservedPackage
            ).forEach(
                repositories.packageRepository::save
            )

            repositories.catalogRepository.save(
                PackageCatalog(
                    id =
                        catalogId,
                    packageIds =
                        setOf(
                            removedPackageId,
                            preservedPackageId
                        )
                )
            )

            createUninstaller(
                directory
            ).execute(
                UninstallContentPackageCommand(
                    catalogId =
                        catalogId,
                    packageId =
                        removedPackageId
                )
            )

            val reopened =
                createRepositories(
                    directory
                )

            assertEquals(
                sharedContent,
                reopened.contentRepository.findById(
                    sharedContentId
                )
            )

            assertEquals(
                sharedLearningItem,
                reopened.learningItemRepository.findById(
                    sharedLearningItemId
                )
            )

            assertEquals(
                sharedLibrary,
                reopened.libraryRepository.findById(
                    sharedLibraryId
                )
            )

            assertNull(
                reopened.packageRepository.findById(
                    removedPackageId
                )
            )

            assertEquals(
                preservedPackage,
                reopened.packageRepository.findById(
                    preservedPackageId
                )
            )

            assertEquals(
                setOf(
                    preservedPackageId
                ),
                assertNotNull(
                    reopened.catalogRepository.findById(
                        catalogId
                    )
                ).packageIds
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun createUninstaller(
        directory: Path
    ): UninstallContentPackageUseCase =
        PersistedLearningPlatformFactory
            .createPersistedUninstaller(
                directory
            )

    private fun createRepositories(
        directory: Path
    ): Repositories =
        Repositories(
            libraryRepository =
                StoreBackedContentLibraryRepository(
                    JsonContentLibraryStore(
                        directory.resolve(
                            "content-libraries.json"
                        )
                    )
                ),
            contentRepository =
                StoreBackedContentRepository(
                    JsonContentStore(
                        directory.resolve(
                            "contents.json"
                        )
                    )
                ),
            learningItemRepository =
                StoreBackedLearningItemRepository(
                    JsonLearningItemStore(
                        directory.resolve(
                            "learning-items.json"
                        )
                    )
                ),
            packageRepository =
                StoreBackedContentPackageRepository(
                    JsonContentPackageStore(
                        directory.resolve(
                            "content-packages.json"
                        )
                    )
                ),
            catalogRepository =
                StoreBackedPackageCatalogRepository(
                    JsonPackageCatalogStore(
                        directory.resolve(
                            "package-catalogs.json"
                        )
                    )
                )
        )

    private fun createContent(
        id: ContentId,
        primaryText: String
    ): Content =
        Content(
            id =
                id,
            type =
                ContentType.SENTENCE,
            text =
                ContentText(
                    primaryText =
                        primaryText
                )
        )

    private fun createLearningItem(
        id: LearningItemId,
        contentId: ContentId
    ): LearningItem =
        LearningItem(
            id =
                id,
            contentId =
                contentId,
            mode =
                LearningMode.MEANING_RECOGNITION
        )

    private fun createLibrary(
        id: ContentLibraryId,
        name: String,
        contentIds: Set<ContentId>
    ): ContentLibrary =
        ContentLibrary(
            id =
                id,
            descriptor =
                LibraryDescriptor(
                    name =
                        name
                ),
            contentIds =
                contentIds
        )

    private fun createPackage(
        id: PackageId,
        name: String,
        libraryIds: Set<ContentLibraryId>
    ): ContentPackage =
        ContentPackage(
            id =
                id,
            descriptor =
                PackageDescriptor(
                    name =
                        name,
                    version =
                        "1.0.0",
                    format =
                        "OPD3"
                ),
            libraryIds =
                libraryIds
        )

    private data class Repositories(
        val libraryRepository:
        StoreBackedContentLibraryRepository,
        val contentRepository:
        StoreBackedContentRepository,
        val learningItemRepository:
        StoreBackedLearningItemRepository,
        val packageRepository:
        StoreBackedContentPackageRepository,
        val catalogRepository:
        StoreBackedPackageCatalogRepository
    )
}