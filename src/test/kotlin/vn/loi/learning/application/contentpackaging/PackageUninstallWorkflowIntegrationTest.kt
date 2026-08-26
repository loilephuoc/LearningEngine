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

class PackageUninstallWorkflowIntegrationTest {

    @Test
    fun `persisted uninstall removes package data from json files`() {
        val directory =
            Files.createTempDirectory(
                "persisted-uninstall-test"
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

            val packageId =
                PackageId(
                    "english-package"
                )

            val libraryId =
                ContentLibraryId(
                    "english-library"
                )

            val contentId =
                ContentId(
                    "hello-content"
                )

            val itemId =
                LearningItemId(
                    "hello-item"
                )

            repositories.contentRepository.save(
                createContent(
                    id = contentId,
                    primaryText = "Hello."
                )
            )

            repositories.learningItemRepository.save(
                createLearningItem(
                    id = itemId,
                    contentId = contentId
                )
            )

            repositories.libraryRepository.save(
                createLibrary(
                    id = libraryId,
                    name = "English Library",
                    contentIds = setOf(
                        contentId
                    )
                )
            )

            repositories.packageRepository.save(
                createPackage(
                    id = packageId,
                    name = "English Package",
                    libraryIds = setOf(
                        libraryId
                    )
                )
            )

            repositories.catalogRepository.save(
                PackageCatalog(
                    id = catalogId,
                    packageIds = setOf(
                        packageId
                    )
                )
            )

            createUninstaller(
                directory
            ).execute(
                UninstallContentPackageCommand(
                    catalogId = catalogId,
                    packageId = packageId
                )
            )

            val reopened =
                createRepositories(
                    directory
                )

            assertNull(
                reopened.learningItemRepository.findById(
                    itemId
                )
            )

            assertNull(
                reopened.contentRepository.findById(
                    contentId
                )
            )

            assertNull(
                reopened.libraryRepository.findById(
                    libraryId
                )
            )

            assertNull(
                reopened.packageRepository.findById(
                    packageId
                )
            )

            assertEquals(
                0,
                assertNotNull(
                    reopened.catalogRepository.findById(
                        catalogId
                    )
                ).packageCount
            )
        } finally {
            directory.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `persisted uninstall removes only selected package data`() {
        val directory =
            Files.createTempDirectory(
                "persisted-selective-uninstall-test"
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

            val removedContentId =
                ContentId(
                    "removed-content"
                )

            val preservedContentId =
                ContentId(
                    "preserved-content"
                )

            val removedItemId =
                LearningItemId(
                    "removed-item"
                )

            val preservedItemId =
                LearningItemId(
                    "preserved-item"
                )

            repositories.contentRepository.saveAll(
                listOf(
                    createContent(
                        id = removedContentId,
                        primaryText = "Removed"
                    ),
                    createContent(
                        id = preservedContentId,
                        primaryText = "Preserved"
                    )
                )
            )

            repositories.learningItemRepository.saveAll(
                listOf(
                    createLearningItem(
                        id = removedItemId,
                        contentId = removedContentId
                    ),
                    createLearningItem(
                        id = preservedItemId,
                        contentId = preservedContentId
                    )
                )
            )

            repositories.libraryRepository.saveAll(
                listOf(
                    createLibrary(
                        id = removedLibraryId,
                        name = "Removed Library",
                        contentIds = setOf(
                            removedContentId
                        )
                    ),
                    createLibrary(
                        id = preservedLibraryId,
                        name = "Preserved Library",
                        contentIds = setOf(
                            preservedContentId
                        )
                    )
                )
            )

            repositories.packageRepository.save(
                createPackage(
                    id = removedPackageId,
                    name = "Removed Package",
                    libraryIds = setOf(
                        removedLibraryId
                    )
                )
            )

            repositories.packageRepository.save(
                createPackage(
                    id = preservedPackageId,
                    name = "Preserved Package",
                    libraryIds = setOf(
                        preservedLibraryId
                    )
                )
            )

            repositories.catalogRepository.save(
                PackageCatalog(
                    id = catalogId,
                    packageIds = setOf(
                        removedPackageId,
                        preservedPackageId
                    )
                )
            )

            createUninstaller(
                directory
            ).execute(
                UninstallContentPackageCommand(
                    catalogId = catalogId,
                    packageId = removedPackageId
                )
            )

            val reopened =
                createRepositories(
                    directory
                )

            assertNull(
                reopened.learningItemRepository.findById(
                    removedItemId
                )
            )

            assertNull(
                reopened.contentRepository.findById(
                    removedContentId
                )
            )

            assertNull(
                reopened.libraryRepository.findById(
                    removedLibraryId
                )
            )

            assertNull(
                reopened.packageRepository.findById(
                    removedPackageId
                )
            )

            assertEquals(
                createLearningItem(
                    id = preservedItemId,
                    contentId = preservedContentId
                ),
                reopened.learningItemRepository.findById(
                    preservedItemId
                )
            )

            assertEquals(
                createContent(
                    id = preservedContentId,
                    primaryText = "Preserved"
                ),
                reopened.contentRepository.findById(
                    preservedContentId
                )
            )

            assertEquals(
                createLibrary(
                    id = preservedLibraryId,
                    name = "Preserved Library",
                    contentIds = setOf(
                        preservedContentId
                    )
                ),
                reopened.libraryRepository.findById(
                    preservedLibraryId
                )
            )

            assertEquals(
                createPackage(
                    id = preservedPackageId,
                    name = "Preserved Package",
                    libraryIds = setOf(
                        preservedLibraryId
                    )
                ),
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
            directory.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `persisted uninstall is idempotent`() {
        val directory =
            Files.createTempDirectory(
                "persisted-idempotent-uninstall-test"
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

            val packageId =
                PackageId(
                    "english-package"
                )

            repositories.packageRepository.save(
                createPackage(
                    id = packageId,
                    name = "English Package"
                )
            )

            repositories.catalogRepository.save(
                PackageCatalog(
                    id = catalogId,
                    packageIds = setOf(
                        packageId
                    )
                )
            )

            val uninstaller =
                createUninstaller(
                    directory
                )

            val command =
                UninstallContentPackageCommand(
                    catalogId = catalogId,
                    packageId = packageId
                )

            uninstaller.execute(
                command
            )

            uninstaller.execute(
                command
            )

            val reopened =
                createRepositories(
                    directory
                )

            assertNull(
                reopened.packageRepository.findById(
                    packageId
                )
            )

            assertEquals(
                emptySet(),
                assertNotNull(
                    reopened.catalogRepository.findById(
                        catalogId
                    )
                ).packageIds
            )
        } finally {
            directory.toFile()
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
    ): Repositories {
        val app = vn.loi.learning.infrastructure.LearningApplicationFactory.createPersisted(directory, false)
        return Repositories(
            libraryRepository = app.contentLibraryRepository!!,
            contentRepository = app.contentRepository!!,
            learningItemRepository = app.learningItemRepository!!,
            packageRepository = app.contentPackageRepository!!,
            catalogRepository = app.packageCatalog!!
        )
    }

    private fun createContent(
        id: ContentId,
        primaryText: String
    ): Content =
        Content(
            id = id,
            type = ContentType.SENTENCE,
            text = ContentText(
                primaryText = primaryText
            )
        )

    private fun createLearningItem(
        id: LearningItemId,
        contentId: ContentId
    ): LearningItem =
        LearningItem(
            id = id,
            contentId = contentId,
            mode = LearningMode.MEANING_RECOGNITION
        )

    private fun createLibrary(
        id: ContentLibraryId,
        name: String,
        contentIds: Set<ContentId>
    ): ContentLibrary =
        ContentLibrary(
            id = id,
            descriptor = LibraryDescriptor(
                name = name
            ),
            contentIds = contentIds
        )

    private fun createPackage(
        id: PackageId,
        name: String,
        libraryIds: Set<ContentLibraryId> = emptySet()
    ): ContentPackage =
        ContentPackage(
            id = id,
            descriptor = PackageDescriptor(
                name = name,
                version = "1.0.0",
                format = "OPD3"
            ),
            libraryIds = libraryIds
        )

    private data class Repositories(
        val libraryRepository: vn.loi.learning.application.port.ContentLibraryRepository,
        val contentRepository: vn.loi.learning.application.port.ContentRepository,
        val learningItemRepository: vn.loi.learning.application.port.LearningItemRepository,
        val packageRepository: vn.loi.learning.application.port.ContentPackageRepository,
        val catalogRepository: vn.loi.learning.application.port.PackageCatalogRepository
    )
}