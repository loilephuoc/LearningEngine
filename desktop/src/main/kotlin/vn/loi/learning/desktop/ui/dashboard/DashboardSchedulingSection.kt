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
            title = "Lịch ôn",
            description = "Khối lượng và mức độ khẩn cấp hiện tại"
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
                                title = "Tổng đến hạn",
                                value = uiState.dueToday,
                                supportingText = "Đến hạn hiện tại",
                                tone = DashboardMetricTone.PRIMARY
                            ),
                            DashboardMetric(
                                title = "Có thể ôn ngay",
                                value = uiState.dueNow,
                                supportingText = "Sẵn sàng ôn",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Quá hạn",
                                value = uiState.overdue,
                                supportingText = "Đã qua lịch ôn",
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
