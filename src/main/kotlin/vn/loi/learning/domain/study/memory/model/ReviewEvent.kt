package vn.loi.learning.domain.study.memory.model

/**
 * Bản ghi bất biến của một lần review.
 *
 * ReviewEvent là append-only:
 * - không sửa nội dung event cũ;
 * - không tái sử dụng ID;
 * - sửa sai bằng cách thêm event mới hoặc cơ chế correction sau này.
 *
 * Việc lưu stateBefore và stateAfter giúp:
 * - audit;
 * - debug Scheduler;
 * - replay lịch sử;
 * - so sánh thuật toán;
 * - huấn luyện AI sau này.
 */
data class ReviewEvent(
    val id: ReviewEventId,
    val rating: ReviewRating,
    val reviewedAt: Moment,
    val responseTime: TimeSpan?,
    val stateBefore: MemoryState,
    val stateAfter: MemoryState
) {

    init {
        require(stateBefore.learnerId == stateAfter.learnerId) {
            "ReviewEvent states must belong to the same learner."
        }

        require(stateBefore.learningItemId == stateAfter.learningItemId) {
            "ReviewEvent states must belong to the same LearningItem."
        }

        require(stateAfter.reviewCount == stateBefore.reviewCount + 1) {
            "A review must increase reviewCount by exactly one."
        }

        require(stateAfter.lastReviewedAt == reviewedAt) {
            "The resulting MemoryState lastReviewedAt must equal reviewedAt."
        }

        require(reviewedAt >= stateBefore.dueAt || stateBefore.stage != LearningStage.NEW) {
            /*
             * Review sớm vẫn có thể được hỗ trợ sau này.
             * Điều kiện hiện tại chỉ tránh một NEW item có thời gian review
             * nằm trước thời điểm item được cung cấp.
             */
            "A new item cannot be reviewed before it becomes available."
        }
    }

    val learnerId: LearnerId
        get() = stateAfter.learnerId

    val learningItemId
        get() = stateAfter.learningItemId
}