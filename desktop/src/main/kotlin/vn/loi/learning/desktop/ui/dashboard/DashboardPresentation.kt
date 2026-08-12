package vn.loi.learning.desktop.ui.dashboard

enum class DashboardInformationRole {
    TODAY,
    PRIMARY_ACTION_CONTEXT,
    KEY_METRICS,
    RECENT_ACTIVITY,
    HISTORICAL_ANALYTICS
}

data class DashboardTodayPresentation(
    val title: String,
    val value: String,
    val supportingFacts: List<DashboardMetric>,
    val actionGuidance: String
)

data class DashboardPresentation(
    val readingOrder: List<DashboardInformationRole>,
    val today: DashboardTodayPresentation,
    val keyMetrics: List<DashboardMetric>
)

object DashboardPresentationResolver {
    private val order = listOf(
        DashboardInformationRole.TODAY,
        DashboardInformationRole.PRIMARY_ACTION_CONTEXT,
        DashboardInformationRole.KEY_METRICS,
        DashboardInformationRole.RECENT_ACTIVITY,
        DashboardInformationRole.HISTORICAL_ANALYTICS
    )

    fun resolve(uiState: DashboardUiState): DashboardPresentation = DashboardPresentation(
        readingOrder = order,
        today = DashboardTodayPresentation(
            title = "Cần ôn hôm nay",
            value = uiState.dueToday,
            supportingFacts = listOf(
                DashboardMetric("Đến hạn", uiState.dueNow, "Có thể ôn ngay", DashboardMetricTone.WARNING),
                DashboardMetric("Quá hạn", uiState.overdue, "Đã qua lịch ôn", DashboardMetricTone.DANGER)
            ),
            actionGuidance = "Bắt đầu từ màn Học"
        ),
        keyMetrics = listOf(
            DashboardMetric("Đã học", uiState.activeMemories, "Bộ nhớ đang hoạt động", DashboardMetricTone.SUCCESS),
            DashboardMetric("Khả năng ghi nhớ", uiState.retention, "Ước lượng hiện tại", DashboardMetricTone.PRIMARY),
            DashboardMetric("Độ chính xác 30 ngày", uiState.accuracy, "Các lượt không phải Again", DashboardMetricTone.INFO),
            DashboardMetric("Lượt ôn 30 ngày", uiState.totalReviews, "Hoạt động gần đây", DashboardMetricTone.WARNING)
        )
    )
}
