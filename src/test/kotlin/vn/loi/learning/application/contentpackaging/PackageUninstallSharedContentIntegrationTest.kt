package vn.loi.learning.application.contentpackaging

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
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageUninstallSharedContentIntegrationTest {

    @Test
    fun `uninstall preserves content referenced by remaining library`() {
        val repositories =
            Repositories()

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
            content(
                id =
                    sharedContentId,
                text =
                    "Shared"
            )

        repositories.contentRepository.save(
            sharedContent
        )

        repositories.contentRepository.save(
            content(
                id =
                    removedOnlyContentId,
                text =
                    "Removed"
            )
        )

        val sharedLearningItem =
            learningItem(
                id =
                    sharedLearningItemId,
                contentId =
                    sharedContentId
            )

        repositories.learningItemRepository.save(
            sharedLearningItem
        )

        repositories.learningItemRepository.save(
            learningItem(
                id =
                    removedLearningItemId,
                contentId =
                    removedOnlyContentId
            )
        )

        val removedLibrary =
            library(
                id =
                    removedLibraryId,
                name =
                    "Removed Library",
                contentIds =
                    setOf(
                        sharedContentId,
                        removedOnlyContentId
                    )
            )

        val preservedLibrary =
            library(
                id =
                    preservedLibraryId,
                name =
                    "Preserved Library",
                contentIds =
                    setOf(
                        sharedContentId
                    )
            )

        repositories.contentLibraryRepository.save(
            removedLibrary
        )

        repositories.contentLibraryRepository.save(
            preservedLibrary
        )

        repositories.contentPackageRepository.save(
            contentPackage(
                id =
                    removedPackageId,
                name =
                    "Removed Package",
                libraryIds =
                    setOf(
                        removedLibraryId
                    )
            )
        )

        repositories.contentPackageRepository.save(
            contentPackage(
                id =
                    preservedPackageId,
                name =
                    "Preserved Package",
                libraryIds =
                    setOf(
                        preservedLibraryId
                    )
            )
        )

        repositories.packageCatalogRepository.save(
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

        repositories
            .uninstallUseCase()
            .execute(
                UninstallContentPackageCommand(
                    catalogId =
                        catalogId,
                    packageId =
                        removedPackageId
                )
            )

        assertEquals(
            sharedContent,
            repositories.contentRepository.findById(
                sharedContentId
            )
        )

        assertEquals(
            sharedLearningItem,
            repositories.learningItemRepository.findById(
                sharedLearningItemId
            )
        )

        assertNull(
            repositories.contentRepository.findById(
                removedOnlyContentId
            )
        )

        assertNull(
            repositories.learningItemRepository.findById(
                removedLearningItemId
            )
        )

        assertNull(
            repositories.contentLibraryRepository.findById(
                removedLibraryId
            )
        )

        assertEquals(
            preservedLibrary,
            repositories.contentLibraryRepository.findById(
                preservedLibraryId
            )
        )

        assertNull(
            repositories.contentPackageRepository.findById(
                removedPackageId
            )
        )

        assertNotNull(
            repositories.contentPackageRepository.findById(
                preservedPackageId
            )
        )

        assertEquals(
            setOf(
                preservedPackageId
            ),
            assertNotNull(
                repositories.packageCatalogRepository.findById(
                    catalogId
                )
            ).packageIds
        )
    }

    @Test
    fun `uninstall preserves all content when package library is shared`() {
        val repositories =
            Repositories()

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

        val contentId =
            ContentId(
                "shared-content"
            )

        val learningItemId =
            LearningItemId(
                "shared-learning-item"
            )

        val sharedContent =
            content(
                id =
                    contentId,
                text =
                    "Shared"
            )

        val sharedLearningItem =
            learningItem(
                id =
                    learningItemId,
                contentId =
                    contentId
            )

        val sharedLibrary =
            library(
                id =
                    sharedLibraryId,
                name =
                    "Shared Library",
                contentIds =
                    setOf(
                        contentId
                    )
            )

        repositories.contentRepository.save(
            sharedContent
        )

        repositories.learningItemRepository.save(
            sharedLearningItem
        )

        repositories.contentLibraryRepository.save(
            sharedLibrary
        )

        repositories.contentPackageRepository.save(
            contentPackage(
                id =
                    removedPackageId,
                name =
                    "Removed Package",
                libraryIds =
                    setOf(
                        sharedLibraryId
                    )
            )
        )

        repositories.contentPackageRepository.save(
            contentPackage(
                id =
                    preservedPackageId,
                name =
                    "Preserved Package",
                libraryIds =
                    setOf(
                        sharedLibraryId
                    )
            )
        )

        repositories.packageCatalogRepository.save(
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

        repositories
            .uninstallUseCase()
            .execute(
                UninstallContentPackageCommand(
                    catalogId =
                        catalogId,
                    packageId =
                        removedPackageId
                )
            )

        assertEquals(
            sharedContent,
            repositories.contentRepository.findById(
                contentId
            )
        )

        assertEquals(
            sharedLearningItem,
            repositories.learningItemRepository.findById(
                learningItemId
            )
        )

        assertEquals(
            sharedLibrary,
            repositories.contentLibraryRepository.findById(
                sharedLibraryId
            )
        )

        assertNull(
            repositories.contentPackageRepository.findById(
                removedPackageId
            )
        )

        assertNotNull(
            repositories.contentPackageRepository.findById(
                preservedPackageId
            )
        )
    }

    private fun content(
        id: ContentId,
        text: String
    ): Content =
        Content(
            id =
                id,
            type =
                ContentType.SENTENCE,
            text =
                ContentText(
                    primaryText =
                        text
                )
        )

    private fun learningItem(
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

    private fun library(
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

    private fun contentPackage(
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

    private class Repositories {

        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        fun uninstallUseCase(): UninstallContentPackageUseCase =
            UninstallContentPackageUseCase(
                uninstallOperation =
                    PackageUninstallOperation(
                        contentLibraryRepository =
                            contentLibraryRepository,
                        contentRepository =
                            contentRepository,
                        learningItemRepository =
                            learningItemRepository,
                        contentPackageRepository =
                            contentPackageRepository,
                        packageCatalogRepository =
                            packageCatalogRepository
                    ),
                transactionRunner =
                    InMemoryTransactionRunner()
            )
    }
}