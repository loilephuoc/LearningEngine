package vn.loi.learning.infrastructure.persistence.sqlite

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import vn.loi.learning.infrastructure.persistence.json.*
import vn.loi.learning.infrastructure.persistence.record.*

data class JsonMigrationReport(
    val migrated: Boolean,
    val contentCount: Int = 0,
    val learningItemCount: Int = 0,
    val memoryStateCount: Int = 0,
    val reviewEventCount: Int = 0,
    val trajectoryCount: Int = 0,
    val studySessionCount: Int = 0,
    val studyQueueCount: Int = 0,
    val contentLibraryCount: Int = 0,
    val libraryCollectionCount: Int = 0,
    val contentPackageCount: Int = 0,
    val packageCatalogCount: Int = 0,
    val installedPackageCount: Int = 0,
    val canonicalLibraryCount: Int = 0,
    val canonicalCollectionCount: Int = 0,
    val continuousReviewIntentCount: Int = 0
)

object JsonToSqliteMigrationService {

    private const val MIGRATION_KEY = "json_migration_completed"

    fun isMigrationNeeded(database: LearningEngineDatabase): Boolean {
        val completed = database.migrationMetadataQueries.get(MIGRATION_KEY).executeAsOneOrNull()
        return completed == null || completed.value_ != "true"
    }

