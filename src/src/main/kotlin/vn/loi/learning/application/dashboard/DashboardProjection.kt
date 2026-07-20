package vn.loi.learning.application.dashboard

/**
 * Projection trả về cho Dashboard UI hoặc API.
 *
 * Projection chỉ đóng vai trò gom toàn bộ dữ liệu
 * mà giao diện cần hiển thị.
 *
 * Nó:
 * - immutable;
 * - không truy cập repository;
 * - không tính lại analytics;
 * - không phụ thuộc UI framework.
 *
 * Một số giá trị như currentStreak và ratingTrend
 * là projection thuần được suy ra từ dailyStatistics đã có sẵn.
 */
data class DashboardProjection(
    val statistics: DashboardStatisticsSnapshot
) {

    val reviewsToday: Int
        get() = statistics.reviewsToday

    val reviewsThisWeek: Int
        get() = statistics.reviewsThisWeek

    val reviewsThisMonth: Int
        get() = statistics.reviewsThisMonth

    val currentAccuracy: Double?
        get() = statistics.currentAccuracy

    val currentAverageResponseTimeMillis: Double?
        get() = statistics.currentAverageResponseTimeMillis

    val hasReviewActivity: Boolean
        get() = statistics.hasReviewActivity

    val dailyStatistics: List<DashboardDailyStatistics>
        get() = statistics.dailyStatistics

    /**
     * Số ngày học liên tiếp tính ngược từ hôm nay.
     *
     * dailyStatistics được DashboardQuery bảo đảm:
     * - có thứ tự tăng dần;
     * - liên tiếp;
     * - kết thúc tại ngày hiện tại.
     *
     * Vì vậy streak có thể được tính bằng cách duyệt ngược
     * cho đến ngày đầu tiên không có review.
     *
     * Nếu dailyStatistics rỗng hoặc hôm nay chưa có review,
     * currentStreak bằng 0.
     */
    val currentStreak: Int
        get() =
            dailyStatistics
                .asReversed()
                .takeWhile { daily ->
                    daily.hasReviewActivity
                }
                .size

    /**
     * Chuỗi dữ liệu xu hướng rating theo ngày.
     *
     * Mỗi phần tử được ánh xạ trực tiếp từ một
     * DashboardDailyStatistics tương ứng.
     *
     * Thứ tự của danh sách được giữ nguyên theo dailyStatistics,
     * tức là tăng dần theo thời gian.
     */
    val ratingTrend: List<DashboardRatingTrendPoint>
        get() =
            dailyStatistics.map { daily ->
                DashboardRatingTrendPoint.from(
                    dailyStatistics = daily
                )
            }

    companion object {

        val EMPTY: DashboardProjection =
            DashboardProjection(
                statistics =
                    DashboardStatisticsSnapshot.EMPTY
            )
    }
}