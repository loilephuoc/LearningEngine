package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardRetentionSection(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardSectionHeader(
            title = "Retention",
            description =
                "Current memory strength and recent performance"
        )

        DashboardAnalyticsLayout(
            visualization = { visualizationModifier ->
                DashboardRetentionGauge(
                    retentionLabel = uiState.retention,
                    retentionValue = uiState.retentionValue,
                    modifier = visualizationModifier
                )
            },
            metrics = { metricsModifier ->
                DashboardMetricGrid(
                    metrics =
                        listOf(
                            DashboardMetric(
                                title = "Average retention",
                                value = uiState.retention,
                                supportingText =
                                    "Estimated retrievability",
                                tone = DashboardMetricTone.SUCCESS
                            ),
                            DashboardMetric(
                                title = "Evaluated memories",
                                value = uiState.retentionEvaluated,
                                supportingText =
                                    "Included in retention",
                                tone = DashboardMetricTone.INFO
                            ),
                            DashboardMetric(
                                title = "Accuracy",
                                value = uiState.accuracy,
                                supportingText = "Last 30 days",
                                tone = DashboardMetricTone.PRIMARY
                            )
                        ),
                    modifier = metricsModifier,
                    preferredColumnCount = 3
                )
            }
        )
    }
}