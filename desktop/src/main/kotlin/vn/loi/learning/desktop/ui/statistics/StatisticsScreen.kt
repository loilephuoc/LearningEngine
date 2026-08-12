package vn.loi.learning.desktop.ui.statistics

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.dashboard.*
import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.DesktopLoadStateCard
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onRetry: () -> Unit,
    onScopeChanged: (vn.loi.learning.domain.library.model.InstalledPackageId?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            DesktopLoadStateCard(uiState.loadState, "Thống kê", onRetry)
            if (uiState.loadState != DesktopLoadState.Loading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Thống kê", style = LETheme.typography.headlinePane, fontWeight = FontWeight.Bold)
                    Text("Hiệu suất học tập và sức khỏe bộ nhớ", color = LETheme.colors.textSecondary)
                    Text("Phạm vi thống kê", style = LETheme.typography.secondaryMetadata, color = LETheme.colors.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.scopeOptions.forEach { option ->
                            FilterChip(
                                selected = option.packageId == uiState.selectedPackageId,
                                onClick = { onScopeChanged(option.packageId) },
                                label = { Text(option.label, maxLines = 1) },
                                enabled = uiState.loadState != DesktopLoadState.Loading
                            )
                        }
                    }
                }
                SummaryCards(uiState)
                RatingDistribution(uiState)
                DashboardMemorySection(uiState.analytics)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth >= 900.dp) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(Modifier.weight(1f)) { DashboardRetentionSection(uiState.analytics) }
                            Box(Modifier.weight(1f)) { DashboardSchedulingSection(uiState.analytics) }
                        }
                    } else {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            DashboardRetentionSection(uiState.analytics)
                            DashboardSchedulingSection(uiState.analytics)
                        }
                    }
                }
                DashboardForecastSection(uiState.analytics)
                DashboardSectionHeader("Hoạt động", "Lịch ôn trong 12 tuần gần nhất")
                DashboardReviewHeatmap(uiState.analytics.reviewHeatmapDays)
                DashboardMetricGrid(listOf(
                    DashboardMetric("Lượt ôn", uiState.analytics.totalReviews, "30 ngày", DashboardMetricTone.PRIMARY),
                    DashboardMetric("Ngày hoạt động (30 ngày)", uiState.analytics.activeDays, "Ngày có lượt ôn", DashboardMetricTone.INFO),
                    DashboardMetric("Lượt ôn / ngày", uiState.analytics.averageReviewsPerActiveDay, "Ngày hoạt động", DashboardMetricTone.NEUTRAL),
                    DashboardMetric("Chuỗi hiện tại", uiState.analytics.studyStreak, "Ngày liên tiếp", DashboardMetricTone.WARNING),
                    DashboardMetric("Lần ôn gần nhất", uiState.analytics.lastStudy, "Ngày gần nhất", DashboardMetricTone.NEUTRAL)
                ), preferredColumnCount = 5)
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scroll),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(12.dp),
            style = ScrollbarStyle(48.dp, 8.dp, RoundedCornerShape(4.dp), 250, MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f), MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable private fun SummaryCards(state: StatisticsUiState) {
    DashboardMetricGrid(listOf(
        DashboardMetric("Lượt ôn 30 ngày", state.totalReviews, "Tổng hoạt động", DashboardMetricTone.PRIMARY),
        DashboardMetric("Độ chính xác", state.successRate, "Không phải Again", DashboardMetricTone.SUCCESS),
        DashboardMetric("Khả năng ghi nhớ", state.analytics.retention, "Hiện tại", DashboardMetricTone.INFO),
        DashboardMetric("Phản hồi trung bình", state.averageResponseTime, "Lượt có thời gian", DashboardMetricTone.NEUTRAL)
    ), preferredColumnCount = 4)
}

@Composable private fun RatingDistribution(state: StatisticsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionHeader("Phân bố đánh giá", "Again / Hard / Good / Easy trong 30 ngày")
        DashboardMetricGrid(listOf(
            DashboardMetric("Again", state.againCount, "Không nhớ", DashboardMetricTone.DANGER),
            DashboardMetric("Hard", state.hardCount, "Khó", DashboardMetricTone.WARNING),
            DashboardMetric("Good", state.goodCount, "Tốt", DashboardMetricTone.SUCCESS),
            DashboardMetric("Easy", state.easyCount, "Dễ", DashboardMetricTone.INFO)
        ), preferredColumnCount = 4)
    }
}
