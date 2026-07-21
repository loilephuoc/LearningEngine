package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Reconciles the latest active StudySession with its persisted queue.
 *
 * An active session without a queue cannot be resumed safely. A session
 * whose queue is already completed represents a restart between the final
 * review and normal session finalization. Both cases are closed so they do
 * not permanently mask future resumable sessions.
 */
class RecoverActiveStudySessionUseCase(
    private val sessionRepository:
    StudySessionRepository,
    private val studyQueueService:
    StudyQueueService,
    private val finishStudySessionUseCase:
    FinishStudySessionUseCase
) {

    fun execute(
        learnerId: LearnerId,
        recoveredAt: Moment
    ): ActiveStudySessionRecovery {
        val activeSession =
            sessionRepository
                .findActiveByLearner(
                    learnerId
                )
                ?: return ActiveStudySessionRecovery
                    .NoActiveSession

        val queue =
            studyQueueService.get(
                activeSession.id
            )

        if (queue == null) {
            return closeIncompleteSession(
                session = activeSession,
                recoveredAt = recoveredAt,
                reason =
                    ActiveStudySessionRecovery
                        .ClosedIncompleteSession
                        .Reason
                        .MISSING_QUEUE
            )
        }

        if (queue.isCompleted) {
            return closeIncompleteSession(
                session = activeSession,
                recoveredAt = recoveredAt,
                reason =
                    ActiveStudySessionRecovery
                        .ClosedIncompleteSession
                        .Reason
                        .COMPLETED_QUEUE
            )
        }

        return ActiveStudySessionRecovery.Resumable(
            session = activeSession,
            queueProgress =
                StudyQueueProgress.from(
                    queue
                )
        )
    }

    private fun closeIncompleteSession(
        session: StudySession,
        recoveredAt: Moment,
        reason: ActiveStudySessionRecovery.ClosedIncompleteSession.Reason
    ): ActiveStudySessionRecovery {
        val safeFinishedAt =
            if (recoveredAt >= session.startedAt) {
                recoveredAt
            } else {
                session.startedAt
            }

        val finishedSession =
            finishStudySessionUseCase.execute(
                sessionId = session.id,
                finishedAt = safeFinishedAt
            )

        return ActiveStudySessionRecovery
            .ClosedIncompleteSession(
                session = finishedSession,
                reason = reason
            )
    }
}
