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
import vn.loi.learning.application.port.RecoveryCoordinatedTransactionRunner
import vn.loi.learning.application.port.RecoveryOperationGate
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
import vn.loi.learning.infrastructure.persistence.json.JsonContinuousReviewIntentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContinuousReviewIntentRepository
import vn.loi.learning.application.port.ContinuousReviewIntentRepository
import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningTrajectoryRepository
import vn.loi.learning.infrastructure.persistence.json.JsonLearningTrajectoryStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningTrajectoryRepository

object LearningApplicationFactory {

    /** Opens every canonical persisted store without mutation; any parse failure rejects recovery staging. */
    fun validatePersisted(
        persistenceDirectory: Path,
        checkpoint: (String) -> Unit = {}
    ) {
        val context = createPersisted(persistenceDirectory, reconcilePartOfSpeechRegistryOnCreate = false)
        checkpoint("content-libraries")
        val libraries = context.contentLibraryRepository?.findAll().orEmpty()
        checkpoint("content-packages")
        val packages = context.contentPackageRepository?.findAll().orEmpty()
        checkpoint("contents")
        val contents = context.contentRepository?.findAll().orEmpty()
        checkpoint("learning-items")
        val items = context.learningItemRepository?.findAll().orEmpty()
        checkpoint("memory-states")
        val memoryStates = context.memoryStateRepository?.findAll().orEmpty()
        checkpoint("review-events")
        val reviewEvents = context.reviewEventRepository?.findAll().orEmpty()
        checkpoint("learning-trajectories")
        val trajectories = context.learningTrajectoryRepository?.findAll().orEmpty()
        checkpoint("study-sessions")
        val sessions = context.studySessionRepository?.findAll().orEmpty()
        checkpoint("study-queues")
        val queues = context.studyQueueRepository?.findAll().orEmpty()
        checkpoint("package-catalogs")
        context.packageCatalog?.findAll()
        checkpoint("installed-packages")
        val installedPackages = context.installedPackageRepository?.findAll().orEmpty()
        checkpoint("local-sync-state")
        (context.localSyncStateRepository as? vn.loi.learning.infrastructure.persistence.json.JsonLocalSyncStateRepository)
            ?.validate()

        val contentIds = contents.mapTo(hashSetOf()) { it.id }
        val libraryIds = libraries.mapTo(hashSetOf()) { it.id }
        val itemIds = items.mapTo(hashSetOf()) { it.id }
        require(items.all { it.contentId in contentIds }) { "LearningItem references missing Content." }
        require(memoryStates.all { it.learningItemId in itemIds }) { "MemoryState references missing LearningItem." }
        require(reviewEvents.all { it.learningItemId in itemIds }) { "ReviewEvent references missing LearningItem." }
        require(queues.all { queue -> queue.learningItemIds.all { it in itemIds } }) {
            "StudyQueue references missing LearningItem."
        }
        require(libraries.all { library -> library.contentIds.all { it in contentIds } }) {
            "ContentLibrary references missing Content."
        }
        require(packages.all { contentPackage -> contentPackage.libraryIds.all { it in libraryIds } }) {
            "ContentPackage references missing ContentLibrary."
        }
        require(contents.map { it.id }.distinct().size == contents.size) { "Duplicate Content IDs." }
        require(items.map { it.id }.distinct().size == items.size) { "Duplicate LearningItem IDs." }
        require(libraries.map { it.id }.distinct().size == libraries.size) { "Duplicate ContentLibrary IDs." }
        require(packages.map { it.id }.distinct().size == packages.size) { "Duplicate ContentPackage IDs." }
        require(installedPackages.map { it.id }.distinct().size == installedPackages.size) { "Duplicate InstalledPackage IDs." }
        require(sessions.map { it.id }.distinct().size == sessions.size) { "Duplicate StudySession IDs." }
        require(queues.map { it.sessionId }.distinct().size == queues.size) { "Duplicate StudyQueue session IDs." }
        require(reviewEvents.map { it.id }.distinct().size == reviewEvents.size) { "Duplicate ReviewEvent IDs." }
        require(memoryStates.map { it.learnerId to it.learningItemId }.distinct().size == memoryStates.size) {
            "Duplicate MemoryState identities."
        }
        require(trajectories.map { it.learnerId to it.trajectory.contentId }.distinct().size == trajectories.size) {
            "Duplicate LearningTrajectory identities."
        }
    }

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

