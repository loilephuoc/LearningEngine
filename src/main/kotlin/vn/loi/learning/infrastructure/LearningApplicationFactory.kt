package vn.loi.learning.infrastructure

import java.nio.file.Path
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.contentlibrary.AttachPackageToLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.ContentLibraryQueryService
import vn.loi.learning.application.contentlibrary.CreateLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.DeleteLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.DetachPackageFromLibraryCollectionUseCase
import vn.loi.learning.application.contentlibrary.LibraryCollectionQueryService
import vn.loi.learning.application.contentlibrary.LibraryContentQueryService
import vn.loi.learning.application.contentlibrary.RenameLibraryCollectionUseCase
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.contentpackaging.PackageImportService
import vn.loi.learning.application.contentpackaging.PackageImportProgressListener
import vn.loi.learning.application.learningdashboard.LearningDashboardQueryService
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.application.topic.TopicQueryService
import vn.loi.learning.domain.study.analytics.service.StudyStatisticsCalculator
import vn.loi.learning.domain.study.memory.FsrsForgettingCurve
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.domain.study.scheduling.ValidatingScheduler
import vn.loi.learning.infrastructure.contentpackaging.ContentPackageImportFactory
import vn.loi.learning.infrastructure.persistence.PersistedLearningPlatformFactory
import vn.loi.learning.infrastructure.persistence.json.JsonContentLibraryStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonContentStore
import vn.loi.learning.infrastructure.persistence.json.JsonLearningItemStore
import vn.loi.learning.infrastructure.persistence.json.JsonInstalledPackageStore
import vn.loi.learning.infrastructure.persistence.json.JsonLibraryCollectionStore
import vn.loi.learning.infrastructure.persistence.json.JsonMemoryStateStore
import vn.loi.learning.infrastructure.persistence.json.JsonPackageCatalogStore
import vn.loi.learning.infrastructure.persistence.json.JsonReviewEventStore
import vn.loi.learning.infrastructure.persistence.json.JsonStudyQueueStore
import vn.loi.learning.infrastructure.persistence.json.JsonStudySessionStore
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedKnowledgeGraphRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLibraryCollectionRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedReviewEventRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedStudySessionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner
import vn.loi.learning.application.knowledge.GetKnowledgeGraphUseCase
import vn.loi.learning.application.knowledge.InstalledLibraryKnowledgeGraphProjection
import vn.loi.learning.application.knowledge.KnowledgeGraphQueryService
import vn.loi.learning.application.knowledge.SaveKnowledgeGraphUseCase
import vn.loi.learning.infrastructure.persistence.json.JsonKnowledgeGraphStore

object LearningApplicationFactory {

    fun createInMemory(): LearningApplicationContext {
        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val libraryCollectionRepository =
            InMemoryLibraryCollectionRepository()

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


        val studyQueueRepository =
            InMemoryStudyQueueRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        val transactionRunner =
            InMemoryTransactionRunner()

        return createContext(
            contentLibraryRepository =
                contentLibraryRepository,
            libraryCollectionRepository =
                libraryCollectionRepository,
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
            studyQueueRepository =
                studyQueueRepository,
            contentPackageRepository =
                contentPackageRepository,
            packageCatalogRepository =
                packageCatalogRepository,
            transactionRunner = transactionRunner,
            mediaDirectory = null
        )
    }

