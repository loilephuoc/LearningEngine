package vn.loi.learning.application.reviewhistory

import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Điều kiện truy vấn lịch sử review của một learner.
 *
 * Query này chỉ mô tả nhu cầu truy vấn.
 * Nó không truy cập repository và không thực hiện lọc dữ liệu.
 *
 * Các điều kiện tùy chọn:
 * - learningItemId: chỉ lấy lịch sử của một LearningItem;
 * - period: chỉ lấy event nằm trong khoảng thời gian;
 * - ratings: chỉ lấy event có rating thuộc tập được chọn.
 *
 * Khi một điều kiện là null hoặc rỗng,
 * điều kiện đó không giới hạn kết quả truy vấn.
 */
data class ReviewHistoryQuery(
    val learnerId: LearnerId,
    val learningItemId: LearningItemId? = null,
    val period: StudyPeriod? = null,
    val ratings: Set<ReviewRating> = emptySet()
)