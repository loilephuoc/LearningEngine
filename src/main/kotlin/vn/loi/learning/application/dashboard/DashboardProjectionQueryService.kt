package vn.loi.learning.application.dashboard

/**
 * Application facade trả về projection hoàn chỉnh
 * dành cho Dashboard UI hoặc API.
 *
 * Service này:
 * - nhận DashboardQuery;
 * - lấy DashboardStatisticsSnapshot;
 * - bọc snapshot thành DashboardProjection.
 *
 * Service không:
 * - truy cập repository;
 * - tính lại analytics;
 * - tự tạo period;
 * - tự tính rating trend hoặc current streak.
 *
 * Các giá trị projection như ratingTrend và currentStreak
 * được DashboardProjection suy ra từ snapshot đã có.
 */
class DashboardProjectionQueryService(
    private val dashboardStatisticsQuery:
    DashboardStatisticsQuery
) {

    fun query(
        query: DashboardQuery
    ): DashboardProjection =
        DashboardProjection(
            statistics =
                dashboardStatisticsQuery.query(
                    query = query
                )
        )
}