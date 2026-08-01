package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun DashboardMetricGrid(
    metrics: List<DashboardMetric>,
    modifier: Modifier = Modifier,
    preferredColumnCount: Int? = null
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val maximumColumnCount =
            when {
                maxWidth < 420.dp -> 1
                maxWidth < 520.dp -> 2
                maxWidth < 900.dp -> 3
                else -> 4
            }

        val columnCount =
            preferredColumnCount
                ?.coerceIn(
                    minimumValue = 1,
                    maximumValue = maximumColumnCount
                )
                ?: maximumColumnCount

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space4)
        ) {
            metrics
                .chunked(columnCount)
                .forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(LETheme.spacing.space4)
                    ) {
                        rowMetrics.forEach { metric ->
                            DashboardMetricCard(
                                title = metric.title,
                                value = metric.value,
                                supportingText =
                                    metric.supportingText,
                                tone = metric.tone,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        repeat(columnCount - rowMetrics.size) {
                            Spacer(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
        }
    }
}
