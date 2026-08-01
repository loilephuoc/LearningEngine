package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import vn.loi.learning.desktop.ui.theme.LETheme
import vn.loi.learning.desktop.ui.theme.LEColors as LEThemeColors
import vn.loi.learning.domain.study.memory.model.ReviewRating

enum class SchedulerFeedbackContext { ACTIVE_ANSWER, COMPLETION, CONTINUITY }

internal enum class SchedulerFeedbackEmphasis { EXPLANATORY, CONSEQUENCE }

internal enum class SchedulerFeedbackPlacement { INLINE_DECISION, CONTAINED, OVERLAY }

@Immutable
internal data class SchedulerFeedbackPresentation(
    val context: SchedulerFeedbackContext,
    val emphasis: SchedulerFeedbackEmphasis,
    val placement: SchedulerFeedbackPlacement,
    val contentTone: StudyContentTone,
    val preservesSemanticRatingIdentity: Boolean,
    val detailsAvailable: Boolean,
    val allowsCompactWrap: Boolean,
    val maximumContentWidthDp: Int?
)

internal object SchedulerFeedbackPresentationResolver {
    fun resolve(context: SchedulerFeedbackContext): SchedulerFeedbackPresentation =
        when (context) {
            SchedulerFeedbackContext.ACTIVE_ANSWER ->
                SchedulerFeedbackPresentation(
                    context = context,
                    emphasis = SchedulerFeedbackEmphasis.EXPLANATORY,
                    placement = SchedulerFeedbackPlacement.INLINE_DECISION,
                    contentTone = StudyContentTone.SECONDARY,
                    preservesSemanticRatingIdentity = true,
                    detailsAvailable = true,
                    allowsCompactWrap = true,
                    maximumContentWidthDp = 640
                )
            SchedulerFeedbackContext.COMPLETION ->
                consequence(context, SchedulerFeedbackPlacement.CONTAINED)
            SchedulerFeedbackContext.CONTINUITY ->
                consequence(context, SchedulerFeedbackPlacement.OVERLAY)
        }

    private fun consequence(
        context: SchedulerFeedbackContext,
        placement: SchedulerFeedbackPlacement
    ) = SchedulerFeedbackPresentation(
        context = context,
        emphasis = SchedulerFeedbackEmphasis.CONSEQUENCE,
        placement = placement,
        contentTone = StudyContentTone.SECONDARY,
        preservesSemanticRatingIdentity = false,
        detailsAvailable = true,
        allowsCompactWrap = false,
        maximumContentWidthDp = null
    )
}

internal fun resolveSchedulerRatingIdentityColor(
    rating: ReviewRating,
    colors: LEThemeColors
) = when (rating) {
    ReviewRating.AGAIN -> colors.dangerText
    ReviewRating.HARD -> colors.warningText
    ReviewRating.GOOD -> colors.successText
    ReviewRating.EASY -> colors.info
}

@Composable
fun CompactSchedulerFeedback(
    feedback: StudySchedulerFeedback,
    context: SchedulerFeedbackContext,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val accessibility = resolveStudySchedulerFeedbackAccessibility(feedback)
    val visualFocus = StudyVisualFocusResolver.resolve(StudyVisualFocusRole.SCHEDULER)
    val presentation = SchedulerFeedbackPresentationResolver.resolve(context)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility.conciseSummary
            }
            .padding(
                horizontal =
                    if (context == SchedulerFeedbackContext.ACTIVE_ANSWER) LESpacing.xs
                    else LESpacing.md,
                vertical = LESpacing.xs
            )
    ) {
            Row(
                modifier =
                    if (presentation.maximumContentWidthDp != null) {
                        Modifier.widthIn(max = presentation.maximumContentWidthDp.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier =
                        if (presentation.allowsCompactWrap) Modifier.weight(1f)
                        else Modifier,
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = feedback.rating,
                        style = LETheme.typography.statusText,
                        color =
                            if (
                                presentation.preservesSemanticRatingIdentity &&
                                feedback.committedRating != null
                            ) {
                                resolveSchedulerRatingIdentityColor(
                                    feedback.committedRating,
                                    LETheme.colors
                                )
                            } else {
                                visualFocus.resolveContentColor(LETheme.colors)
                            },
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Ôn lại sau ${feedback.scheduledInterval}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight =
                            if (presentation.emphasis == SchedulerFeedbackEmphasis.EXPLANATORY) {
                                FontWeight.Normal
                            } else {
                                FontWeight.SemiBold
                            },
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
