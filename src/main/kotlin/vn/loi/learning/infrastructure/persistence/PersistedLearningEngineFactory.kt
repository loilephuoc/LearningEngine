package vn.loi.learning.infrastructure.persistence

import java.nio.file.Path
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.domain.study.scheduling.Scheduler
import vn.loi.learning.domain.study.scheduling.ValidatingScheduler
import vn.loi.learning.infrastructure.StudyQueueFactory
import vn.loi.learning.infrastructure.persistence.sqlite.JsonToSqliteMigrationService
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteDatabaseFactory
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteReviewEventRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteStudySessionRepository
import vn.loi.learning.infrastructure.persistence.sqlite.SqliteTransactionRunner

/**
 * Composition root cho LearningEngine sử dụng SQLite persistence.
 */
object PersistedLearningEngineFactory {

    fun create(
        persistenceDirectory: Path,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        scheduler: Scheduler = FsrsScheduler(),
        transactionRunner: TransactionRunner? = null
    ): LearningEngine {
        val dbPath = persistenceDirectory.resolve("learning_engine.db")
        val database = SqliteDatabaseFactory.createFromFile(dbPath)
        JsonToSqliteMigrationService.migrateIfNeeded(persistenceDirectory, database)

        val memoryStateRepository = SqliteMemoryStateRepository(database)
        val reviewEventRepository = SqliteReviewEventRepository(database)
        val studySessionRepository = SqliteStudySessionRepository(database)
        val studyQueueRepository = SqliteStudyQueueRepository(database)

        val studyQueue =
            StudyQueueFactory.create(
                repository = studyQueueRepository
            )

        val validatingScheduler =
            ValidatingScheduler(
                delegate = scheduler
            )

        val effectiveTransactionRunner = transactionRunner ?: SqliteTransactionRunner(database)

        return LearningEngine(
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            memoryStateRepository = memoryStateRepository,
            reviewEventRepository = reviewEventRepository,
            sessionRepository = studySessionRepository,
            studyQueueService = studyQueue,
            transactionRunner = effectiveTransactionRunner,
            scheduler = validatingScheduler
        )
    }
}
