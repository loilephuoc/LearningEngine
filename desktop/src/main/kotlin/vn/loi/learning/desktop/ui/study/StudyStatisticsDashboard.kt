package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LEColors
import vn.loi.learning.desktop.ui.theme.LEIconsTokens
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
internal fun StudyStatisticsDashboard(
    presentation: StudyHeaderStatisticsPresentation,
    layout: StudyStatisticsLayoutPresentation,
    modifier: Modifier = Modifier
) {
    LESurface(
        variant = LESurfaceVariant.STATISTICS,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = presentation.accessibilityDescription
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space3)) {
            presentation.metrics.chunked(layout.metricsPerRow).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowMetrics.forEachIndexed { index, metric ->
                        if (index > 0) {
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                color = LETheme.colors.borderSubtle
                            )
                        }
                        StudyStatisticsMetric(
                            metric = metric,
                            showSubtitle = layout.showSubtitles,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyStatisticsMetric(
    metric: StudyHeaderMetricPresentation,
    showSubtitle: Boolean,
    modifier: Modifier = Modifier
) {
    val semanticColor = metric.type.semanticColor(LETheme.colors)
    val activeColor =
        if (metric.emphasis == StudyMetricEmphasis.MUTED) LETheme.colors.textMuted else semanticColor
    Column(
        modifier = modifier
            .padding(horizontal = LETheme.spacing.space2)
            .clearAndSetSemantics { contentDescription = metric.accessibilityText },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space1)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = metric.type.icon(LETheme.icons),
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(LETheme.spacing.space5)
            )
            Text(
                text = metric.label,
                style = LETheme.typography.metricLabel,
                color = LETheme.colors.textSecondary,
                maxLines = 1
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = metric.primaryValue,
                style = LETheme.typography.metricValue,
                color = activeColor,
                maxLines = 1
            )
            metric.secondaryValue?.let {
                Text(
                    text = it,
                    style = LETheme.typography.metricLabel,
                    color = LETheme.colors.textMuted,
                    maxLines = 1
                )
            }
        }
        if (showSubtitle && metric.subtitle != null) {
            Text(
                text = metric.subtitle,
                style = LETheme.typography.metricSubtitle,
                color = LETheme.colors.textMuted,
                maxLines = 1
            )
        }
    }
}

private fun StudyHeaderMetricType.semanticColor(colors: LEColors): Color = when (this) {
    StudyHeaderMetricType.TOTAL -> colors.metricPurple
    StudyHeaderMetricType.NEW -> colors.metricGreen
    StudyHeaderMetricType.REVIEW -> colors.metricBlue
    StudyHeaderMetricType.DUE -> colors.metricOrange
    StudyHeaderMetricType.AGAIN -> colors.metricRed
    StudyHeaderMetricType.HARD -> colors.metricOrange
    StudyHeaderMetricType.GOOD -> colors.metricGreen
    StudyHeaderMetricType.EASY -> colors.metricBlue
}

private fun StudyHeaderMetricType.icon(icons: LEIconsTokens): ImageVector = when (this) {
    StudyHeaderMetricType.TOTAL -> icons.StatisticsTotal
    StudyHeaderMetricType.NEW -> icons.StatisticsNew
    StudyHeaderMetricType.REVIEW -> icons.StatisticsReview
    StudyHeaderMetricType.DUE -> icons.StatisticsDue
    StudyHeaderMetricType.AGAIN -> icons.StatisticsAgain
    StudyHeaderMetricType.HARD -> icons.StatisticsHard
    StudyHeaderMetricType.GOOD -> icons.StatisticsGood
    StudyHeaderMetricType.EASY -> icons.StatisticsEasy
}
