package vn.loi.learning.infrastructure

import java.nio.file.Path
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.contentlibrary.ContentLibraryQueryService
import vn.loi.learning.application.contentlibrary.LibraryContentQueryService
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.learningdashboard.LearningDashboardQueryService
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.domain.study.analytics.service.StudyStatisticsCalculator
import vn.loi.learning.domain.study.memory.FsrsForgettingCurve
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.infrastructure.contentpackaging.ContentPackageImportFactory
import vn.loi.learning.infrastructure.persistence.PersistedLearningPlatformFactory
import vn.loi.learning.infrastructure.persistence.json.JsonContentLibraryStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentStore
import vn.loi.learning.infrastructure.persistence.json.JsonLearningItemStore
import vn.loi.learning.infrastructure.persistence.json.JsonMemoryStateStore
import vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore
import vn.loi.learning.infrastructure.persistence.json.JsonReviewEventStore
import vn.loi.learning.infrastructure.persistence.json.JsonStudySessionStore
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedReviewEventRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedStudySessionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner

object LearningApplicationFactory {

    fun createInMemory(): LearningApplicationContext {
        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val memoryStateRepository =
            InMemoryMemoryStateRepository()

        val reviewEventRepository =
            InMemoryReviewEventRepository()

        val studySessionRepository =
            InMemoryStudySessionRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        val transactionRunner =
            InMemoryTransactionRunner()

        return createContext(
            contentLibraryRepository =
                contentLibraryRepository,
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository,
            memoryStateRepository =
                memoryStateRepository,
            reviewEventRepository =
                reviewEventRepository,
            studySessionRepository =
                studySessionRepository,
            contentPackageRepository =
                contentPackageRepository,
            packageCatalogRepository =
                packageCatalogRepository,
            transactionRunner =
                transactionRunner
        )
    }

    fun createPersisted(
        persistenceDirectory: Path
    ): LearningApplicationContext {
        val contentLibrariesPath =
            persistenceDirectory.resolve(
                CONTENT_LIBRARIES_FILE_NAME
            )

        val contentsPath =
            persistenceDirectory.resolve(
                CONTENTS_FILE_NAME
            )

        val learningItemsPath =
            persistenceDirectory.resolve(
                LEARNING_ITEMS_FILE_NAME
            )

        val memoryStatesPath =
            persistenceDirectory.resolve(
                MEMORY_STATES_FILE_NAME
            )

        val reviewEventsPath =
            persistenceDirectory.resolve(
                REVIEW_EVENTS_FILE_NAME
            )

        val studySessionsPath =
            persistenceDirectory.resolve(
                STUDY_SESSIONS_FILE_NAME
            )

        val contentPackagesPath =
            persistenceDirectory.resolve(
                CONTENT_PACKAGES_FILE_NAME
            )

        val packageCatalogsPath =
            persistenceDirectory.resolve(
                PACKAGE_CATALOGS_FILE_NAME
            )

        val contentLibraryRepository =
            StoreBackedContentLibraryRepository(
                JsonContentLibraryStore(
                    contentLibrariesPath
                )
            )

        val contentRepository =
            StoreBackedContentRepository(
                JsonContentStore(
                    contentsPath
                )
            )

        val learningItemRepository =
            StoreBackedLearningItemRepository(
                JsonLearningItemStore(
                    learningItemsPath
                )
            )

        val memoryStateRepository =
            StoreBackedMemoryStateRepository(
                JsonMemoryStateStore(
                    memoryStatesPath
                )
            )

        val reviewEventRepository =
            StoreBackedReviewEventRepository(
                JsonReviewEventStore(
                    reviewEventsPath
                )
            )

        val studySessionRepository =
            StoreBackedStudySessionRepository(
                JsonStudySessionStore(
                    studySessionsPath
                )
            )

        val contentPackageRepository =
            StoreBackedContentPackageRepository(
                JsonContentPackageStore(
                    contentPackagesPath
                )
            )

        val packageCatalogRepository =
            StoreBackedPackageCatalogRepository(
                JsonPackageCatalogStore(
                    packageCatalogsPath
                )
            )

        val transactionRunner =
            JsonFileTransactionRunner(
                listOf(
                    contentLibrariesPath,
                    contentsPath,
                    learningItemsPath,
                    memoryStatesPath,
                    reviewEventsPath,
                    studySessionsPath,
                    contentPackagesPath,
                    packageCatalogsPath
                )
            )

        return createContext(
            contentLibraryRepository =
                contentLibraryRepository,
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository,
            memoryStateRepository =
                memoryStateRepository,
            reviewEventRepository =
                reviewEventRepository,
            studySessionRepository =
                studySessionRepository,
            contentPackageRepository =
                contentPackageRepository,
            packageCatalogRepository =
                packageCatalogRepository,
            transactionRunner =
                transactionRunner
        )
    }

