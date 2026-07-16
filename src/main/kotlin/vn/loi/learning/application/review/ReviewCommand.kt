package vn.loi.learning.application.review

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Dữ liệu đầu vào cho một lần review.
 *
 * reviewedAt và reviewEventId được truyền từ bên ngoài để use case
 * có thể test hoàn toàn xác định.
 */
data class ReviewCommand(
    val reviewEventId: ReviewEventId,
    val learnerId: LearnerId,
    val learningItemId: LearningItemId,
    val rating: ReviewRating,
    val reviewedAt: Moment,
    val responseTime: TimeSpan? = null
)