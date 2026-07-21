package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardSchedulingSection(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardSectionHeader(
            title = "Scheduling",
            description =
                "Current review workload and scheduling urgency"
        )

        DashboardAnalyticsLayout(
            visualization = { visualizationModifier ->
                DashboardSchedulingPressureChart(
                    values = uiState.schedulingPressure,
                    modifier = visualizationModifier
                )
            },
            metrics = { metricsModifier ->
                DashboardMetricGrid(
                    metrics =
                        listOf(
                            DashboardMetric(
                                title = "Due total",
                                value = uiState.dueToday,
                                supportingText = "Due at query time",
                                tone = DashboardMetricTone.PRIMARY
                            ),
                            DashboardMetric(
                                title = "Due now",
                                value = uiState.dueNow,
                                supportingText = "Ready to review",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Overdue",
                                value = uiState.overdue,
                                supportingText = "Past scheduled time",
                                tone = DashboardMetricTone.DANGER
                            )
                        ),
                    modifier = metricsModifier,
                    preferredColumnCount = 3
                )
            }
        )
    }
}