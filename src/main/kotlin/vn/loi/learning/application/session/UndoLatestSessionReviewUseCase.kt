package vn.loi.learning.application.session

import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.domain.study.session.model.SessionId

/** Atomically reverses exactly the latest committed review of one session. */
class UndoLatestSessionReviewUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val memoryStates: MemoryStateRepository,
    private val reviewEvents: ReviewEventRepository,
    private val transactions: TransactionRunner,
    private val trajectories: LearningTrajectoryRepository? = null
) {
    fun execute(sessionId: SessionId): UndoLatestSessionReviewResult {
        val session = requireNotNull(sessions.findById(sessionId)) { "Session $sessionId does not exist." }
        val undo = session.undoableReview ?: return UndoLatestSessionReviewResult.NothingToUndo
        val event = reviewEvents.findAll(session.learnerId, undo.learningItemId)
            .lastOrNull { it.id == undo.reviewEventId }
            ?: error("Undoable ReviewEvent ${undo.reviewEventId} does not exist.")
        require(event.stateBefore == undo.memoryStateBefore) { "Undo checkpoint does not match ReviewEvent before-state." }
        val currentMemory = memoryStates.find(session.learnerId, undo.learningItemId)
        require(currentMemory == event.stateAfter) { "MemoryState has changed since the review and cannot be undone." }

        return transactions.runInTransaction {
            reviewEvents.removeLatest(event)
            if (undo.memoryStateExistedBefore) {
                memoryStates.save(undo.memoryStateBefore)
            } else {
                memoryStates.delete(session.learnerId, undo.learningItemId)
            }
            if (undo.trajectoryChanged) {
                val before = undo.learningTrajectoryBefore
                if (before == null) trajectories?.delete(session.learnerId, undo.contentId)
                else trajectories?.save(session.learnerId, before)
            }
            val queue = if (undo.advancesSessionProgress) {
                queues.rewind(sessionId, undo.learningItemId)
            } else {
                queues.require(sessionId)
            }
            val restored = session.undoLatestReview()
            sessions.save(restored)
            UndoLatestSessionReviewResult.Undone(
                restored,
                LearningSessionProgress.from(restored, StudyQueueProgress.from(queue))
            )
        }
    }
}
