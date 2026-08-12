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
            title = "Bộ nhớ",
            description = "Phân bố item theo trạng thái bộ nhớ"
        )

        DashboardMemoryDistributionChart(
            values = uiState.memoryStageDistribution,
            modifier = Modifier.fillMaxWidth()
        )

        DashboardMetricGrid(
                    metrics =
                        listOf(
                            DashboardMetric(
                                title = "Mới",
                                value = uiState.newItems,
                                supportingText = "Chưa học",
                                tone = DashboardMetricTone.INFO
                            ),
                            DashboardMetric(
                                title = "Đang học",
                                value = uiState.learningItems,
                                supportingText = "Đang học",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Ôn tập",
                                value = uiState.reviewItems,
                                supportingText = "Ôn định kỳ",
                                tone = DashboardMetricTone.PRIMARY
                            ),
                            DashboardMetric(
                                title = "Học lại",
                                value = uiState.relearningItems,
                                supportingText = "Đang học lại",
                                tone = DashboardMetricTone.DANGER
                            ),
                            DashboardMetric(
                                title = "Thành thạo",
                                value = uiState.masteredItems,
                                supportingText = "Bộ nhớ dài hạn",
                                tone = DashboardMetricTone.SUCCESS
                            ),
                            DashboardMetric(
                                title = "Tạm dừng",
                                value = uiState.suspendedItems,
                                supportingText = "Tạm dừng",
                                tone = DashboardMetricTone.NEUTRAL
                            )
                        ),
            modifier = Modifier.fillMaxWidth(),
            preferredColumnCount = 3
        )
    }
}