    private fun createContext(
        contentLibraryRepository:
        vn.loi.learning.application.port.ContentLibraryRepository,
        contentRepository:
        vn.loi.learning.application.port.ContentRepository,
        learningItemRepository:
        vn.loi.learning.application.port.LearningItemRepository,
        memoryStateRepository:
        vn.loi.learning.application.port.MemoryStateRepository,
        reviewEventRepository:
        vn.loi.learning.application.port.ReviewEventRepository,
        studySessionRepository:
        vn.loi.learning.application.port.StudySessionRepository,
        contentPackageRepository:
        vn.loi.learning.application.port.ContentPackageRepository,
        packageCatalogRepository:
        vn.loi.learning.application.port.PackageCatalogRepository,
        transactionRunner:
        vn.loi.learning.application.port.TransactionRunner
    ): LearningApplicationContext {
        val engine =
            LearningEngine(
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                memoryStateRepository =
                    memoryStateRepository,
                reviewEventRepository =
                    reviewEventRepository,
                sessionRepository =
                    studySessionRepository,
                transactionRunner =
                    transactionRunner,
                scheduler =
                    FsrsScheduler()
            )

        val reviewHistory =
            ReviewHistoryQueryService(
                reviewEventRepository =
                    reviewEventRepository
            )

        val statistics =
            StudyStatisticsQueryService(
                reviewHistoryQueryService =
                    reviewHistory,
                studyStatisticsCalculator =
                    StudyStatisticsCalculator()
            )

        val dashboard =
            LearningDashboardQueryServiceFactory.create(
                memoryStateQuery = memoryStateRepository as vn.loi.learning.application.port.MemoryStateQuery,
                reviewEventRepository =
                    reviewEventRepository,
                forgettingCurve =
                    FsrsForgettingCurve()
            )

        val installedPackages =
            InstalledPackageQueryService(
                contentPackageRepository =
                    contentPackageRepository
            )

        val contentLibraries =
            ContentLibraryQueryService(
                contentLibraryRepository =
                    contentLibraryRepository,
                learningItemRepository =
                    learningItemRepository
            )

        val libraryContents =
            LibraryContentQueryService(
                contentLibraryRepository =
                    contentLibraryRepository,
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository
            )

        val packageImporter:
                    (Path) -> PackageImportService =
            { packageDirectory ->
                PersistedLearningPlatformFactory.create(
                    packageScanner =
                        ContentPackageImportFactory.createScanner(
                            packageDirectory
                        ),
                    packageInstaller =
                        ContentPackageImportFactory.createInstaller(),
                    packageContentImporter =
                        ContentPackageImportFactory.createContentImporter(),
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
            }

        return LearningApplicationContext(
            engine = engine,
            dashboard = dashboard,
            statistics = statistics,
            reviewHistory = reviewHistory,
            installedPackages = installedPackages,
            contentLibraries = contentLibraries,
            libraryContents = libraryContents,
            packageImporter = packageImporter
        )
    }

    private const val CONTENT_LIBRARIES_FILE_NAME =
        "content-libraries.json"

    private const val CONTENTS_FILE_NAME =
        "contents.json"

    private const val LEARNING_ITEMS_FILE_NAME =
        "learning-items.json"

    private const val MEMORY_STATES_FILE_NAME =
        "memory-states.json"

    private const val REVIEW_EVENTS_FILE_NAME =
        "review-events.json"

    private const val STUDY_SESSIONS_FILE_NAME =
        "study-sessions.json"

    private const val CONTENT_PACKAGES_FILE_NAME =
        "content-packages.json"

    private const val PACKAGE_CATALOGS_FILE_NAME =
        "package-catalogs.json"
}

