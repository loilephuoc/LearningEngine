package vn.loi.learning.application.session

import vn.loi.learning.domain.study.session.model.SessionId

/**
 * Query use case chỉ đọc tiến độ StudyQueue.
 *
 * Use case không:
 * - tạo queue;
 * - advance queue;
 * - thay đổi session;
 * - chạy selection;
 * - ghi persistence.
 */
class GetStudyQueueProgressUseCase(
    private val studyQueueService:
    StudyQueueService
) {

    /**
     * Trả về null khi session không có persisted queue.
     */
    fun execute(
        sessionId: SessionId
    ): StudyQueueProgress? =
        studyQueueService
            .get(sessionId)
            ?.let(
                StudyQueueProgress::from
            )

    /**
     * Trả về progress hoặc ném lỗi khi persisted queue không tồn tại.
     */
    fun require(
        sessionId: SessionId
    ): StudyQueueProgress =
        StudyQueueProgress.from(
            studyQueueService.require(
                sessionId
            )
        )
}