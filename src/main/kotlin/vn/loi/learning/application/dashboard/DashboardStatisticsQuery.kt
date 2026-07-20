package vn.loi.learning.application.dashboard

/**
 * Cổng truy vấn statistics snapshot dành cho Dashboard.
 *
 * Abstraction này giúp phần tạo DashboardProjection
 * không phụ thuộc trực tiếp vào implementation cụ thể
 * của DashboardQueryService.
 */
fun interface DashboardStatisticsQuery {

    fun query(
        query: DashboardQuery
    ): DashboardStatisticsSnapshot
}