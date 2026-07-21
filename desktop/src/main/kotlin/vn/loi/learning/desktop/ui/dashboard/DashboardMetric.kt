package vn.loi.learning.desktop.ui.dashboard

data class DashboardMetric(
    val title: String,
    val value: String,
    val supportingText: String,
    val tone: DashboardMetricTone = DashboardMetricTone.NEUTRAL
)

enum class DashboardMetricTone {
    NEUTRAL,
    PRIMARY,
    WARNING,
    DANGER,
    SUCCESS,
    INFO
}