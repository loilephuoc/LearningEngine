package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardReviewHeatmap(
    days: List<DashboardHeatmapDay>,
    modifier: Modifier = Modifier
) {
    val totalReviews =
        days.sumOf { day ->
            day.reviewCount
        }

    val maximumCount =
        days.maxOfOrNull { day ->
            day.reviewCount
        } ?: 0

    DashboardVisualizationCard(
        title = "Lịch ôn 12 tuần",
        hasData = totalReviews > 0,
        modifier = modifier,
        compact = true
    ) {
        if (totalReviews == 0) {
            DashboardChartEmptyState(
                title = "No review history yet",
                description =
                    "Your daily activity will appear here after the first review."
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val policy = resolveHeatmapLayoutPolicy(maxWidth)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(policy.gap)
                    ) {
                        HeatmapMonthLabels(days, policy)
                        Row(horizontalArrangement = Arrangement.spacedBy(policy.gap)) {
                            HeatmapWeekdayLabels(policy)
                            HeatmapGrid(days, maximumCount, policy)
                        }
                    }
                    HeatmapLegend(modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@Composable
private fun HeatmapMonthLabels(
    days: List<DashboardHeatmapDay>,
    policy: HeatmapLayoutPolicy
) {
    val weeks =
        days
            .take(HEATMAP_WEEK_COUNT * DAYS_PER_WEEK)
            .chunked(DAYS_PER_WEEK)

    Row(
        modifier = Modifier.padding(start = policy.weekdayLabelWidth + policy.gap),
        horizontalArrangement = Arrangement.spacedBy(policy.gap)
    ) {
        weeks.forEachIndexed { index, week ->
            val firstDate =
                week
                    .firstOrNull()
                    ?.let { day ->
                        LocalDate.ofEpochDay(day.epochDay)
                    }

            val previousFirstDate =
                weeks
                    .getOrNull(index - 1)
                    ?.firstOrNull()
                    ?.let { day ->
                        LocalDate.ofEpochDay(day.epochDay)
                    }

            val showMonth =
                firstDate != null &&
                        (
                                previousFirstDate == null ||
                                        firstDate.month != previousFirstDate.month
                                )

            Box(
                modifier = Modifier.width(policy.cellSize),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text =
                        if (showMonth) {
                            firstDate.month.getDisplayName(
                                TextStyle.SHORT,
                                Locale.US
                            )
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeatmapWeekdayLabels(policy: HeatmapLayoutPolicy) {
    Column(
        modifier = Modifier.width(policy.weekdayLabelWidth),
        verticalArrangement = Arrangement.spacedBy(policy.gap)
    ) {
        WEEKDAY_LABELS.forEach { label ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .size(policy.cellSize),
                contentAlignment = Alignment.CenterStart
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    days: List<DashboardHeatmapDay>,
    maximumCount: Int,
    policy: HeatmapLayoutPolicy
) {
    val weeks =
        days
            .take(HEATMAP_WEEK_COUNT * DAYS_PER_WEEK)
            .chunked(DAYS_PER_WEEK)

    Row(horizontalArrangement = Arrangement.spacedBy(policy.gap)) {
        weeks.forEach { week ->
            Column(
                verticalArrangement = Arrangement.spacedBy(policy.gap)
            ) {
                week.forEach { day ->
                    HeatmapCell(
                        day = day,
                        maximumCount = maximumCount,
                        cellSize = policy.cellSize
                    )
                }

                repeat(DAYS_PER_WEEK - week.size) {
                    Box(
                        modifier =
                            Modifier
                                .size(policy.cellSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    day: DashboardHeatmapDay,
    maximumCount: Int,
    cellSize: Dp
) {
    val fraction =
        if (maximumCount == 0) {
            0f
        } else {
            day.reviewCount.toFloat() /
                    maximumCount.toFloat()
        }

    val accessibility =
        resolveDashboardHeatmapDayAccessibility(
            day = day
        )

    val color =
        when {
            day.isFuture ->
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.30f)

            day.reviewCount == 0 ->
                MaterialTheme.colorScheme.surfaceVariant

            else ->
                MaterialTheme.colorScheme.primary.copy(
                    alpha =
                        0.25f +
                                fraction * 0.75f
                )
        }

    Box(
        modifier =
            Modifier
                .size(cellSize)
                .background(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                )
                .semantics {
                    contentDescription =
                        accessibility.contentDescription
                }
    )
}

@Composable
private fun HeatmapLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ít",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        listOf(
            0.20f,
            0.40f,
            0.60f,
            0.80f,
            1.00f
        ).forEach { alpha ->
            HeatmapLegendCell(
                color =
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = alpha)
            )
        }

        Text(
            text = "Nhiều",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeatmapLegendCell(
    color: Color
) {
    Box(
        modifier =
            Modifier
                .size(12.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(3.dp)
                )
    )
}

private const val HEATMAP_WEEK_COUNT = 12

private const val DAYS_PER_WEEK = 7

internal data class HeatmapLayoutPolicy(
    val cellSize: Dp,
    val gap: Dp,
    val weekdayLabelWidth: Dp
)

internal fun resolveHeatmapLayoutPolicy(availableWidth: Dp): HeatmapLayoutPolicy {
    val gap = 3.dp
    val weekdayLabelWidth = 28.dp
    val usableGridWidth = availableWidth - weekdayLabelWidth - gap
    val fittedCellSize =
        (usableGridWidth - gap * (HEATMAP_WEEK_COUNT - 1)) / HEATMAP_WEEK_COUNT
    return HeatmapLayoutPolicy(
        cellSize = fittedCellSize.coerceIn(12.dp, 18.dp),
        gap = gap,
        weekdayLabelWidth = weekdayLabelWidth
    )
}

private val WEEKDAY_LABELS =
    listOf(
        "Mon",
        "",
        "Wed",
        "",
        "Fri",
        "",
        ""
    )
