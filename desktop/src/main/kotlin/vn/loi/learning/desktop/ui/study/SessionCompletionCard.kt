package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButton
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

@Composable
fun SessionCompletionCard(
    completionUiState: SessionCompletionUiState,
    workspaceStrings: StudyWorkspaceStrings,
    schedulerFeedback: StudySchedulerFeedback? = null,
    continuousReviewEnabled: Boolean = false,
    continuousReviewAvailable: Boolean = false,
    onContinuousReviewChanged: (Boolean) -> Unit = {},
    onUndo: () -> Unit = {},
    undoAvailable: Boolean = false,
    onBackToLesson: ((InstalledPackageId, ContentId) -> Unit)? = null,
    onContinueLearning: ((InstalledPackageId, ContentId) -> Unit)? = null,
    onLearningAction: (StudyLearningAction) -> Unit = {},
    actionsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val pkgId = completionUiState.installedPackageId
    val contentId = completionUiState.contentId
    val backToLessonAvailable = onBackToLesson != null && pkgId != null && contentId != null
    val presentation = SessionCompletionPresentationResolver.resolve(
        state = completionUiState,
        actionsEnabled = actionsEnabled,
        backToLessonAvailable = backToLessonAvailable,
        undoAvailable = undoAvailable,
        continuousReviewAvailable = continuousReviewAvailable
    )
    val actionByIdentity = presentation.actions.associateBy { it.identity }

    LESurface(
        variant = LESurfaceVariant.PRIMARY,
        modifier = modifier.fillMaxWidth(),
        contentPadding = LETheme.spacing.space0,
        border = LETheme.borders.default,
        shadowElevation = LETheme.elevation.elevation1
    ) {
        Column(
            modifier = Modifier.padding(LETheme.spacing.space6),
            verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space5),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = LETheme.icons.Success,
                contentDescription = null,
                tint = LETheme.colors.accentPrimary
            )
            Text(
                text = completionUiState.statusLabel,
                style = LETheme.typography.displayWord,
                color = LETheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = completionUiState.lessonTitle,
                style = LETheme.typography.sectionTitle,
                color = LETheme.colors.textSecondary
            )
            completionUiState.packageName?.let { pkg ->
                Text(
                    text = "Package: $pkg",
                    style = LETheme.typography.bodyDefinition,
                    color = LETheme.colors.textSecondary
                )
            }

            LESurface(
                variant = LESurfaceVariant.SECONDARY,
                contentPadding = LETheme.spacing.space4,
                shadowElevation = LETheme.elevation.elevation0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space3)
                    ) {
                        presentation.outcomeItems.forEach { item ->
                            SessionSummaryMetric(
                                label = item.label,
                                value = item.value.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (completionUiState.reflectionMessage.isNotBlank()) {
                        Text(
                            text = completionUiState.reflectionMessage,
                            style = LETheme.typography.bodyDefinition,
                            color = LETheme.colors.textSecondary
                        )
                    }
                }
            }

            if (completionUiState.snapshot != null || schedulerFeedback != null) {
                LESurface(
                    variant = LESurfaceVariant.SCHEDULER,
                    contentPadding = LETheme.spacing.space4,
                    shadowElevation = LETheme.elevation.elevation0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space3)) {
                        completionUiState.snapshot?.let { snapshot ->
                            if (snapshot.whatWasLearned.isNotBlank()) {
                                CompletionSummarySection("What you learned", snapshot.whatWasLearned)
                            }
                            if (snapshot.overallOutcome.isNotBlank()) {
                                CompletionSummarySection("Overall outcome", snapshot.overallOutcome)
                            }
                            if (snapshot.reflection.isNotBlank()) {
                                CompletionSummarySection("Reflection", snapshot.reflection)
                            }
                        }
                        schedulerFeedback?.let {
                            CompactSchedulerFeedback(
                                feedback = it,
                                context = SchedulerFeedbackContext.COMPLETION
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = LETheme.colors.borderSubtle)

            completionUiState.learningActions.chunked(2).forEach { actions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEach { action ->
                        val ranked = requireNotNull(actionByIdentity[action.action.toCompletionIdentity()])
                        val invoke = {
                            if (
                                action.action == StudyLearningAction.CONTINUE &&
                                completionUiState.isLessonStudy &&
                                onContinueLearning != null &&
                                pkgId != null &&
                                contentId != null
                            ) {
                                onContinueLearning(pkgId, contentId)
                            } else {
                                onLearningAction(action.action)
                            }
                        }
                        LEButton(
                            label = action.label,
                            onClick = invoke,
                            variant = ranked.buttonVariant,
                            enabled = ranked.enabled,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (continuousReviewAvailable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = workspaceStrings.continuousReviewAccessibility
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = workspaceStrings.continuousReviewLabel,
                        style = LETheme.typography.bodyDefinition,
                        color = LETheme.colors.textSecondary
                    )
                    Switch(
                        checked = continuousReviewEnabled,
                        enabled = actionsEnabled,
                        onCheckedChange = onContinuousReviewChanged
                    )
                }
            }

            if (undoAvailable) {
                val undo = resolveStudyActionAccessibility(
                    StudyActionControl.UNDO_LATEST,
                    workspaceStrings
                )
                LEButton(
                    label = undo.visibleLabel,
                    onClick = onUndo,
                    variant = requireNotNull(actionByIdentity[CompletionActionIdentity.UNDO]).buttonVariant,
                    enabled = actionsEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (backToLessonAvailable) {
                LEButton(
                    label = "Back to Lesson",
                    onClick = { onBackToLesson(pkgId, contentId) },
                    variant = requireNotNull(
                        actionByIdentity[CompletionActionIdentity.BACK_TO_LESSON]
                    ).buttonVariant,
                    enabled = actionsEnabled
                )
            }
        }
    }
}
