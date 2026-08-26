package vn.loi.learning.infrastructure.persistence.sqlite

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.infrastructure.persistence.json.*

object SqliteToJsonExportService {

    fun exportToJsonDirectory(
        database: LearningEngineDatabase,
        targetDir: Path
    ) {
        Files.createDirectories(targetDir)

        val contentRepo = SqliteContentRepository(database)
        val contentStore = JsonContentStore(targetDir.resolve("contents.json"))
        val contentRecords = contentRepo.store.loadAll()
        if (contentRecords.isNotEmpty() || !Files.exists(targetDir.resolve("contents.json"))) {
            contentStore.saveAll(contentRecords)
        }

        val itemRepo = SqliteLearningItemRepository(database)
        val itemStore = JsonLearningItemStore(targetDir.resolve("learning-items.json"))
        val itemRecords = itemRepo.store.loadAll()
        if (itemRecords.isNotEmpty() || !Files.exists(targetDir.resolve("learning-items.json"))) {
            itemStore.saveAll(itemRecords)
        }

        val memoryRepo = SqliteMemoryStateRepository(database)
        val memoryStore = JsonMemoryStateStore(targetDir.resolve("memory-states.json"))
        val memoryRecords = memoryRepo.loadAll()
        if (memoryRecords.isNotEmpty() || !Files.exists(targetDir.resolve("memory-states.json"))) {
            memoryStore.save(memoryRecords)
        }

        val reviewRepo = SqliteReviewEventRepository(database)
        val reviewStore = JsonReviewEventStore(targetDir.resolve("review-events.json"))
        val reviewRecords = reviewRepo.loadAll()
        if (reviewRecords.isNotEmpty() || !Files.exists(targetDir.resolve("review-events.json"))) {
            reviewStore.saveAll(reviewRecords)
        }

        val trajectoryRepo = SqliteLearningTrajectoryRepository(database)
        val trajectoryStore = JsonLearningTrajectoryStore(targetDir.resolve("learning-trajectories.json"))
        val trajectoryRecords = trajectoryRepo.loadAll()
        if (trajectoryRecords.isNotEmpty() || !Files.exists(targetDir.resolve("learning-trajectories.json"))) {
            trajectoryStore.saveAll(trajectoryRecords)
        }

        val sessionRepo = SqliteStudySessionRepository(database)
        val sessionStore = JsonStudySessionStore(targetDir.resolve("study-sessions.json"))
        val sessionRecords = sessionRepo.loadAll()
        if (sessionRecords.isNotEmpty() || !Files.exists(targetDir.resolve("study-sessions.json"))) {
            sessionStore.saveAll(sessionRecords)
        }

        val queueRepo = SqliteStudyQueueRepository(database)
        val queueStore = JsonStudyQueueStore(targetDir.resolve("study-queues.json"))
        val queueRecords = queueRepo.loadAll()
        if (queueRecords.isNotEmpty() || !Files.exists(targetDir.resolve("study-queues.json"))) {
            queueStore.saveAll(queueRecords)
        }

        val libraryRepo = SqliteContentLibraryRepository(database)
        val libraryStore = JsonContentLibraryStore(targetDir.resolve("content-libraries.json"))
        val libraryRecords = libraryRepo.store.loadAll()
        if (libraryRecords.isNotEmpty() || !Files.exists(targetDir.resolve("content-libraries.json"))) {
            libraryStore.saveAll(libraryRecords)
        }

        val collectionRepo = SqliteLibraryCollectionRepository(database)
        val collectionStore = JsonLibraryCollectionStore(targetDir.resolve("library-collections.json"))
        val collectionRecords = collectionRepo.store.loadAll()
        if (collectionRecords.isNotEmpty() || !Files.exists(targetDir.resolve("library-collections.json"))) {
            collectionStore.saveAll(collectionRecords)
        }

        val packageRepo = SqliteContentPackageRepository(database)
        val packageStore = JsonContentPackageStore(targetDir.resolve("content-packages.json"))
        val packageRecords = packageRepo.loadAll()
        if (packageRecords.isNotEmpty() || !Files.exists(targetDir.resolve("content-packages.json"))) {
            packageStore.saveAll(packageRecords)
        }

        val catalogRepo = SqlitePackageCatalogRepository(database)
        val catalogStore = JsonPackageCatalogStore(targetDir.resolve("package-catalogs.json"))
        val catalogRecords = catalogRepo.loadAll()
        if (catalogRecords.isNotEmpty() || !Files.exists(targetDir.resolve("package-catalogs.json"))) {
            catalogStore.saveAll(catalogRecords)
        }

        val installedRepo = SqliteInstalledPackageRepository(database)
        val installedStore = JsonInstalledPackageStore(targetDir.resolve("installed-packages.json"))
        val installedRecords = installedRepo.loadAll()
        if (installedRecords.isNotEmpty() || !Files.exists(targetDir.resolve("installed-packages.json"))) {
            installedStore.saveAll(installedRecords)
        }

        val canonicalLibRepo = SqliteCanonicalLibraryRepository(database)
        val canonicalLibStore = JsonCanonicalLibraryStore(targetDir.resolve("canonical-libraries.json"))
        val canonicalLibRecords = canonicalLibRepo.loadAll()
        if (canonicalLibRecords.isNotEmpty() || !Files.exists(targetDir.resolve("canonical-libraries.json"))) {
            canonicalLibStore.saveAll(canonicalLibRecords)
        }

        val canonicalColRepo = SqliteCanonicalCollectionRepository(database)
        val canonicalColStore = JsonCanonicalCollectionStore(targetDir.resolve("canonical-collections.json"))
        val canonicalColRecords = canonicalColRepo.loadAll()
        if (canonicalColRecords.isNotEmpty() || !Files.exists(targetDir.resolve("canonical-collections.json"))) {
            canonicalColStore.saveAll(canonicalColRecords)
        }

        val intentRepo = SqliteContinuousReviewIntentRepository(database)
        val intentStore = JsonContinuousReviewIntentRepository(targetDir.resolve("continuous-review-intents.json"))
        val intentRecords = intentRepo.loadAll()
        if (intentRecords.isNotEmpty() || !Files.exists(targetDir.resolve("continuous-review-intents.json"))) {
            intentRecords.map { it.toDomain() }.forEach { intentStore.save(it) }
        }

        val kgRepo = SqliteKnowledgeGraphRepository(database)
        val kgStore = JsonKnowledgeGraphStore(targetDir.resolve("knowledge-graph.json"))
        val kgRecord = kgRepo.load()
        if (kgRecord.nodes.isNotEmpty() || kgRecord.edges.isNotEmpty() || !Files.exists(targetDir.resolve("knowledge-graph.json"))) {
            kgStore.save(kgRecord)
        }
    }
}
