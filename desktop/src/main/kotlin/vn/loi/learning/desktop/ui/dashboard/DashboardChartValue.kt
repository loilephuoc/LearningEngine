package vn.loi.learning.desktop.ui.dashboard

data class DashboardChartValue(
    val label: String,
    val value: Int
) {

    init {
        require(value >= 0) {
            "Dashboard chart value must not be negative."
        }
    }
}