    fun createPersisted(
        persistenceDirectory: Path
    ): LearningApplicationContext {
        val contentLibrariesPath =
            persistenceDirectory.resolve(
                CONTENT_LIBRARIES_FILE_NAME
            )

        val installedPackagesPath =
            persistenceDirectory.resolve(
                INSTALLED_PACKAGES_FILE_NAME
            )

        val libraryCollectionsPath =
            persistenceDirectory.resolve(
                LIBRARY_COLLECTIONS_FILE_NAME
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


        val studyQueuesPath =
            persistenceDirectory.resolve(
                STUDY_QUEUES_FILE_NAME
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

        val libraryCollectionRepository =
            StoreBackedLibraryCollectionRepository(
                JsonLibraryCollectionStore(
                    libraryCollectionsPath
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


        val studyQueueRepository =
            StoreBackedStudyQueueRepository(
                JsonStudyQueueStore(
                    studyQueuesPath
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
                    installedPackagesPath,
                    contentLibrariesPath,
                    libraryCollectionsPath,
                    contentsPath,
                    learningItemsPath,
                    memoryStatesPath,
                    reviewEventsPath,
                    studySessionsPath,
                    studyQueuesPath,
                    contentPackagesPath,
                    packageCatalogsPath
                )
            )

        return createContext(
            contentLibraryRepository =
                contentLibraryRepository,
            libraryCollectionRepository =
                libraryCollectionRepository,
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
            studyQueueRepository =
                studyQueueRepository,
            contentPackageRepository =
                contentPackageRepository,
            packageCatalogRepository =
                packageCatalogRepository,
            transactionRunner = transactionRunner,
            mediaDirectory = persistenceDirectory.resolve(MEDIA_DIRECTORY_NAME),
            installedPackageRepository =
                StoreBackedInstalledPackageRepository(
                    JsonInstalledPackageStore(
                        installedPackagesPath
                    )
                ),
            knowledgeGraphRepository =
                StoreBackedKnowledgeGraphRepository(
                    JsonKnowledgeGraphStore(
                        persistenceDirectory.resolve(
                            KNOWLEDGE_GRAPH_FILE_NAME
                        )
                    )
                )
        )
    }

    private fun createContext(
        contentLibraryRepository:
        ContentLibraryRepository,
        libraryCollectionRepository:
        LibraryCollectionRepository,
        contentRepository:
        ContentRepository,
        learningItemRepository:
        LearningItemRepository,
        memoryStateRepository:
        MemoryStateRepository,
        reviewEventRepository:
        ReviewEventRepository,
        studySessionRepository:
        StudySessionRepository,
        studyQueueRepository:
        StudyQueueRepository,
        contentPackageRepository:
        ContentPackageRepository,
        packageCatalogRepository:
        PackageCatalogRepository,
        transactionRunner:
        TransactionRunner,
        mediaDirectory: Path?,
        installedPackageRepository:
        vn.loi.learning.domain.library.repository.InstalledPackageRepository =
            vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository(),
        knowledgeGraphRepository:
        vn.loi.learning.domain.knowledge.repository.KnowledgeGraphRepository? = null
    ): LearningApplicationContext {
        val studyQueue =
            StudyQueueFactory.create(
                repository = studyQueueRepository
            )

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
                studyQueueService =
                    studyQueue,
                transactionRunner =
                    transactionRunner,
                scheduler =
                    ValidatingScheduler(
                        delegate =
                            FsrsScheduler()
                    )
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
                memoryStateQuery =
                    memoryStateRepository as MemoryStateQuery,
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

        val topics =
            TopicQueryService(
                contentPackageRepository =
                    contentPackageRepository,
                contentLibraryRepository =
                    contentLibraryRepository,
                contentRepository =
                    contentRepository
            )

        val libraryCollections =
            LibraryCollectionQueryService(
                libraryCollectionRepository =
                    libraryCollectionRepository
            )

        val createLibraryCollection =
            CreateLibraryCollectionUseCase(
                contentLibraryRepository =
                    contentLibraryRepository,
                libraryCollectionRepository =
                    libraryCollectionRepository,
                transactionRunner =
                    transactionRunner
            )

        val renameLibraryCollection =
            RenameLibraryCollectionUseCase(
                libraryCollectionRepository =
                    libraryCollectionRepository,
                transactionRunner =
                    transactionRunner
            )

        val attachPackageToLibraryCollection =
            AttachPackageToLibraryCollectionUseCase(
                libraryCollectionRepository =
                    libraryCollectionRepository,
                contentPackageRepository =
                    contentPackageRepository,
                transactionRunner =
                    transactionRunner
            )

        val detachPackageFromLibraryCollection =
            DetachPackageFromLibraryCollectionUseCase(
                libraryCollectionRepository =
                    libraryCollectionRepository,
                transactionRunner =
                    transactionRunner
            )

        val deleteLibraryCollection =
            DeleteLibraryCollectionUseCase(
                libraryCollectionRepository =
                    libraryCollectionRepository,
                transactionRunner =
                    transactionRunner
            )

        val packageImporter:
                    (Path) -> PackageImportService =
            { packageDirectory ->
                createPackageImporter(
                    packageDirectory = packageDirectory,
                    progressListener = null,
                    mediaDirectory = mediaDirectory,
                    contentLibraryRepository = contentLibraryRepository,
                    contentRepository = contentRepository,
                    learningItemRepository = learningItemRepository,
                    contentPackageRepository = contentPackageRepository,
                    packageCatalogRepository = packageCatalogRepository,
                    transactionRunner = transactionRunner
                )
            }

        val packageImporterWithProgress:
                    (Path, PackageImportProgressListener) -> PackageImportService =
            { packageDirectory, progressListener ->
                createPackageImporter(
                    packageDirectory = packageDirectory,
                    progressListener = progressListener,
                    mediaDirectory = mediaDirectory,
                    contentLibraryRepository = contentLibraryRepository,
                    contentRepository = contentRepository,
                    learningItemRepository = learningItemRepository,
                    contentPackageRepository = contentPackageRepository,
                    packageCatalogRepository = packageCatalogRepository,
                    transactionRunner = transactionRunner
                )
            }

        val defaultLibraryId =
            vn.loi.learning.domain.library.model.LibraryId("default-library")
        val domainLibraryRepository =
            vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryRepository().apply {
                save(
                    vn.loi.learning.domain.library.model.Library.reconstitute(
                        id = defaultLibraryId,
                        name = "Learning Engine Library"
                    )
                )
            }
        val domainInstalledPackageRepository = installedPackageRepository
        val domainCollectionRepository =
            vn.loi.learning.infrastructure.persistence.memory.InMemoryCollectionRepository()
        val libraryQuery =
            vn.loi.learning.application.library.query.LibraryQueryService(
                libraryRepository = domainLibraryRepository,
                installedPackageRepository = domainInstalledPackageRepository,
                collectionRepository = domainCollectionRepository
            )

        val conflictAwareImporter =
            vn.loi.learning.application.contentpackaging.ConflictAwarePackageImporter(
                inspector = vn.loi.learning.application.contentpackaging.PackageImportInspector(domainInstalledPackageRepository),
                installedPackageRepository = domainInstalledPackageRepository,
                transactionRunner = transactionRunner
            )

        val knowledgeGraphQueryService = knowledgeGraphRepository?.let {
            KnowledgeGraphQueryService(it)
        }
        val saveKnowledgeGraphUseCase = knowledgeGraphRepository?.let {
            SaveKnowledgeGraphUseCase(it)
        }
        val getKnowledgeGraphUseCase = knowledgeGraphRepository?.let {
            GetKnowledgeGraphUseCase(it)
        }
        val installedLibraryGraphProjection =
            InstalledLibraryKnowledgeGraphProjection(
                installedPackageRepository = installedPackageRepository
            )

        return LearningApplicationContext(
            engine = engine,
            studyQueue = studyQueue,
            dashboard = dashboard,
            statistics = statistics,
            reviewHistory = reviewHistory,
            installedPackages = installedPackages,
            contentLibraries = contentLibraries,
            libraryContents = libraryContents,
            topics = topics,
            libraryCollections = libraryCollections,
            createLibraryCollection =
                createLibraryCollection,
            renameLibraryCollection =
                renameLibraryCollection,
            attachPackageToLibraryCollection =
                attachPackageToLibraryCollection,
            detachPackageFromLibraryCollection =
                detachPackageFromLibraryCollection,
            deleteLibraryCollection =
                deleteLibraryCollection,
            packageImporter = packageImporter,
            packageImporterWithProgress = packageImporterWithProgress,
            libraryQuery = libraryQuery,
            defaultLibraryId = defaultLibraryId,
            conflictAwareImporter = conflictAwareImporter,
            knowledgeGraphQuery = knowledgeGraphQueryService,
            saveKnowledgeGraph = saveKnowledgeGraphUseCase,
            getKnowledgeGraph = getKnowledgeGraphUseCase,
            installedLibraryGraphProjection = installedLibraryGraphProjection
        )
    }



    private fun createPackageImporter(
        packageDirectory: Path,
        progressListener: PackageImportProgressListener?,
        mediaDirectory: Path?,
        contentLibraryRepository: ContentLibraryRepository,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        contentPackageRepository: ContentPackageRepository,
        packageCatalogRepository: PackageCatalogRepository,
        transactionRunner: TransactionRunner
    ): PackageImportService =
                PersistedLearningPlatformFactory.create(
                    packageScanner =
                        ContentPackageImportFactory.createScanner(
                            packageDirectory
                        ),
                    packageInstaller =
                        ContentPackageImportFactory.createInstaller(),
                    packageContentImporter =
                        if (mediaDirectory == null) {
                            ContentPackageImportFactory.createContentImporter()
                        } else {
                            ContentPackageImportFactory.createContentImporter(mediaDirectory)
                        },
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
                        transactionRunner,
                    progressListener = progressListener
                )

    private const val INSTALLED_PACKAGES_FILE_NAME =
        "installed-packages.json"

    private const val CONTENT_LIBRARIES_FILE_NAME =
        "content-libraries.json"

    private const val LIBRARY_COLLECTIONS_FILE_NAME =
        "library-collections.json"

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

    private const val STUDY_QUEUES_FILE_NAME =
        "study-queues.json"

    private const val CONTENT_PACKAGES_FILE_NAME =
        "content-packages.json"

    private const val PACKAGE_CATALOGS_FILE_NAME =
        "package-catalogs.json"

    private const val KNOWLEDGE_GRAPH_FILE_NAME =
        "knowledge-graph.json"

    private const val MEDIA_DIRECTORY_NAME =
        "media"
}
