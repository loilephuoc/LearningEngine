package vn.loi.learning.application.dashboard

import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Thống kê Dashboard của một ngày học đã được xác định.
 *
 * Application Layer không tự chuyển Moment thành ngày lịch vì:
 * - ngày phụ thuộc múi giờ;
 * - ranh giới ngày có thể phụ thuộc cấu hình người học;
 * - Domain hiện không chứa timezone.
 *
 * Vì vậy, period phải được xác định từ bên ngoài và được truyền
 * vào dưới dạng khoảng [startInclusive, endExclusive).
 *
 * Model này chỉ ghép StudyPeriod với StudyStatistics.
 * Nó không truy vấn lịch sử review và không tự tính toán metric.
 */
data class DashboardDailyStatistics(
    val period: StudyPeriod,
    val statistics: StudyStatistics
) {

    /**
     * Thời điểm bắt đầu ngày học.
     *
     * Thuộc tính này hỗ trợ UI và các projection sắp xếp dữ liệu
     * mà không cần truy cập sâu vào period.
     */
    val dayStart: Moment
        get() = period.startInclusive

    /**
     * Tổng số review trong ngày.
     */
    val totalReviews: Int
        get() = statistics.totalReviews

    /**
     * Tỷ lệ review thành công trong ngày.
     *
     * Trả về null nếu ngày chưa có review.
     */
    val accuracy: Double?
        get() = statistics.successfulReviewProportion

    /**
     * Response time trung bình trong ngày.
     *
     * Trả về null nếu ngày chưa có response-time data.
     */
    val averageResponseTimeMillis: Double?
        get() = statistics.averageResponseTimeMillis

    /**
     * Ngày có ít nhất một review.
     */
    val hasReviewActivity: Boolean
        get() = statistics.hasReviews
}