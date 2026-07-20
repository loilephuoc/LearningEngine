package vn.loi.learning.infrastructure.persistence

import java.nio.file.Path
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.domain.study.scheduling.Scheduler
import vn.loi.learning.infrastructure.persistence.json.JsonMemoryStateStore
import vn.loi.learning.infrastructure.persistence.json.JsonReviewEventStore
import vn.loi.learning.infrastructure.persistence.json.JsonStudySessionStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedReviewEventRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedStudySessionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

/**
 * Composition root cho LearningEngine sử dụng JSON persistence.
 *
 * Factory chỉ chịu trách nhiệm lắp ráp dependency.
 * Không chứa business logic.
 *
 * ContentRepository và LearningItemRepository được truyền từ bên ngoài
 * vì nguồn Content/LearningItem có thể đến từ importer, package,
 * database hoặc cloud adapter khác.
 */
object PersistedLearningEngineFactory {

    fun create(
        persistenceDirectory: Path,
        contentRepository: ContentRepository,
        learningItemRepository: LearningItemRepository,
        scheduler: Scheduler = FsrsScheduler(),
        transactionRunner: TransactionRunner =
            InMemoryTransactionRunner()
    ): LearningEngine {
        val memoryStateStore =
            JsonMemoryStateStore(
                filePath =
                    persistenceDirectory.resolve(
                        MEMORY_STATES_FILE_NAME
                    )
            )

        val reviewEventStore =
            JsonReviewEventStore(
                filePath =
                    persistenceDirectory.resolve(
                        REVIEW_EVENTS_FILE_NAME
                    )
            )

        val studySessionStore =
            JsonStudySessionStore(
                filePath =
                    persistenceDirectory.resolve(
                        STUDY_SESSIONS_FILE_NAME
                    )
            )

        val memoryStateRepository =
            StoreBackedMemoryStateRepository(
                store = memoryStateStore
            )

        val reviewEventRepository =
            StoreBackedReviewEventRepository(
                store = reviewEventStore
            )

        val studySessionRepository =
            StoreBackedStudySessionRepository(
                store = studySessionStore
            )

        return LearningEngine(
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
                scheduler
        )
    }

    private const val MEMORY_STATES_FILE_NAME =
        "memory-states.json"

    private const val REVIEW_EVENTS_FILE_NAME =
        "review-events.json"

    private const val STUDY_SESSIONS_FILE_NAME =
        "study-sessions.json"
}