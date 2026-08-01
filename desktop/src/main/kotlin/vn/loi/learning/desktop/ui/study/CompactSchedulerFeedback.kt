package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.LEBorder
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LEElevation
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.designsystem.LETypography
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun CompactSchedulerFeedback(
    feedback: StudySchedulerFeedback,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val accessibility = resolveStudySchedulerFeedbackAccessibility(feedback)
    val visualFocus = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.SCHEDULER)
    val surfacePresentation =
        StudySurfacePresentationResolver.resolve(
            StudySurfaceStage.UNDERSTANDING,
            StudySurfaceRole.EXPLANATORY
        )
    LESurface(
        variant = surfacePresentation.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility.conciseSummary
            },
        contentPadding = LETheme.spacing.space0,
        border = surfacePresentation.resolveBorder(LETheme.borders),
        shadowElevation = surfacePresentation.resolveElevation(LETheme.elevation)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LESpacing.md, vertical = LESpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = feedback.rating,
                        style = LETheme.typography.statusText,
                        color = visualFocus.resolveContentColor(LETheme.colors),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Ôn lại sau ${feedback.scheduledInterval}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = visualFocus.resolveContentColor(LETheme.colors)
                    )
                }

                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.semantics {
                        contentDescription = if (isExpanded) "Thu gọn chi tiết" else "Xem chi tiết"
                    }
                ) {
                    Text(if (isExpanded) "Thu gọn" else "Chi tiết")
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(LESpacing.xs))
                HorizontalDivider(color = LETheme.colors.borderSubtle)
                Spacer(modifier = Modifier.height(LESpacing.sm))

                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        MetricItem(label = "Rating", value = feedback.rating, modifier = Modifier.weight(1f))
                        MetricItem(label = "Stage", value = feedback.stageTransition, modifier = Modifier.weight(1f))
                        MetricItem(label = "Interval", value = feedback.scheduledInterval, modifier = Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        MetricItem(label = "Difficulty", value = "${feedback.difficultyBefore} → ${feedback.difficultyAfter}", modifier = Modifier.weight(1f))
                        MetricItem(label = "Stability", value = "${feedback.stabilityBefore} → ${feedback.stabilityAfter}", modifier = Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        MetricItem(label = "Review count", value = feedback.reviewCount.toString(), modifier = Modifier.weight(1f))
                        MetricItem(label = "Lapse count", value = feedback.lapseCount.toString(), modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(LESpacing.xs))
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = LETypography.caption,
            color = LEColors.textMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.SCHEDULER)
                .resolveContentColor(LETheme.colors)
        )
    }
}