    fun migrateIfNeeded(
        persistenceDirectory: Path,
        database: LearningEngineDatabase
    ): JsonMigrationReport {
        if (!isMigrationNeeded(database)) {
            return JsonMigrationReport(migrated = false)
        }

        val contentLibrariesPath = persistenceDirectory.resolve("content-libraries.json")
        val libraryCollectionsPath = persistenceDirectory.resolve("library-collections.json")
        val contentsPath = persistenceDirectory.resolve("contents.json")
        val learningItemsPath = persistenceDirectory.resolve("learning-items.json")
        val memoryStatesPath = persistenceDirectory.resolve("memory-states.json")
        val reviewEventsPath = persistenceDirectory.resolve("review-events.json")
        val learningTrajectoriesPath = persistenceDirectory.resolve("learning-trajectories.json")
        val studySessionsPath = persistenceDirectory.resolve("study-sessions.json")
        val studyQueuesPath = persistenceDirectory.resolve("study-queues.json")
        val contentPackagesPath = persistenceDirectory.resolve("content-packages.json")
        val packageCatalogsPath = persistenceDirectory.resolve("package-catalogs.json")
        val installedPackagesPath = persistenceDirectory.resolve("installed-packages.json")
        val canonicalLibrariesPath = persistenceDirectory.resolve("canonical-libraries.json")
        val canonicalCollectionsPath = persistenceDirectory.resolve("canonical-library-collections.json")
        val continuousReviewIntentsPath = persistenceDirectory.resolve("continuous-review-intents.json")
        val knowledgeGraphPath = persistenceDirectory.resolve("knowledge-graph.json")
        val localSyncStatePath = persistenceDirectory.resolve("sync-state.json")

        val hasAnyJson = listOf(
            contentLibrariesPath, libraryCollectionsPath, contentsPath, learningItemsPath,
            memoryStatesPath, reviewEventsPath, learningTrajectoriesPath, studySessionsPath,
            studyQueuesPath, contentPackagesPath, packageCatalogsPath, installedPackagesPath,
            canonicalLibrariesPath, canonicalCollectionsPath, continuousReviewIntentsPath,
            knowledgeGraphPath, localSyncStatePath
        ).any { Files.exists(it) && Files.size(it) > 0L }

        if (!hasAnyJson) {
            val now = Instant.now().toEpochMilli()
            database.migrationMetadataQueries.insertOrReplace(MIGRATION_KEY, "true", now)
            return JsonMigrationReport(migrated = false)
        }

        // Load all legacy records
        val contentLibraryStore = JsonContentLibraryStore(contentLibrariesPath)
        val libraryCollectionStore = JsonLibraryCollectionStore(libraryCollectionsPath)
        val contentStore = JsonContentStore(contentsPath)
        val learningItemStore = JsonLearningItemStore(learningItemsPath)
        val memoryStateStore = JsonMemoryStateStore(memoryStatesPath)
        val reviewEventStore = JsonReviewEventStore(reviewEventsPath)
        val learningTrajectoryStore = JsonLearningTrajectoryStore(learningTrajectoriesPath)
        val studySessionStore = JsonStudySessionStore(studySessionsPath)
        val studyQueueStore = JsonStudyQueueStore(studyQueuesPath)
        val contentPackageStore = JsonContentPackageStore(contentPackagesPath)
        val packageCatalogStore = JsonPackageCatalogStore(packageCatalogsPath)
        val installedPackageStore = JsonInstalledPackageStore(installedPackagesPath)
        val canonicalLibraryStore = JsonCanonicalLibraryStore(canonicalLibrariesPath)
        val canonicalCollectionStore = JsonCanonicalCollectionStore(canonicalCollectionsPath)
        val knowledgeGraphStore = JsonKnowledgeGraphStore(knowledgeGraphPath)
        val continuousReviewIntentRepo = JsonContinuousReviewIntentRepository(continuousReviewIntentsPath)
        val localSyncStateRepo = JsonLocalSyncStateRepository(localSyncStatePath)

        val contentLibraries = contentLibraryStore.loadAll()
        val libraryCollections = libraryCollectionStore.loadAll()
        val contents = contentStore.loadAll()
        val learningItems = learningItemStore.loadAll()
        val memoryStates = memoryStateStore.load()
        val reviewEvents = reviewEventStore.loadAll()
        val trajectories = learningTrajectoryStore.loadAll()
        val studySessions = studySessionStore.loadAll()
        val studyQueues = studyQueueStore.loadAll()
        val contentPackages = contentPackageStore.loadAll()
        val packageCatalogs = packageCatalogStore.loadAll()
        val installedPackages = installedPackageStore.loadAll()
        val canonicalLibraries = canonicalLibraryStore.loadAll()
        val canonicalCollections = canonicalCollectionStore.loadAll()
        val knowledgeGraph = if (Files.exists(knowledgeGraphPath) && Files.size(knowledgeGraphPath) > 0L) {
            knowledgeGraphStore.load()
        } else null
        val continuousReviewIntents: List<ContinuousReviewIntentRecord> = if (Files.exists(continuousReviewIntentsPath) && Files.size(continuousReviewIntentsPath) > 0L) {
            try {
                val raw = String(Files.readAllBytes(continuousReviewIntentsPath), java.nio.charset.StandardCharsets.UTF_8)
                SqliteJsonUtils.decodeOrDefault(raw, emptyList())
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        // Repositories for insertion
        val contentRepo = SqliteContentRepository(database)
        val learningItemRepo = SqliteLearningItemRepository(database)
        val memoryStateRepo = SqliteMemoryStateRepository(database)
        val reviewEventRepo = SqliteReviewEventRepository(database)
        val trajectoryRepo = SqliteLearningTrajectoryRepository(database)
        val studySessionRepo = SqliteStudySessionRepository(database)
        val studyQueueRepo = SqliteStudyQueueRepository(database)
        val contentLibraryRepo = SqliteContentLibraryRepository(database)
        val libraryCollectionRepo = SqliteLibraryCollectionRepository(database)
        val contentPackageRepo = SqliteContentPackageRepository(database)
        val packageCatalogRepo = SqlitePackageCatalogRepository(database)
        val installedPackageRepo = SqliteInstalledPackageRepository(database)
        val canonicalLibraryRepo = SqliteCanonicalLibraryRepository(database)
        val canonicalCollectionRepo = SqliteCanonicalCollectionRepository(database)
        val knowledgeGraphRepo = SqliteKnowledgeGraphRepository(database)
        val continuousReviewIntentSqliteRepo = SqliteContinuousReviewIntentRepository(database)

        database.transaction {
            if (contents.isNotEmpty()) contentRepo.store.saveAll(contents)
            if (learningItems.isNotEmpty()) learningItemRepo.store.saveAll(learningItems)
            if (memoryStates.isNotEmpty()) memoryStateRepo.save(memoryStates)
            if (reviewEvents.isNotEmpty()) reviewEventRepo.saveAll(reviewEvents)
            if (trajectories.isNotEmpty()) trajectoryRepo.saveAll(trajectories)
            if (studySessions.isNotEmpty()) studySessionRepo.saveAll(studySessions)
            if (studyQueues.isNotEmpty()) studyQueueRepo.saveAll(studyQueues)
            if (contentLibraries.isNotEmpty()) contentLibraryRepo.store.saveAll(contentLibraries)
            if (libraryCollections.isNotEmpty()) libraryCollectionRepo.store.saveAll(libraryCollections)
            if (contentPackages.isNotEmpty()) contentPackageRepo.saveAll(contentPackages)
            if (packageCatalogs.isNotEmpty()) packageCatalogRepo.saveAll(packageCatalogs)
            if (installedPackages.isNotEmpty()) installedPackageRepo.saveAll(installedPackages)
            if (canonicalLibraries.isNotEmpty()) canonicalLibraryRepo.saveAll(canonicalLibraries)
            if (canonicalCollections.isNotEmpty()) canonicalCollectionRepo.saveAll(canonicalCollections)
            if (knowledgeGraph != null) knowledgeGraphRepo.save(knowledgeGraph)
            if (continuousReviewIntents.isNotEmpty()) continuousReviewIntentSqliteRepo.saveAll(continuousReviewIntents)

            val now = Instant.now().toEpochMilli()
            database.migrationMetadataQueries.insertOrReplace(MIGRATION_KEY, "true", now)
        }

        // Validate counts
        val sqliteContents = contentRepo.findAll()
        val sqliteLearningItems = learningItemRepo.store.loadAll()
        val sqliteMemoryStates = memoryStateRepo.load()
        val sqliteReviewEvents = reviewEventRepo.loadAll()
        val sqliteSessions = studySessionRepo.loadAll()
        val sqliteQueues = studyQueueRepo.loadAll()
        val sqlitePackages = contentPackageRepo.loadAll()
        val sqliteInstalled = installedPackageRepo.loadAll()

        check(sqliteContents.size == contents.size) {
            "Content migration count mismatch: JSON had ${contents.size}, SQLite has ${sqliteContents.size}"
        }
        check(sqliteLearningItems.size == learningItems.size) {
            "LearningItem migration count mismatch: JSON had ${learningItems.size}, SQLite has ${sqliteLearningItems.size}"
        }
        check(sqliteMemoryStates.size == memoryStates.size) {
            "MemoryState migration count mismatch: JSON had ${memoryStates.size}, SQLite has ${sqliteMemoryStates.size}"
        }
        check(sqliteReviewEvents.size == reviewEvents.size) {
            "ReviewEvent migration count mismatch: JSON had ${reviewEvents.size}, SQLite has ${sqliteReviewEvents.size}"
        }
        check(sqliteSessions.size == studySessions.size) {
            "StudySession migration count mismatch: JSON had ${studySessions.size}, SQLite has ${sqliteSessions.size}"
        }
        check(sqliteQueues.size == studyQueues.size) {
            "StudyQueue migration count mismatch: JSON had ${studyQueues.size}, SQLite has ${sqliteQueues.size}"
        }
        check(sqlitePackages.size == contentPackages.size) {
            "ContentPackage migration count mismatch: JSON had ${contentPackages.size}, SQLite has ${sqlitePackages.size}"
        }
        check(sqliteInstalled.size == installedPackages.size) {
            "InstalledPackage migration count mismatch: JSON had ${installedPackages.size}, SQLite has ${sqliteInstalled.size}"
        }

        return JsonMigrationReport(
            migrated = true,
            contentCount = contents.size,
            learningItemCount = learningItems.size,
            memoryStateCount = memoryStates.size,
            reviewEventCount = reviewEvents.size,
            trajectoryCount = trajectories.size,
            studySessionCount = studySessions.size,
            studyQueueCount = studyQueues.size,
            contentLibraryCount = contentLibraries.size,
            libraryCollectionCount = libraryCollections.size,
            contentPackageCount = contentPackages.size,
            packageCatalogCount = packageCatalogs.size,
            installedPackageCount = installedPackages.size,
            canonicalLibraryCount = canonicalLibraries.size,
            canonicalCollectionCount = canonicalCollections.size,
            continuousReviewIntentCount = 0
        )
    }
}
