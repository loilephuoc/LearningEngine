package vn.loi.learning.infrastructure.persistence

import java.nio.file.Path
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageInstaller
import vn.loi.learning.application.contentpackaging.PackageRegistrationOperation
import vn.loi.learning.application.contentpackaging.PackageScanner
import vn.loi.learning.application.contentpackaging.PackageUninstallOperation
import vn.loi.learning.application.contentpackaging.UninstallContentPackageUseCase
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
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.TransactionRunner

object PersistedLearningPlatformFactory {

    fun createPersisted(
        persistenceDirectory: Path,
        packageScanner: PackageScanner,
        packageInstaller: PackageInstaller,
        packageContentImporter: PackageContentImporter
    ): PackageImportService =
        create(
            packageScanner = packageScanner,
            packageInstaller = packageInstaller,
            packageContentImporter = packageContentImporter,
            contentLibraryRepository = StoreBackedContentLibraryRepository(JsonContentLibraryStore(persistenceDirectory.resolve("content-libraries.json"))),
            contentRepository = StoreBackedContentRepository(JsonContentStore(persistenceDirectory.resolve("contents.json"))),
            learningItemRepository = StoreBackedLearningItemRepository(JsonLearningItemStore(persistenceDirectory.resolve("learning-items.json"))),
            contentPackageRepository = StoreBackedContentPackageRepository(JsonContentPackageStore(persistenceDirectory.resolve("content-packages.json"))),
            packageCatalogRepository = StoreBackedPackageCatalogRepository(JsonPackageCatalogStore(persistenceDirectory.resolve("package-catalogs.json"))),
            transactionRunner = JsonFileTransactionRunner(listOf(persistenceDirectory.resolve("content-libraries.json"), persistenceDirectory.resolve("contents.json"), persistenceDirectory.resolve("learning-items.json"), persistenceDirectory.resolve("content-packages.json"), persistenceDirectory.resolve("package-catalogs.json")))
        )

    fun create(
        packageScanner: PackageScanner,
        packageInstaller: PackageInstaller,
        packageContentImporter: PackageContentImporter,
        contentLibraryRepository: ContentLibraryRepository,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        contentPackageRepository: vn.loi.learning.application.port.ContentPackageRepository,
        packageCatalogRepository: vn.loi.learning.application.port.PackageCatalogRepository,
        transactionRunner: TransactionRunner
    ): PackageImportService {

        val packageRegistrationOperation =
            PackageRegistrationOperation(
                contentPackageRepository = contentPackageRepository,
                packageCatalogRepository = packageCatalogRepository
            )

        return PackageImportService(
            packageScanner = packageScanner,
            packageInstaller = packageInstaller,
            packageContentImporter = packageContentImporter,
            contentRepository = contentRepository,
            contentLibraryRepository = contentLibraryRepository,
            learningItemRepository = learningItemRepository,
            packageRegistrationOperation = packageRegistrationOperation,
            transactionRunner = transactionRunner
        )
    }
    fun createPersistedUninstaller(
        persistenceDirectory: Path
    ): UninstallContentPackageUseCase =
        createUninstaller(
            contentLibraryRepository = StoreBackedContentLibraryRepository(JsonContentLibraryStore(persistenceDirectory.resolve("content-libraries.json"))),
            contentRepository = StoreBackedContentRepository(JsonContentStore(persistenceDirectory.resolve("contents.json"))),
            learningItemRepository = StoreBackedLearningItemRepository(JsonLearningItemStore(persistenceDirectory.resolve("learning-items.json"))),
            contentPackageRepository = StoreBackedContentPackageRepository(JsonContentPackageStore(persistenceDirectory.resolve("content-packages.json"))),
            packageCatalogRepository = StoreBackedPackageCatalogRepository(JsonPackageCatalogStore(persistenceDirectory.resolve("package-catalogs.json"))),
            transactionRunner = JsonFileTransactionRunner(listOf(persistenceDirectory.resolve("learning-items.json"), persistenceDirectory.resolve("contents.json"), persistenceDirectory.resolve("content-libraries.json"), persistenceDirectory.resolve("content-packages.json"), persistenceDirectory.resolve("package-catalogs.json")))
        )

    fun createUninstaller(
        contentLibraryRepository: ContentLibraryRepository,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        contentPackageRepository: vn.loi.learning.application.port.ContentPackageRepository,
        packageCatalogRepository: vn.loi.learning.application.port.PackageCatalogRepository,
        transactionRunner: TransactionRunner
    ): UninstallContentPackageUseCase =
        UninstallContentPackageUseCase(
            uninstallOperation = PackageUninstallOperation(
                contentLibraryRepository = contentLibraryRepository,
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                contentPackageRepository = contentPackageRepository,
                packageCatalogRepository = packageCatalogRepository
            ),
            transactionRunner = transactionRunner
        )
}












