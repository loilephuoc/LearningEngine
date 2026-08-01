package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.session.model.SessionCompletionProvenance

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
    FinishStudySessionUseCase,
    private val installedPackageRepository:
    vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null
) {

    fun execute(
        learnerId: LearnerId,
        recoveredAt: Moment,
        topicId: TopicId? = null
    ): ActiveStudySessionRecovery {
        val activeSession =
            if (topicId == null) {
                sessionRepository
                    .findActiveByLearner(
                        learnerId
                    )
            } else {
                sessionRepository
                    .findActiveByLearnerAndTopic(
                        learnerId = learnerId,
                        topicId = topicId
                    )
            }
                ?: return ActiveStudySessionRecovery
                    .NoActiveSession

        val installedPackageId = activeSession.installedPackageId
        if (installedPackageId != null && installedPackageRepository != null) {
            val installedPkg = installedPackageRepository.findById(installedPackageId)
            if (installedPkg != null && (installedPkg.state == PackageState.REMOVED || installedPkg.state == PackageState.ARCHIVED)) {
                return ActiveStudySessionRecovery.NoActiveSession
            }
        }

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
                        .MISSING_QUEUE,
                queueProgress = null
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
                        .COMPLETED_QUEUE,
                queueProgress = StudyQueueProgress.from(queue)
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
        reason: ActiveStudySessionRecovery.ClosedIncompleteSession.Reason,
        queueProgress: StudyQueueProgress?
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
                finishedAt = safeFinishedAt,
                completionProvenance = SessionCompletionProvenance.RECOVERY_RECONCILIATION
            )

        return ActiveStudySessionRecovery
            .ClosedIncompleteSession(
                session = finishedSession,
                reason = reason,
                queueProgress = queueProgress
            )
    }
}
