package vn.loi.learning.application.port

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent

/**
 * Port lưu trữ và truy vấn lịch sử review.
 *
 * ReviewEvent là dữ liệu append-only:
 * - event đã lưu không được sửa;
 * - event mới chỉ được thêm vào;
 * - kết quả truy vấn được sắp xếp theo thời điểm review tăng dần.
 */
interface ReviewEventRepository {

    /**
     * Thêm một ReviewEvent mới vào lịch sử.
     */
    fun append(
        event: ReviewEvent
    )

    /**
     * Truy vấn toàn bộ lịch sử review của một learner,
     * trên tất cả LearningItem.
     */
    fun findAll(
        learnerId: LearnerId
    ): List<ReviewEvent>

    /**
     * Truy vấn lịch sử review của một learner
     * đối với một LearningItem cụ thể.
     */
    fun findAll(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent>

    /** Removes only the expected latest event. Used by atomic one-step undo. */
    fun removeLatest(event: ReviewEvent) {
        throw UnsupportedOperationException("ReviewEvent removal is not supported by this repository.")
    }

    fun deleteByLearningItemIds(learningItemIds: Set<LearningItemId>) {}
}
