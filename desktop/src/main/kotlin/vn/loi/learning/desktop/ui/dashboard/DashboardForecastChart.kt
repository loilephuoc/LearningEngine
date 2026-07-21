package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardForecastChart(
    values: List<DashboardChartValue>,
    modifier: Modifier = Modifier
) {
    val maximumValue =
        values.maxOfOrNull { item ->
            item.value
        } ?: 0

    DashboardVisualizationCard(
        title = "Seven-day workload",
        hasData = maximumValue > 0,
        modifier = modifier
    ) {
        if (maximumValue == 0) {
            DashboardChartEmptyState(
                title = "No upcoming workload",
                description =
                    "No reviews are scheduled within the next seven days."
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                values.forEach { item ->
                    ForecastBar(
                        item = item,
                        maximumValue = maximumValue
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastBar(
    item: DashboardChartValue,
    maximumValue: Int
) {
    val progress =
        item.value.toFloat() /
                maximumValue.toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            modifier = Modifier.width(88.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .background(
                        color =
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(7.dp)
                    )
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress)
                        .height(14.dp)
                        .background(
                            color =
                                MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(7.dp)
                        )
            )
        }

        Text(
            text = item.value.toString(),
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}