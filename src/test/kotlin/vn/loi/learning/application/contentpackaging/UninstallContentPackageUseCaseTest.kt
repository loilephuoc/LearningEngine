package vn.loi.learning.application.contentpackaging

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
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
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class UninstallContentPackageUseCaseTest {

    @Test
    fun `removes package and unregisters it from catalog`() {
        val packageId =
            PackageId(
                "english-package"
            )

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val libraryId =
            ContentLibraryId(
                "english-library"
            )

        val contentId =
            ContentId(
                "hello-content"
            )

        val meaningItemId =
            LearningItemId(
                "hello-meaning"
            )

        val listeningItemId =
            LearningItemId(
                "hello-listening"
            )

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

        contentLibraryRepository.save(
            ContentLibrary(
                id = libraryId,
                descriptor =
                    LibraryDescriptor(
                        name =
                            "English Library"
                    ),
                contentIds =
                    setOf(
                        contentId
                    )
            )
        )

        contentRepository.save(
            Content(
                id = contentId,
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Hello.",
                        translatedText =
                            "Xin chào."
                    )
            )
        )

        learningItemRepository.save(
            LearningItem(
                id =
                    meaningItemId,
                contentId =
                    contentId,
                mode =
                    LearningMode.MEANING_RECOGNITION
            )
        )

        learningItemRepository.save(
            LearningItem(
                id =
                    listeningItemId,
                contentId =
                    contentId,
                mode =
                    LearningMode.LISTENING_RECOGNITION
            )
        )

        contentPackageRepository.save(
            ContentPackage(
                id = packageId,
                descriptor =
                    PackageDescriptor(
                        name =
                            "English Elementary",
                        version =
                            "1.0.0",
                        format =
                            "OPD3"
                    ),
                libraryIds =
                    setOf(
                        libraryId
                    )
            )
        )

        packageCatalogRepository.save(
            PackageCatalog(
                id = catalogId,
                packageIds =
                    setOf(
                        packageId
                    )
            )
        )

        val useCase =
            createUseCase(
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
            )

        useCase.execute(
            UninstallContentPackageCommand(
                catalogId =
                    catalogId,
                packageId =
                    packageId
            )
        )

        assertNull(
            learningItemRepository.findById(
                meaningItemId
            )
        )

        assertNull(
            learningItemRepository.findById(
                listeningItemId
            )
        )

        assertEquals(
            0,
            learningItemRepository.count()
        )

        assertNull(
            contentRepository.findById(
                contentId
            )
        )

        assertEquals(
            0,
            contentRepository.count()
        )

        assertNull(
            contentLibraryRepository.findById(
                libraryId
            )
        )

        assertEquals(
            0,
            contentLibraryRepository.count()
        )

        assertNull(
            contentPackageRepository.findById(
                packageId
            )
        )

        assertEquals(
            0,
            contentPackageRepository.count()
        )

        val updatedCatalog =
            assertNotNull(
                packageCatalogRepository.findById(
                    catalogId
                )
            )

        assertFalse(
            updatedCatalog.contains(
                packageId
            )
        )

        assertEquals(
            0,
            updatedCatalog.packageCount
        )

        assertEquals(
            1,
            packageCatalogRepository.count()
        )
    }

    @Test
    fun `preserves libraries and content referenced by another package`() {
        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val packageId =
            PackageId(
                "english-package-v1"
            )

        val remainingPackageId =
            PackageId(
                "english-package-v2"
            )

        val sharedLibraryId =
            ContentLibraryId(
                "shared-english-library"
            )

        val exclusiveLibraryId =
            ContentLibraryId(
                "exclusive-english-library"
            )

        val sharedContentId =
            ContentId(
                "shared-content"
            )

        val exclusiveContentId =
            ContentId(
                "exclusive-content"
            )

        val sharedLearningItemId =
            LearningItemId(
                "shared-learning-item"
            )

        val exclusiveLearningItemId =
            LearningItemId(
                "exclusive-learning-item"
            )

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

        contentLibraryRepository.save(
            ContentLibrary(
                id =
                    sharedLibraryId,
                descriptor =
                    LibraryDescriptor(
                        name =
                            "Shared Library"
                    ),
                contentIds =
                    setOf(
                        sharedContentId
                    )
            )
        )

        contentLibraryRepository.save(
            ContentLibrary(
                id =
                    exclusiveLibraryId,
                descriptor =
                    LibraryDescriptor(
                        name =
                            "Exclusive Library"
                    ),
                contentIds =
                    setOf(
                        exclusiveContentId
                    )
            )
        )

        contentRepository.save(
            Content(
                id =
                    sharedContentId,
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Shared content"
                    )
            )
        )

        contentRepository.save(
            Content(
                id =
                    exclusiveContentId,
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Exclusive content"
                    )
            )
        )

        learningItemRepository.save(
            LearningItem(
                id =
                    sharedLearningItemId,
                contentId =
                    sharedContentId,
                mode =
                    LearningMode.MEANING_RECOGNITION
            )
        )

        learningItemRepository.save(
            LearningItem(
                id =
                    exclusiveLearningItemId,
                contentId =
                    exclusiveContentId,
                mode =
                    LearningMode.MEANING_RECOGNITION
            )
        )

        contentPackageRepository.save(
            ContentPackage(
                id =
                    packageId,
                descriptor =
                    PackageDescriptor(
                        name =
                            "English Package V1",
                        version =
                            "1.0.0",
                        format =
                            "OPD3"
                    ),
                libraryIds =
                    setOf(
                        sharedLibraryId,
                        exclusiveLibraryId
                    )
            )
        )

        contentPackageRepository.save(
            ContentPackage(
                id =
                    remainingPackageId,
                descriptor =
                    PackageDescriptor(
                        name =
                            "English Package V2",
                        version =
                            "2.0.0",
                        format =
                            "OPD3"
                    ),
                libraryIds =
                    setOf(
                        sharedLibraryId
                    )
            )
        )

        packageCatalogRepository.save(
            PackageCatalog(
                id =
                    catalogId,
                packageIds =
                    setOf(
                        packageId,
                        remainingPackageId
                    )
            )
        )

        val useCase =
            createUseCase(
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
            )

        useCase.execute(
            UninstallContentPackageCommand(
                catalogId =
                    catalogId,
                packageId =
                    packageId
            )
        )

        assertNotNull(
            contentLibraryRepository.findById(
                sharedLibraryId
            )
        )

        assertNotNull(
            contentRepository.findById(
                sharedContentId
            )
        )

        assertNotNull(
            learningItemRepository.findById(
                sharedLearningItemId
            )
        )

        assertNull(
            contentLibraryRepository.findById(
                exclusiveLibraryId
            )
        )

        assertNull(
            contentRepository.findById(
                exclusiveContentId
            )
        )

        assertNull(
            learningItemRepository.findById(
                exclusiveLearningItemId
            )
        )

        assertNull(
            contentPackageRepository.findById(
                packageId
            )
        )

        assertNotNull(
            contentPackageRepository.findById(
                remainingPackageId
            )
        )

        val updatedCatalog =
            assertNotNull(
                packageCatalogRepository.findById(
                    catalogId
                )
            )

        assertFalse(
            updatedCatalog.contains(
                packageId
            )
        )

        assertEquals(
            setOf(
                remainingPackageId
            ),
            updatedCatalog.packageIds
        )
    }

    @Test
    fun `deletes resolved content package identity without deleting unrelated package`() {
        val contentLibraryRepository = InMemoryContentLibraryRepository()
        val contentRepository = InMemoryContentRepository()
        val learningItemRepository = InMemoryLearningItemRepository()
        val contentPackageRepository = InMemoryContentPackageRepository()
        val packageCatalogRepository = InMemoryPackageCatalogRepository()
        val resolvedPackageId = PackageId("resolved-package-id")
        val unrelatedPackageId = PackageId("unrelated-package-id")

        contentPackageRepository.save(
            ContentPackage(
                id = resolvedPackageId,
                descriptor = PackageDescriptor("selected-topic", "1.0", "OPD3")
            )
        )
        contentPackageRepository.save(
            ContentPackage(
                id = unrelatedPackageId,
                descriptor = PackageDescriptor("Unrelated", "1.0", "OPD3")
            )
        )

        createUseCase(
            contentLibraryRepository,
            contentRepository,
            learningItemRepository,
            contentPackageRepository,
            packageCatalogRepository
        ).execute(
            UninstallContentPackageCommand(
                catalogId = PackageCatalogId("missing-catalog"),
                packageId = PackageId("selected-topic")
            )
        )

        assertNull(contentPackageRepository.findById(resolvedPackageId))
        assertNotNull(contentPackageRepository.findById(unrelatedPackageId))
    }

    @Test
    fun `fails before mutation when installed package ownership graph has no content package`() {
        val installedPackages = InMemoryInstalledPackageRepository()
        val installedPackage = InstalledPackage.reconstitute(
            id = InstalledPackageId("installed-a"),
            libraryId = LibraryId("default-library"),
            packageId = PackageId("package-a"),
            topicId = TopicId("topic-a"),
            name = PackageName("Package A"),
            version = PackageVersion("1.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.EPOCH,
            contentCount = 3,
            learningItemCount = 3
        )
        installedPackages.save(installedPackage)
        val operation = PackageUninstallOperation(
            contentLibraryRepository = InMemoryContentLibraryRepository(),
            contentRepository = InMemoryContentRepository(),
            learningItemRepository = InMemoryLearningItemRepository(),
            contentPackageRepository = InMemoryContentPackageRepository(),
            packageCatalogRepository = InMemoryPackageCatalogRepository(),
            installedPackageRepository = installedPackages
        )

        assertFailsWith<IllegalStateException> {
            operation.execute(
                UninstallContentPackageCommand(
                    PackageCatalogId("catalog"),
                    installedPackage.packageId
                )
            )
        }

        assertEquals(installedPackage, installedPackages.findById(installedPackage.id))
    }

    @Test
    fun `deletes physical media namespace when uninstalling package`() {
        val tempDir = java.nio.file.Files.createTempDirectory("uninstall-media-test-")
        try {
            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(tempDir)
            mediaStorage.store("TestPackage", "sample.mp3", "audio".toByteArray())
            assertEquals(true, mediaStorage.exists("TestPackage/sample.mp3"))

            val packageId = PackageId("TestPackage")
            val catalogId = PackageCatalogId("installed-packages")
            val contentLibraryRepository = InMemoryContentLibraryRepository()
            val contentRepository = InMemoryContentRepository()
            val learningItemRepository = InMemoryLearningItemRepository()
            val contentPackageRepository = InMemoryContentPackageRepository()
            val packageCatalogRepository = InMemoryPackageCatalogRepository()
            val installedPackages = InMemoryInstalledPackageRepository()

            val installedPackage = InstalledPackage(
                id = InstalledPackageId("installed-pkg-1"),
                packageId = packageId,
                name = PackageName("TestPackage"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                libraryId = LibraryId("lib-1"),
                topicId = TopicId("topic-1"),
                contentCount = 1,
                learningItemCount = 1
            )
            installedPackages.save(installedPackage)

            contentLibraryRepository.save(
                ContentLibrary(
                    id = ContentLibraryId("lib-1"),
                    descriptor = LibraryDescriptor("lib-1"),
                    contentIds = emptySet()
                )
            )

            packageCatalogRepository.save(
                PackageCatalog(
                    id = catalogId,
                    packageIds = setOf(packageId)
                )
            )
            contentPackageRepository.save(
                ContentPackage(
                    id = packageId,
                    descriptor = PackageDescriptor(name = "TestPackage", version = "1.0.0", format = "OPD3"),
                    libraryIds = setOf(ContentLibraryId("lib-1"))
                )
            )

            val operation = PackageUninstallOperation(
                contentLibraryRepository = contentLibraryRepository,
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                contentPackageRepository = contentPackageRepository,
                packageCatalogRepository = packageCatalogRepository,
                installedPackageRepository = installedPackages,
                contentMediaStorage = mediaStorage
            )

            operation.execute(UninstallContentPackageCommand(catalogId, packageId))

            assertEquals(false, mediaStorage.exists("TestPackage/sample.mp3"))
            assertEquals(false, java.nio.file.Files.exists(tempDir.resolve("TestPackage")))
        } finally {
            java.nio.file.Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder()).forEach(java.nio.file.Files::deleteIfExists)
        }
    }

    private fun createUseCase(
        contentLibraryRepository:
        InMemoryContentLibraryRepository,
        contentRepository:
        InMemoryContentRepository,
        learningItemRepository:
        InMemoryLearningItemRepository,
        contentPackageRepository:
        InMemoryContentPackageRepository,
        packageCatalogRepository:
        InMemoryPackageCatalogRepository
    ): UninstallContentPackageUseCase =
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
