package vn.loi.learning.infrastructure.persistence

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageImportOutcome
import vn.loi.learning.application.contentpackaging.PackageImportProgressListener
import vn.loi.learning.application.contentpackaging.PackageImportResult
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.contentpackaging.PackageInstaller
import vn.loi.learning.application.contentpackaging.PackageRegistrationOperation
import vn.loi.learning.application.contentpackaging.PackageScanner
import vn.loi.learning.application.contentpackaging.PackageUninstallOperation
import vn.loi.learning.application.contentpackaging.UninstallContentPackageUseCase
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.infrastructure.PersistedLearningPlatform
import vn.loi.learning.infrastructure.contentpackaging.ContentPackageImportFactory
import vn.loi.learning.infrastructure.persistence.sqlite.JsonToSqliteMigrationService
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteContentPackageRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteContentRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteDatabaseFactory
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteLearningItemRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqlitePackageCatalogRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteTransactionRunner

object PersistedLearningPlatformFactory {

    fun createPlatform(
        persistenceDirectory: Path,
        packageDirectory: Path
    ): PersistedLearningPlatform {
        val dbPath = persistenceDirectory.resolve("learning_engine.db")
        val database = SqliteDatabaseFactory.createFromFile(dbPath)
        JsonToSqliteMigrationService.migrateIfNeeded(persistenceDirectory, database)

        val contentLibraryRepository = SqliteContentLibraryRepository(database)
        val contentRepository = SqliteContentRepository(database)
        val learningItemRepository = SqliteLearningItemRepository(database)
        val contentPackageRepository = SqliteContentPackageRepository(database)
        val packageCatalogRepository = SqlitePackageCatalogRepository(database)
        val transactionRunner = SqliteTransactionRunner(database)

        val packageImportService =
            create(
                packageScanner =
                    ContentPackageImportFactory.createScanner(
                        packageDirectory
                    ),
                packageInstaller =
                    ContentPackageImportFactory.createInstaller(),
                packageContentImporter =
                    ContentPackageImportFactory
                        .createContentImporter(),
                contentLibraryRepository =
                    contentLibraryRepository,
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                contentPackageRepository =
                    contentPackageRepository,
                packageCatalogRepository =
                    packageCatalogRepository,
                transactionRunner =
                    transactionRunner
            )

        val learningEngine =
            PersistedLearningEngineFactory.create(
                persistenceDirectory =
                    persistenceDirectory,
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                transactionRunner =
                    transactionRunner
            )

        return PersistedLearningPlatform(
            learningEngine = learningEngine,
            packageImportService = packageImportService
        )
    }

    fun createPersisted(
        persistenceDirectory: Path,
        packageScanner: PackageScanner,
        packageInstaller: PackageInstaller,
        packageContentImporter: PackageContentImporter
    ): PackageImportService {
        val dbPath = persistenceDirectory.resolve("learning_engine.db")
        val database = SqliteDatabaseFactory.createFromFile(dbPath)
        JsonToSqliteMigrationService.migrateIfNeeded(persistenceDirectory, database)

        val contentLibraryRepository = SqliteContentLibraryRepository(database)
        val contentRepository = SqliteContentRepository(database)
        val learningItemRepository = SqliteLearningItemRepository(database)
        val contentPackageRepository = SqliteContentPackageRepository(database)
        val packageCatalogRepository = SqlitePackageCatalogRepository(database)
        val transactionRunner = SqliteTransactionRunner(database)

        return create(
            packageScanner = packageScanner,
            packageInstaller = packageInstaller,
            packageContentImporter = packageContentImporter,
            contentLibraryRepository = contentLibraryRepository,
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            contentPackageRepository = contentPackageRepository,
            packageCatalogRepository = packageCatalogRepository,
            transactionRunner = transactionRunner
        )
    }

