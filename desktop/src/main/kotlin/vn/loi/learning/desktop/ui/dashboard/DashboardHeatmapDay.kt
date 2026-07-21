package vn.loi.learning.desktop.ui.dashboard

data class DashboardHeatmapDay(
    val epochDay: Long,
    val reviewCount: Int,
    val isFuture: Boolean
) {

    init {
        require(reviewCount >= 0) {
            "Dashboard heatmap review count must not be negative."
        }
    }
}