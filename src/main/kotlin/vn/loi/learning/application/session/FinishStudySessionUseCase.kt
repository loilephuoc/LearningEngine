package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot
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
        completionSnapshot: SessionCompletionSnapshot? = null
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
            session.finish(
                finishedAt,
                completionSnapshot
            )

        sessionRepository.save(
            finishedSession
        )

        if (finishedSession.undoableReview == null) {
            studyQueueService?.delete(sessionId)
        }

        return finishedSession
    }
}
