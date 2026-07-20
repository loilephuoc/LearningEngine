package vn.loi.learning.application.dashboard

import vn.loi.learning.domain.study.analytics.model.StudyStatistics

/**
 * Snapshot thống kê tổng hợp dành cho Dashboard.
 *
 * Snapshot chứa:
 * - thống kê của ngày học hiện tại;
 * - thống kê của tuần hiện tại;
 * - thống kê của tháng hiện tại;
 * - chuỗi daily statistics phục vụ biểu đồ và streak.
 *
 * Mỗi khoảng thời gian tái sử dụng StudyStatistics để:
 * - không lặp lại model analytics;
 * - không lặp lại cách tính rating;
 * - không lặp lại cách tính response time;
 * - giữ Dashboard Query độc lập với persistence.
 *
 * Snapshot này chỉ là dữ liệu kết quả.
 * Nó không truy vấn lịch sử review và không tự tính toán metric.
 */
data class DashboardStatisticsSnapshot(
    val today: StudyStatistics,
    val currentWeek: StudyStatistics,
    val currentMonth: StudyStatistics,
    val dailyStatistics: List<DashboardDailyStatistics> =
        emptyList()
) {

    /**
     * Tổng số review trong ngày học hiện tại.
     */
    val reviewsToday: Int
        get() = today.totalReviews

    /**
     * Tổng số review trong tuần hiện tại.
     */
    val reviewsThisWeek: Int
        get() = currentWeek.totalReviews

    /**
     * Tổng số review trong tháng hiện tại.
     */
    val reviewsThisMonth: Int
        get() = currentMonth.totalReviews

    /**
     * Accuracy hiện tại của Dashboard.
     *
     * Accuracy được hiểu là tỷ lệ review thành công:
     * HARD, GOOD hoặc EASY.
     *
     * Giá trị được lấy từ tháng hiện tại để có phạm vi
     * ổn định hơn dữ liệu của riêng hôm nay.
     *
     * Trả về null nếu tháng hiện tại chưa có review.
     */
    val currentAccuracy: Double?
        get() = currentMonth.successfulReviewProportion

    /**
     * Response time trung bình trong tháng hiện tại.
     *
     * Trả về null nếu chưa có review nào ghi nhận response time.
     */
    val currentAverageResponseTimeMillis: Double?
        get() = currentMonth.averageResponseTimeMillis

    /**
     * Dashboard có ít nhất một review trong tháng hiện tại.
     *
     * Vì today và currentWeek đều thuộc currentMonth,
     * currentMonth là phạm vi rộng nhất của snapshot này.
     */
    val hasReviewActivity: Boolean
        get() = currentMonth.hasReviews

    companion object {

        val EMPTY: DashboardStatisticsSnapshot =
            DashboardStatisticsSnapshot(
                today = StudyStatistics.EMPTY,
                currentWeek = StudyStatistics.EMPTY,
                currentMonth = StudyStatistics.EMPTY,
                dailyStatistics = emptyList()
            )
    }
}