package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardMemoryDistributionChart(
    values: List<DashboardChartValue>,
    modifier: Modifier = Modifier
) {
    val total =
        values.sumOf { item ->
            item.value
        }

    DashboardVisualizationCard(
        title = "Stage distribution",
        hasData = total > 0,
        modifier = modifier
    ) {
        if (total == 0) {
            DashboardChartEmptyState(
                title = "No memory data yet",
                description =
                    "Import learning content and begin studying to build memory statistics."
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                MemoryDistributionBar(
                    values = values
                )

                MemoryDistributionLegend(
                    values = values,
                    total = total
                )
            }
        }
    }
}

@Composable
private fun MemoryDistributionBar(
    values: List<DashboardChartValue>
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        values
            .filter { item ->
                item.value > 0
            }
            .forEach { item ->
                Box(
                    modifier =
                        Modifier
                            .weight(item.value.toFloat())
                            .height(24.dp)
                            .background(
                                color =
                                    memoryStageColor(
                                        label = item.label
                                    ),
                                shape = RoundedCornerShape(4.dp)
                            )
                )
            }
    }
}

@Composable
private fun MemoryDistributionLegend(
    values: List<DashboardChartValue>,
    total: Int
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val columnCount =
            when {
                maxWidth < 460.dp -> 1
                maxWidth < 760.dp -> 2
                else -> 3
            }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            values
                .chunked(columnCount)
                .forEach { rowValues ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(16.dp)
                    ) {
                        rowValues.forEach { item ->
                            MemoryDistributionLegendItem(
                                item = item,
                                total = total,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        repeat(columnCount - rowValues.size) {
                            Box(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun MemoryDistributionLegendItem(
    item: DashboardChartValue,
    total: Int,
    modifier: Modifier = Modifier
) {
    val percentage =
        (
                (
                        item.value.toDouble() /
                                total.toDouble()
                        ) * 100.0
                ).toInt()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(
                        color =
                            memoryStageColor(
                                label = item.label
                            ),
                        shape = RoundedCornerShape(3.dp)
                    )
        )

        Column {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${item.value} · $percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun memoryStageColor(
    label: String
): Color =
    when (label) {
        "New" ->
            MaterialTheme.colorScheme.tertiary

        "Learning" ->
            MaterialTheme.colorScheme.secondary

        "Review" ->
            MaterialTheme.colorScheme.primary

        "Relearning" ->
            MaterialTheme.colorScheme.error

        "Mastered" ->
            MaterialTheme.colorScheme.primaryContainer

        "Suspended" ->
            MaterialTheme.colorScheme.outline

        else ->
            MaterialTheme.colorScheme.primary
    }