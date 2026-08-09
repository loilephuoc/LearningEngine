package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot
import vn.loi.learning.domain.study.session.model.SessionCompletionProvenance
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Kết thúc StudySession và dọn queue hoạt động của session.
 *
 * StudySession đã hoàn tất vẫn được lưu để phục vụ history.
 * StudyQueue được xóa vì chỉ là trạng thái điều hướng đang hoạt động.
 */
class FinishStudySessionUseCase(
    private val sessionRepository: StudySessionRepository,
    private val studyQueueService:
    StudyQueueService? = null
) {

    fun execute(
        sessionId: SessionId,
        finishedAt: Moment,
        completionSnapshot: SessionCompletionSnapshot? = null,
        completionProvenance: SessionCompletionProvenance = SessionCompletionProvenance.ORDINARY_SUCCESS
    ): StudySession {
        val session =
            requireNotNull(
                sessionRepository.findById(
                    sessionId
                )
            ) {
                "Session $sessionId does not exist."
            }

        val finishedSession =
            if (completionProvenance == SessionCompletionProvenance.REPLACED_OR_LEFT) {
                // An intentionally replaced/left session must not carry navigation-only Undo state
                // into a future mode. StudySession.leave preserves committed reviews while clearing it.
                session.leave(finishedAt)
            } else {
                session.finish(
                    finishedAt,
                    completionSnapshot,
                    completionProvenance
                )
            }

        sessionRepository.save(
            finishedSession
        )

        if (finishedSession.undoableReview == null) {
            studyQueueService?.delete(sessionId)
        }

        return finishedSession
    }
}
