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
fun DashboardActivitySection(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardSectionHeader(
            title = "Activity",
            description =
                "Review activity and rating distribution over the last 30 days"
        )

        DashboardReviewHeatmap(
            days = uiState.reviewHeatmapDays
        )

        DashboardAnalyticsLayout(
            visualization = { visualizationModifier ->
                DashboardRatingDistributionChart(
                    values = uiState.ratingDistribution,
                    modifier = visualizationModifier
                )
            },
            metrics = { metricsModifier ->
                DashboardMetricGrid(
                    metrics =
                        listOf(
                            DashboardMetric(
                                title = "Reviews",
                                value = uiState.totalReviews,
                                supportingText = "Last 30 days",
                                tone = DashboardMetricTone.PRIMARY
                            ),
                            DashboardMetric(
                                title = "Active days",
                                value = uiState.activeDays,
                                supportingText = "Days with reviews",
                                tone = DashboardMetricTone.INFO
                            ),
                            DashboardMetric(
                                title = "Reviews per active day",
                                value =
                                    uiState.averageReviewsPerActiveDay,
                                supportingText = "Average workload",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Accuracy",
                                value = uiState.accuracy,
                                supportingText = "Non-Again ratings",
                                tone = DashboardMetricTone.SUCCESS
                            ),
                            DashboardMetric(
                                title = "Current streak",
                                value = uiState.studyStreak,
                                supportingText = "Consecutive active days",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Last review",
                                value = uiState.lastStudy,
                                supportingText = "Most recent activity",
                                tone = DashboardMetricTone.NEUTRAL
                            )
                        ),
                    modifier = metricsModifier,
                    preferredColumnCount = 3
                )
            }
        )
    }
}

@Composable
private fun DashboardRatingDistributionChart(
    values: List<DashboardChartValue>,
    modifier: Modifier = Modifier
) {
    val total =
        values.sumOf { item ->
            item.value
        }

    DashboardVisualizationCard(
        title = "Rating distribution",
        hasData = total > 0,
        modifier = modifier
    ) {
        if (total == 0) {
            DashboardChartEmptyState(
                title = "No review activity yet",
                description =
                    "Complete a study session to see your rating distribution."
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                values.forEach { item ->
                    RatingDistributionRow(
                        item = item,
                        total = total
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingDistributionRow(
    item: DashboardChartValue,
    total: Int
) {
    val progress =
        item.value.toFloat() /
                total.toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            modifier = Modifier.width(56.dp),
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
                                ratingBarColor(
                                    label = item.label
                                ),
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

@Composable
private fun ratingBarColor(
    label: String
): Color =
    when (label) {
        "Again" ->
            MaterialTheme.colorScheme.error

        "Hard" ->
            MaterialTheme.colorScheme.tertiary

        "Good" ->
            MaterialTheme.colorScheme.primary

        "Easy" ->
            MaterialTheme.colorScheme.secondary

        else ->
            MaterialTheme.colorScheme.primary
    }