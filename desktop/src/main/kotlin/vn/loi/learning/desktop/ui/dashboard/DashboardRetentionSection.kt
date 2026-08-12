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
            title = "Khả năng ghi nhớ",
            description = "Sức khỏe bộ nhớ và hiệu suất gần đây"
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
                                title = "Ghi nhớ trung bình",
                                value = uiState.retention,
                                supportingText =
                                    "Khả năng nhớ ước tính",
                                tone = DashboardMetricTone.SUCCESS
                            ),
                            DashboardMetric(
                                title = "Bộ nhớ được đánh giá",
                                value = uiState.retentionEvaluated,
                                supportingText =
                                    "Được tính vào retention",
                                tone = DashboardMetricTone.INFO
                            ),
                            DashboardMetric(
                                title = "Độ chính xác",
                                value = uiState.accuracy,
                                supportingText = "30 ngày gần nhất",
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