        val recoveryOperationGate = RecoveryOperationGate()
        val transactionRunner =
            RecoveryCoordinatedTransactionRunner(
                InMemoryTransactionRunner(),
                recoveryOperationGate
            )

        val continuousReviewIntentRepository =
            InMemoryContinuousReviewIntentRepository()
        val learningTrajectoryRepository = InMemoryLearningTrajectoryRepository()

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
            continuousReviewIntentRepository = continuousReviewIntentRepository,
            learningTrajectoryRepository = learningTrajectoryRepository,
            mediaDirectory = null,
            recoveryOperationGate = recoveryOperationGate
        )
    }

    fun createPersisted(
        persistenceDirectory: Path,
        reconcilePartOfSpeechRegistryOnCreate: Boolean = true
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
        val learningTrajectoriesPath = persistenceDirectory.resolve(LEARNING_TRAJECTORIES_FILE_NAME)

        val studySessionsPath =
            persistenceDirectory.resolve(
                STUDY_SESSIONS_FILE_NAME
            )


        val studyQueuesPath =
            persistenceDirectory.resolve(
                STUDY_QUEUES_FILE_NAME
            )

        val continuousReviewIntentsPath =
            persistenceDirectory.resolve(CONTINUOUS_REVIEW_INTENTS_FILE_NAME)

        val localSyncStatePath = persistenceDirectory.resolve(LOCAL_SYNC_STATE_FILE_NAME)
        val localSyncStateRepository =
            vn.loi.learning.infrastructure.persistence.json.JsonLocalSyncStateRepository(localSyncStatePath)

        val contentPackagesPath =
            persistenceDirectory.resolve(
                CONTENT_PACKAGES_FILE_NAME
            )

        val packageCatalogsPath =
            persistenceDirectory.resolve(
                PACKAGE_CATALOGS_FILE_NAME
            )

        val canonicalLibrariesPath =
            persistenceDirectory.resolve(
                CANONICAL_LIBRARIES_FILE_NAME
            )

        val canonicalCollectionsPath =
            persistenceDirectory.resolve(
                CANONICAL_COLLECTIONS_FILE_NAME
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

        val recoveryOperationGate = RecoveryOperationGate()
        val continuousReviewIntentRepository =
            JsonContinuousReviewIntentRepository(continuousReviewIntentsPath) { mutation ->
                recoveryOperationGate.canonicalMutation(mutation)
            }
        val learningTrajectoryRepository = StoreBackedLearningTrajectoryRepository(
            JsonLearningTrajectoryStore(learningTrajectoriesPath)
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

        val canonicalLibraryRepository =
            vn.loi.learning.infrastructure.persistence.repository.StoreBackedCanonicalLibraryRepository(
                vn.loi.learning.infrastructure.persistence.json.JsonCanonicalLibraryStore(
                    canonicalLibrariesPath
                )
            )

        val canonicalCollectionRepository =
            vn.loi.learning.infrastructure.persistence.repository.StoreBackedCanonicalCollectionRepository(
                vn.loi.learning.infrastructure.persistence.json.JsonCanonicalCollectionStore(
                    canonicalCollectionsPath
                )
            )

        val transactionRunner = RecoveryCoordinatedTransactionRunner(
            JsonFileTransactionRunner(
                listOf(
                    installedPackagesPath,
                    canonicalLibrariesPath,
                    canonicalCollectionsPath,
                    contentLibrariesPath,
                    libraryCollectionsPath,
                    contentsPath,
                    learningItemsPath,
                    memoryStatesPath,
                    reviewEventsPath,
                    learningTrajectoriesPath,
                    studySessionsPath,
                    studyQueuesPath,
                    continuousReviewIntentsPath,
                    localSyncStatePath,
                    contentPackagesPath,
                    packageCatalogsPath
                )
            ),
            recoveryOperationGate
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
            continuousReviewIntentRepository = continuousReviewIntentRepository,
            learningTrajectoryRepository = learningTrajectoryRepository,
            localSyncStateRepository = localSyncStateRepository,
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
                ),
            domainLibraryRepository = canonicalLibraryRepository,
            domainCollectionRepository = canonicalCollectionRepository,
            reconcilePartOfSpeechRegistryOnCreate = reconcilePartOfSpeechRegistryOnCreate,
            recoveryOperationGate = recoveryOperationGate
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
        continuousReviewIntentRepository:
        ContinuousReviewIntentRepository,
        learningTrajectoryRepository: LearningTrajectoryRepository,
        localSyncStateRepository: vn.loi.learning.application.sync.LocalSyncStateRepository? = null,
        mediaDirectory: Path?,
        installedPackageRepository:
        vn.loi.learning.domain.library.repository.InstalledPackageRepository =
            vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository(),
        knowledgeGraphRepository:
        vn.loi.learning.domain.knowledge.repository.KnowledgeGraphRepository? = null,
        domainLibraryRepository:
        vn.loi.learning.domain.library.repository.LibraryRepository? = null,
        domainCollectionRepository:
        vn.loi.learning.domain.library.repository.CollectionRepository? = null,
        reconcilePartOfSpeechRegistryOnCreate: Boolean = true,
        recoveryOperationGate: RecoveryOperationGate? = null
    ): LearningApplicationContext {
        val partOfSpeechRegistry =
            vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry()
        if (reconcilePartOfSpeechRegistryOnCreate) {
            vn.loi.learning.application.partofspeech.PartOfSpeechRegistryReconciler(
                contentRepository = contentRepository,
                registry = partOfSpeechRegistry
            ).reconcile()
        }

        val studyQueue =
            StudyQueueFactory.create(
                repository = studyQueueRepository
            )

        var packageContentQueryRef: vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService? = null
        var topicsRef: TopicQueryService? = null

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
                memoryStateQuery =
                    memoryStateRepository as MemoryStateQuery,
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
                    ),
                packageContentQuerySupplier = { packageContentQueryRef },
                topicQueryServiceSupplier = { topicsRef },
                installedPackageRepository = installedPackageRepository,
                continuousReviewIntentRepository = continuousReviewIntentRepository,
                learningTrajectoryRepository = learningTrajectoryRepository
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

        lateinit var completePackageImportLifecycle:
            vn.loi.learning.application.contentpackaging.CompletePackageImportLifecycleUseCase

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
                    transactionRunner = transactionRunner,
                    installedPackageRepository = installedPackageRepository,
                    memoryStateRepository = memoryStateRepository,
                    reviewEventRepository = reviewEventRepository,
                    studySessionRepository = studySessionRepository,
                    studyQueueRepository = studyQueueRepository,
                    partOfSpeechRegistry = partOfSpeechRegistry,
                    packageLifecycleCompletion = { result -> completePackageImportLifecycle.execute(listOf(result)).single() }
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
                    transactionRunner = transactionRunner,
                    installedPackageRepository = installedPackageRepository,
                    memoryStateRepository = memoryStateRepository,
                    reviewEventRepository = reviewEventRepository,
                    studySessionRepository = studySessionRepository,
                    studyQueueRepository = studyQueueRepository,
                    partOfSpeechRegistry = partOfSpeechRegistry,
                    packageLifecycleCompletion = { result -> completePackageImportLifecycle.execute(listOf(result)).single() }
                )
            }

        val defaultLibraryId =
            vn.loi.learning.domain.library.model.LibraryId("default-library")

        val domainLibRepo = domainLibraryRepository ?:
            vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryRepository()
        val domainCollRepo = domainCollectionRepository ?:
            vn.loi.learning.infrastructure.persistence.memory.InMemoryCollectionRepository()

        // Safe Default Library Bootstrap (R2-03): Load if exists, create/reconcile once if missing
        val existingDefaultLib = domainLibRepo.findById(defaultLibraryId)
        if (existingDefaultLib == null) {
            val initialEntries = installedPackageRepository.findAllByLibraryId(defaultLibraryId).map {
                vn.loi.learning.domain.library.model.LibraryEntry(
                    installedPackageId = it.id,
                    packageId = it.packageId,
                    registeredAt = it.installedAt
                )
            }
            domainLibRepo.save(
                vn.loi.learning.domain.library.model.Library.reconstitute(
                    id = defaultLibraryId,
                    name = "Learning Engine Library",
                    entries = initialEntries
                )
            )
        } else {
            val existingEntryIds = existingDefaultLib.entries.map { it.installedPackageId }.toSet()
            val unregisteredPackages = installedPackageRepository.findAllByLibraryId(defaultLibraryId)
                .filterNot { it.id in existingEntryIds }
            if (unregisteredPackages.isNotEmpty()) {
                var reconciledLib: vn.loi.learning.domain.library.model.Library = existingDefaultLib
                for (pkg in unregisteredPackages) {
                    reconciledLib = reconciledLib.registerEntry(pkg.id, pkg.packageId, pkg.installedAt)
                }
                domainLibRepo.save(reconciledLib)
            }
        }

        val domainInstalledPackageRepository = installedPackageRepository
        val libraryQuery =
            vn.loi.learning.application.library.query.LibraryQueryService(
                libraryRepository = domainLibRepo,
                installedPackageRepository = domainInstalledPackageRepository,
                collectionRepository = domainCollRepo
            )
        val libraryCommand =
            vn.loi.learning.application.library.command.LibraryCommandService(
                libraryRepository = domainLibRepo,
                installedPackageRepository = domainInstalledPackageRepository,
                collectionRepository = domainCollRepo,
                transactionRunner = transactionRunner
            )

        val conflictAwareImporter =
            vn.loi.learning.application.contentpackaging.ConflictAwarePackageImporter(
                inspector = vn.loi.learning.application.contentpackaging.PackageImportInspector(domainInstalledPackageRepository),
                installedPackageRepository = domainInstalledPackageRepository,
                transactionRunner = transactionRunner
            )

        val packageUninstallOp =
            vn.loi.learning.application.contentpackaging.PackageUninstallOperation(
                contentLibraryRepository = contentLibraryRepository,
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository,
                contentPackageRepository = contentPackageRepository,
                packageCatalogRepository = packageCatalogRepository,
                installedPackageRepository = domainInstalledPackageRepository,
                libraryRepository = domainLibRepo,
                collectionRepository = domainCollRepo,
                memoryStateRepository = memoryStateRepository,
                reviewEventRepository = reviewEventRepository,
                studySessionRepository = studySessionRepository,
                studyQueueRepository = studyQueueRepository,
                contentMediaStorage = mediaDirectory?.let { vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(it) }
            )


        val uninstallContentPackageUseCase =
            vn.loi.learning.application.contentpackaging.UninstallContentPackageUseCase(
                uninstallOperation = packageUninstallOp,
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

        val packageContentQuery =
            vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService(
                installedPackages = installedPackages,
                libraryContents = libraryContents,
                libraryQuery = libraryQuery,
                defaultLibraryIdSupplier = { defaultLibraryId }
            )

        topicsRef = topics
        packageContentQueryRef = packageContentQuery

        val packageProgress =
            vn.loi.learning.application.packageprogress.PackageLearningProgressQueryService(
                packageContentQuery = packageContentQuery,
                engine = engine,
                memoryStateQuery = memoryStateRepository as MemoryStateQuery
            )
        val packageLatestRatings =
            vn.loi.learning.application.packageprogress.PackageLatestRatingQueryService(
                packageContentQuery = packageContentQuery,
                engine = engine,
                reviewEventRepository = reviewEventRepository
            )
        val studyHeaderStatistics =
            vn.loi.learning.application.packageprogress.StudyHeaderStatisticsQueryService(
                engine = engine,
                memoryStateQuery = memoryStateRepository as MemoryStateQuery,
                reviewEventRepository = reviewEventRepository
            )

        val exportContentPackageUseCase = vn.loi.learning.application.contentpackaging.export.DefaultExportContentPackageUseCase(
            installedPackageRepository = domainInstalledPackageRepository,
            contentPackageRepository = contentPackageRepository,
            contentLibraryRepository = contentLibraryRepository,
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            opd3PackageExporter = vn.loi.learning.application.contentpackaging.Opd3PackageExporter(zipWriter = vn.loi.learning.infrastructure.contentpackaging.JvmDeterministicZipWriter()),
            mediaDirectory = mediaDirectory,
            contentMediaStorage = mediaDirectory?.let { vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(it) }
        )

        val packageBrowserQuery =
            vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService(
                installedPackageRepository = domainInstalledPackageRepository,
                installedPackages = installedPackages,
                contentPackageRepository = contentPackageRepository,
                contentLibraryRepository = contentLibraryRepository,
                contentRepository = contentRepository,
                learningItemRepository = learningItemRepository
            )
        val contentBrowserEdit =
            vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
                contentRepository = contentRepository,
                contentLibraryRepository = contentLibraryRepository,
                installedPackageRepository = domainInstalledPackageRepository,
                contentPackageRepository = contentPackageRepository,
                transactionRunner = transactionRunner,
                studySessionRepository = studySessionRepository,
                mediaStorage = mediaDirectory?.let { vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(it) }
            )
        val lessonBrowser = vn.loi.learning.application.contentpackaging.browser.LessonBrowserQueryService(packageBrowserQuery)
        val scopedStudy = vn.loi.learning.application.session.ScopedStudySessionService(
            packageItems = packageBrowserQuery::getBrowserItemsForPackage,
            collectionPackages = { id -> libraryQuery.getCollectionNode(id)?.assignedPackages?.map { it.id } },
            startSession = engine::startSession
        )
        val packageVerifier = vn.loi.learning.application.contentpackaging.Opd3PackageVerifier()
        val upgradeContentPackage = vn.loi.learning.application.contentpackaging.UpgradeContentPackageUseCase(
            vn.loi.learning.application.contentpackaging.PackageUpgradeOperation(contentPackageRepository, packageCatalogRepository),
            transactionRunner
        )

        val learningInsightClock = vn.loi.learning.domain.study.evidence.EvidenceClock {
            vn.loi.learning.domain.study.memory.model.Moment(System.currentTimeMillis())
        }
        val learningInsights = vn.loi.learning.application.learninginsight.GetLearningInsightUseCase(
            trajectories = learningTrajectoryRepository,
            profiles = vn.loi.learning.domain.study.evidence.LearningDifficultyProfileCalculator(
                learningInsightClock
            ),
            strategy = vn.loi.learning.domain.study.evidence.AdaptiveLearningStrategy(),
            projector = vn.loi.learning.application.learninginsight.LearningInsightProjector(),
            clock = learningInsightClock
        )

        completePackageImportLifecycle =
            vn.loi.learning.application.contentpackaging.CompletePackageImportLifecycleUseCase(
                conflictAwareImporter, domainLibRepo, defaultLibraryId, domainInstalledPackageRepository,
                libraryCommand, contentLibraryRepository, contentRepository, learningItemRepository
            )
        val activeStudySessionScopeReconciler =
            vn.loi.learning.application.session.ActiveStudySessionScopeReconciler(
                studySessionRepository,
                studyQueueRepository,
                domainInstalledPackageRepository,
                packageContentQuery,
                learningItemRepository,
                engine::leaveActiveStudySession
            )
        val dailyStudyBudget = vn.loi.learning.application.study.DailyStudyBudgetQueryService(
            reviewEventRepository, memoryStateRepository, learningItemRepository
        )
        val packageIntegrityChecker = vn.loi.learning.application.integrity.PackageIntegrityChecker(
            installedPackages = domainInstalledPackageRepository,
            contentPackages = contentPackageRepository,
            contentLibraries = contentLibraryRepository,
            contents = contentRepository,
            learningItems = learningItemRepository,
            libraries = domainLibRepo,
            mediaStorage = null,
            studySessions = studySessionRepository,
            studyQueues = studyQueueRepository,
            memoryStates = memoryStateRepository,
            reviewEvents = reviewEventRepository,
            trajectories = learningTrajectoryRepository
        )
        val intermediatePublicTransportRepair =
            vn.loi.learning.application.integrity.ReconcileIntermediatePublicTransportOrphan(
                contentRepository, learningItemRepository, domainInstalledPackageRepository,
                contentPackageRepository, contentLibraryRepository, memoryStateRepository,
                reviewEventRepository, learningTrajectoryRepository, studySessionRepository,
                studyQueueRepository, transactionRunner
            )

        val syncEngine = vn.loi.learning.infrastructure.sync.SyncEngine(
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            memoryStateRepository = memoryStateRepository,
            reviewEventRepository = reviewEventRepository,
            installedPackageRepository = domainInstalledPackageRepository,
            contentLibraryRepository = contentLibraryRepository,
            contentPackageRepository = contentPackageRepository,
            mediaStorage = mediaDirectory?.let { vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(it) },
            transactionRunner = transactionRunner
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
            libraryCommand = libraryCommand,
            defaultLibraryId = defaultLibraryId,
            conflictAwareImporter = conflictAwareImporter,
            uninstallContentPackage = uninstallContentPackageUseCase,
            knowledgeGraphQuery = knowledgeGraphQueryService,
            saveKnowledgeGraph = saveKnowledgeGraphUseCase,
            getKnowledgeGraph = getKnowledgeGraphUseCase,
            installedLibraryGraphProjection = installedLibraryGraphProjection,
            domainLibraryRepository = domainLibRepo,
            memoryStateRepository = memoryStateRepository,
            learningItemRepository = learningItemRepository,
            packageContentQuery = packageContentQuery,
            packageProgress = packageProgress,
            packageLatestRatings = packageLatestRatings,
            studyHeaderStatistics = studyHeaderStatistics,
            packageCatalog = packageCatalogRepository,
            contentPackageRepository = contentPackageRepository,
            contentLibraryRepository = contentLibraryRepository,
            contentRepository = contentRepository,
            installedPackageRepository = domainInstalledPackageRepository,
            studySessionRepository = studySessionRepository,
            studyQueueRepository = studyQueueRepository,
            reviewEventRepository = reviewEventRepository,
            learningTrajectoryRepository = learningTrajectoryRepository,
            learningInsights = learningInsights,
            exportContentPackage = exportContentPackageUseCase,
            packageBrowserQuery = packageBrowserQuery,
            contentBrowserEdit = contentBrowserEdit,
            transactionRunner = transactionRunner,
            lessonBrowser = lessonBrowser,
            scopedStudy = scopedStudy,
            packageVerifier = packageVerifier,
            upgradeContentPackage = upgradeContentPackage,
            partOfSpeechRegistry = partOfSpeechRegistry,
            completePackageImportLifecycle = completePackageImportLifecycle,
            activeStudySessionScopeReconciler = activeStudySessionScopeReconciler,
            dailyStudyBudget = dailyStudyBudget,
            packageIntegrityChecker = packageIntegrityChecker,
            intermediatePublicTransportRepair = intermediatePublicTransportRepair,
            recoveryOperationGate = recoveryOperationGate,
            syncEngine = syncEngine,
            localSyncStateRepository = localSyncStateRepository,
            localSyncCoordinator = localSyncStateRepository?.let {
                vn.loi.learning.application.sync.LocalSyncCoordinator(it, transactionRunner)
            }
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
        transactionRunner: TransactionRunner,
        installedPackageRepository: vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null,
        memoryStateRepository: MemoryStateRepository? = null,
        reviewEventRepository: ReviewEventRepository? = null,
        studySessionRepository: StudySessionRepository? = null,
        studyQueueRepository: StudyQueueRepository? = null,
        partOfSpeechRegistry: vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry? = null,
        packageLifecycleCompletion: ((vn.loi.learning.application.contentpackaging.PackageImportResult) -> vn.loi.learning.application.contentpackaging.PackageImportOutcome)? = null
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
                            ContentPackageImportFactory.createContentImporter(mediaDirectory, installedPackageRepository)
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
                    progressListener = progressListener,
                    installedPackageRepository = installedPackageRepository,
                    memoryStateRepository = memoryStateRepository,
                    reviewEventRepository = reviewEventRepository,
                    studySessionRepository = studySessionRepository,
                    studyQueueRepository = studyQueueRepository,
                    partOfSpeechRegistry = partOfSpeechRegistry,
                    packageLifecycleCompletion = packageLifecycleCompletion
                )

    private const val INSTALLED_PACKAGES_FILE_NAME =
        "installed-packages.json"

    private const val CANONICAL_LIBRARIES_FILE_NAME =
        "canonical-libraries.json"

    private const val CANONICAL_COLLECTIONS_FILE_NAME =
        "canonical-library-collections.json"

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

    private const val LEARNING_TRAJECTORIES_FILE_NAME =
        "learning-trajectories.json"

    private const val STUDY_SESSIONS_FILE_NAME =
        "study-sessions.json"

    private const val STUDY_QUEUES_FILE_NAME =
        "study-queues.json"

    private const val CONTINUOUS_REVIEW_INTENTS_FILE_NAME =
        "continuous-review-intents.json"

    private const val LOCAL_SYNC_STATE_FILE_NAME = "sync-state.json"

    private const val CONTENT_PACKAGES_FILE_NAME =
        "content-packages.json"

    private const val PACKAGE_CATALOGS_FILE_NAME =
        "package-catalogs.json"

    private const val KNOWLEDGE_GRAPH_FILE_NAME =
        "knowledge-graph.json"

    private const val MEDIA_DIRECTORY_NAME =
        "media"
}
