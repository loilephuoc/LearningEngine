package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardRetentionGauge(
    retentionLabel: String,
    retentionValue: Float?,
    modifier: Modifier = Modifier
) {
    DashboardVisualizationCard(
        title = "Current retrievability",
        hasData = retentionValue != null,
        modifier = modifier
    ) {
        if (retentionValue == null) {
            DashboardChartEmptyState(
                title = "No retention data yet",
                description =
                    "Complete reviews to generate retrievability estimates."
            )
        } else {
            RetentionGaugeContent(
                retentionLabel = retentionLabel,
                retentionValue = retentionValue
            )
        }
    }
}

@Composable
private fun RetentionGaugeContent(
    retentionLabel: String,
    retentionValue: Float
) {
    val progress =
        retentionValue.coerceIn(
            minimumValue = 0f,
            maximumValue = 1f
        )

    val trackColor =
        MaterialTheme.colorScheme.surfaceVariant

    val progressColor =
        MaterialTheme.colorScheme.secondary

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(132.dp)
        ) {
            val strokeWidth = 18.dp.toPx()
            val diameter =
                size.minDimension - strokeWidth

            val topLeft =
                Offset(
                    x =
                        (size.width - diameter) /
                                2f,
                    y =
                        (size.height - diameter) /
                                2f
                )

            val arcSize =
                Size(
                    width = diameter,
                    height = diameter
                )

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style =
                    Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
            )

            drawArc(
                color = progressColor,
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style =
                    Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = retentionLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Average retention",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}