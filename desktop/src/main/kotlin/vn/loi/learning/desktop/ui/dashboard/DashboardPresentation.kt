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
            title = "Ready today",
            value = uiState.dueToday,
            supportingFacts = listOf(
                DashboardMetric("Due now", uiState.dueNow, "Ready to review", DashboardMetricTone.WARNING),
                DashboardMetric("Overdue", uiState.overdue, "Past scheduled time", DashboardMetricTone.DANGER)
            ),
            actionGuidance = "Open Learn to begin today's session"
        ),
        keyMetrics = listOf(
            DashboardMetric("Learning items", uiState.totalLearningItems, "Total available", DashboardMetricTone.NEUTRAL),
            DashboardMetric("New items", uiState.newItems, "Not studied yet", DashboardMetricTone.INFO),
            DashboardMetric("Active memories", uiState.activeMemories, "In active memory stages", DashboardMetricTone.SUCCESS)
        )
    )
}
