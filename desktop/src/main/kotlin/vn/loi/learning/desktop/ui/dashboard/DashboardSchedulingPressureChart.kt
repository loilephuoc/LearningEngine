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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardSchedulingPressureChart(
    values: List<DashboardChartValue>,
    modifier: Modifier = Modifier
) {
    val maximumValue =
        values.maxOfOrNull { item ->
            item.value
        } ?: 0

    DashboardVisualizationCard(
        title = "Scheduling pressure",
        hasData = maximumValue > 0,
        modifier = modifier
    ) {
        if (maximumValue == 0) {
            DashboardChartEmptyState(
                title = "Nothing is due",
                description =
                    "Your current review queue is clear."
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                values.forEach { item ->
                    SchedulingPressureRow(
                        item = item,
                        maximumValue = maximumValue
                    )
                }
            }
        }
    }
}

@Composable
private fun SchedulingPressureRow(
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
            modifier = Modifier.width(76.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(
                        color =
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress)
                        .height(16.dp)
                        .background(
                            color =
                                schedulingPressureColor(
                                    label = item.label
                                ),
                            shape = RoundedCornerShape(8.dp)
                        )
            )
        }

        Text(
            text = item.value.toString(),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun schedulingPressureColor(
    label: String
): Color =
    when (label) {
        "Due total" ->
            MaterialTheme.colorScheme.primary

        "Due now" ->
            MaterialTheme.colorScheme.tertiary

        "Overdue" ->
            MaterialTheme.colorScheme.error

        else ->
            MaterialTheme.colorScheme.primary
    }