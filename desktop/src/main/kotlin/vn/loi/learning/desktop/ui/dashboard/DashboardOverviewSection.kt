package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DashboardOverviewSection(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    DashboardMetricGrid(
        metrics =
            listOf(
                DashboardMetric(
                    title = "Learning items",
                    value = uiState.totalLearningItems,
                    supportingText = "Total available",
                    tone = DashboardMetricTone.PRIMARY
                ),
                DashboardMetric(
                    title = "Due today",
                    value = uiState.dueToday,
                    supportingText = "Ready for review",
                    tone = DashboardMetricTone.WARNING
                ),
                DashboardMetric(
                    title = "New items",
                    value = uiState.newItems,
                    supportingText = "Not studied yet",
                    tone = DashboardMetricTone.INFO
                )
            ),
        modifier = modifier
    )
}