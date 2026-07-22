package vn.loi.learning.application.session

import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Result of reconciling persisted session and queue state at startup.
 */
sealed interface ActiveStudySessionRecovery {

    data object NoActiveSession :
        ActiveStudySessionRecovery

    data class Resumable(
        val session: StudySession,
        val queueProgress: StudyQueueProgress,
        val currentLearningItemId: vn.loi.learning.domain.study.learning.model.LearningItemId? =
            session.currentLearningItemId,
        val answerRevealed: Boolean = session.answerRevealed
    ) : ActiveStudySessionRecovery

    data class ClosedIncompleteSession(
        val session: StudySession,
        val reason: Reason
    ) : ActiveStudySessionRecovery {

        enum class Reason {
            MISSING_QUEUE,
            COMPLETED_QUEUE
        }
    }
}