    fun create(
        packageScanner: PackageScanner,
        packageInstaller: PackageInstaller,
        packageContentImporter: PackageContentImporter,
        contentLibraryRepository: ContentLibraryRepository,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        contentPackageRepository: ContentPackageRepository,
        packageCatalogRepository: PackageCatalogRepository,
        transactionRunner: TransactionRunner,
        progressListener: PackageImportProgressListener? = null,
        installedPackageRepository: vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null,
        memoryStateRepository: MemoryStateRepository? = null,
        reviewEventRepository: ReviewEventRepository? = null,
        studySessionRepository: StudySessionRepository? = null,
        studyQueueRepository: vn.loi.learning.application.port.StudyQueueRepository? = null,
        partOfSpeechRegistry: vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry? = null,
        packageLifecycleCompletion: ((PackageImportResult) -> PackageImportOutcome)? = null
    ): PackageImportService {
        val packageRegistrationOperation =
            PackageRegistrationOperation(
                contentPackageRepository =
                    contentPackageRepository,
                packageCatalogRepository =
                    packageCatalogRepository
            )

        return PackageImportService(
            packageScanner = packageScanner,
            packageInstaller = packageInstaller,
            packageContentImporter =
                packageContentImporter,
            contentRepository =
                contentRepository,
            contentLibraryRepository =
                contentLibraryRepository,
            learningItemRepository =
                learningItemRepository,
            packageRegistrationOperation =
                packageRegistrationOperation,
            transactionRunner =
                transactionRunner,
            progressListener = progressListener,
            installedPackageRepository = installedPackageRepository,
            contentPackageRepository = contentPackageRepository,
            packageLifecycleCompletion = packageLifecycleCompletion,
            orphanPackageLearningStateReconciler =
                vn.loi.learning.application.contentpackaging.OrphanPackageLearningStateReconciler(
                    installedPackageRepository = installedPackageRepository,
                    contentPackageRepository = contentPackageRepository,
                    memoryStateRepository = memoryStateRepository,
                    reviewEventRepository = reviewEventRepository,
                    studySessionRepository = studySessionRepository,
                    studyQueueRepository = studyQueueRepository
                ),
            partOfSpeechRegistry = partOfSpeechRegistry
        )
    }

    fun createPersistedUninstaller(
        persistenceDirectory: Path
    ): UninstallContentPackageUseCase {
        val dbPath = persistenceDirectory.resolve("learning_engine.db")
        val database = SqliteDatabaseFactory.createFromFile(dbPath)
        JsonToSqliteMigrationService.migrateIfNeeded(persistenceDirectory, database)

        val contentLibraryRepository = SqliteContentLibraryRepository(database)
        val contentRepository = SqliteContentRepository(database)
        val learningItemRepository = SqliteLearningItemRepository(database)
        val contentPackageRepository = SqliteContentPackageRepository(database)
        val packageCatalogRepository = SqlitePackageCatalogRepository(database)
        val transactionRunner = SqliteTransactionRunner(database)

        return createUninstaller(
            contentLibraryRepository = contentLibraryRepository,
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            contentPackageRepository = contentPackageRepository,
            packageCatalogRepository = packageCatalogRepository,
            transactionRunner = transactionRunner
        )
    }

    fun createUninstaller(
        contentLibraryRepository: ContentLibraryRepository,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        contentPackageRepository: ContentPackageRepository,
        packageCatalogRepository: PackageCatalogRepository,
        transactionRunner: TransactionRunner,
        memoryStateRepository: MemoryStateRepository? = null,
        reviewEventRepository: ReviewEventRepository? = null,
        studySessionRepository: StudySessionRepository? = null,
        contentMediaStorage: vn.loi.learning.application.port.ContentMediaStorage? = null
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
                        packageCatalogRepository,
                    memoryStateRepository =
                        memoryStateRepository,
                    reviewEventRepository =
                        reviewEventRepository,
                    studySessionRepository =
                        studySessionRepository,
                    contentMediaStorage =
                        contentMediaStorage
                ),
            transactionRunner =
                transactionRunner
        )
}
