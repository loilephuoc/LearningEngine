package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Releases one active practice source before another source is selected.
 *
 * Review transactions already committed remain durable. The active queue and its Undo checkpoint
 * are session-local navigation state and cannot be carried into the replacement source.
 */
class LeaveActiveStudySessionUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val transactions: TransactionRunner
) {
    fun execute(learnerId: LearnerId, leftAt: Moment): StudySession? =
        transactions.runInTransaction {
            val active = sessions.findActiveByLearner(learnerId) ?: return@runInTransaction null
            val effectiveLeftAt = if (leftAt >= active.startedAt) leftAt else active.startedAt
            val left = active.leave(effectiveLeftAt)
            sessions.save(left)
            queues.delete(active.id)
            left
        }
}
