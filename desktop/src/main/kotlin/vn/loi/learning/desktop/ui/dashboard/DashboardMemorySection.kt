package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardMemorySection(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardSectionHeader(
            title = "Memory",
            description =
                "Distribution of items across memory stages"
        )

        DashboardAnalyticsLayout(
            visualization = { visualizationModifier ->
                DashboardMemoryDistributionChart(
                    values = uiState.memoryStageDistribution,
                    modifier = visualizationModifier
                )
            },
            metrics = { metricsModifier ->
                DashboardMetricGrid(
                    metrics =
                        listOf(
                            DashboardMetric(
                                title = "New",
                                value = uiState.newItems,
                                supportingText = "Never studied",
                                tone = DashboardMetricTone.INFO
                            ),
                            DashboardMetric(
                                title = "Learning",
                                value = uiState.learningItems,
                                supportingText = "In learning",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Review",
                                value = uiState.reviewItems,
                                supportingText = "Regular review",
                                tone = DashboardMetricTone.PRIMARY
                            ),
                            DashboardMetric(
                                title = "Relearning",
                                value = uiState.relearningItems,
                                supportingText = "Forgotten items",
                                tone = DashboardMetricTone.DANGER
                            ),
                            DashboardMetric(
                                title = "Mastered",
                                value = uiState.masteredItems,
                                supportingText = "Long-term memory",
                                tone = DashboardMetricTone.SUCCESS
                            ),
                            DashboardMetric(
                                title = "Suspended",
                                value = uiState.suspendedItems,
                                supportingText = "Excluded",
                                tone = DashboardMetricTone.NEUTRAL
                            )
                        ),
                    modifier = metricsModifier,
                    preferredColumnCount = 3
                )
            }
        )
    }
}