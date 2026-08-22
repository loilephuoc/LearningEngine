package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.study.StudyQueuePlanningService
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus

class UpdateActiveStudySessionLimitsUseCase(
    private val sessions: StudySessionRepository,
    private val planning: StudyQueuePlanningService,
    private val queues: StudyQueueService,
    private val transactions: TransactionRunner
) {
    fun execute(sessionId: SessionId, newLimit: Int, reviewLimit: Int) = transactions.runInTransaction {
        require(newLimit >= 0 && reviewLimit >= 0 && newLimit + reviewLimit > 0)
        val current = requireNotNull(sessions.findById(sessionId)) { "Study session is unavailable." }
        require(current.status == SessionStatus.ACTIVE) { "Only an active study session can be replanned." }
        require(newLimit >= current.newItemsReviewed) { "New limit cannot be below completed new items." }
        require(reviewLimit >= current.reviewItemsReviewed) { "Review limit cannot be below completed reviews." }
        val updated = current.copy(policy = current.policy.copy(
            newItemLimit = newLimit,
            reviewItemLimit = reviewLimit
        ))
        val plan = planning.plan(updated)
        sessions.save(updated)
        queues.replace(plan)
        updated
    }
}
