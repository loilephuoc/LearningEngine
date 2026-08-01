package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DashboardOverviewSection(
    metrics: List<DashboardMetric>,
    modifier: Modifier = Modifier
) {
    DashboardMetricGrid(
        metrics = metrics,
        modifier = modifier
    )
}
