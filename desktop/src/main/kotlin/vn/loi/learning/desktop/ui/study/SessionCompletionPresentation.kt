package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant

enum class CompletionActionIdentity {
    CONTINUE,
    REVIEW_LATEST_NEW,
    REVIEW_AGAIN_HARD,
    REVIEW_ALL_LEARNED,
    BACK_TO_LIBRARY,
    BACK_TO_LESSON,
    UNDO,
    CONTINUOUS_REVIEW
}

enum class CompletionActionPriority { PRIMARY, SECONDARY, TERTIARY, RECOVERY, TOGGLE }

enum class CompletionVisualRole { ACKNOWLEDGEMENT, OUTCOME, CONSEQUENCE, ACTIONS }

@Immutable
data class CompletionActionPresentation(
    val identity: CompletionActionIdentity,
    val priority: CompletionActionPriority,
    val enabled: Boolean,
    val learningAction: StudyLearningAction? = null
) {
    val buttonVariant: LEButtonVariant
        get() = when (priority) {
            CompletionActionPriority.PRIMARY -> LEButtonVariant.PRIMARY
            CompletionActionPriority.SECONDARY -> LEButtonVariant.SECONDARY
            CompletionActionPriority.TERTIARY,
            CompletionActionPriority.RECOVERY -> LEButtonVariant.QUIET
            CompletionActionPriority.TOGGLE -> LEButtonVariant.SECONDARY
        }
}

@Immutable
data class SessionCompletionPresentation(
    val acknowledgementRole: CompletionVisualRole = CompletionVisualRole.ACKNOWLEDGEMENT,
    val outcomeRole: CompletionVisualRole = CompletionVisualRole.OUTCOME,
    val consequenceRole: CompletionVisualRole = CompletionVisualRole.CONSEQUENCE,
    val actionRole: CompletionVisualRole = CompletionVisualRole.ACTIONS,
    val outcomeItems: List<SessionCompletionOutcomeItem>,
    val actions: List<CompletionActionPresentation>
)

@Immutable
data class SessionCompletionOutcomeItem(val label: String, val value: Int)

object SessionCompletionPresentationResolver {
    fun resolve(
        state: SessionCompletionUiState,
        actionsEnabled: Boolean,
        backToLessonAvailable: Boolean,
        undoAvailable: Boolean,
        continuousReviewAvailable: Boolean
    ): SessionCompletionPresentation {
        val learningActions = state.learningActions.map { action ->
            CompletionActionPresentation(
                identity = action.action.toCompletionIdentity(),
                priority = CompletionActionPriority.SECONDARY,
                enabled = actionsEnabled && action.enabled,
                learningAction = action.action
            )
        }
        val primaryIdentity = listOf(
            CompletionActionIdentity.CONTINUE,
            CompletionActionIdentity.REVIEW_LATEST_NEW,
            CompletionActionIdentity.REVIEW_AGAIN_HARD,
            CompletionActionIdentity.REVIEW_ALL_LEARNED
        ).firstOrNull { identity ->
            learningActions.any { it.identity == identity && it.enabled }
        }
        val rankedLearningActions = learningActions.map { action ->
            action.copy(
                priority = when {
                    action.identity == primaryIdentity -> CompletionActionPriority.PRIMARY
                    action.identity == CompletionActionIdentity.BACK_TO_LIBRARY ->
                        CompletionActionPriority.TERTIARY
                    else -> CompletionActionPriority.SECONDARY
                }
            )
        }
        val supportingActions = buildList {
            if (continuousReviewAvailable) {
                add(CompletionActionPresentation(
                    CompletionActionIdentity.CONTINUOUS_REVIEW,
                    CompletionActionPriority.TOGGLE,
                    enabled = actionsEnabled
                ))
            }
            if (undoAvailable) {
                add(CompletionActionPresentation(
                    CompletionActionIdentity.UNDO,
                    CompletionActionPriority.RECOVERY,
                    enabled = actionsEnabled
                ))
            }
            if (backToLessonAvailable) {
                add(CompletionActionPresentation(
                    CompletionActionIdentity.BACK_TO_LESSON,
                    CompletionActionPriority.TERTIARY,
                    enabled = actionsEnabled
                ))
            }
        }
        return SessionCompletionPresentation(
            outcomeItems = buildList {
                add(SessionCompletionOutcomeItem("Total reviews", state.reviewedCount))
                if (state.newItemsReviewed > 0) {
                    add(SessionCompletionOutcomeItem("New", state.newItemsReviewed))
                }
                if (state.reviewItemsReviewed > 0) {
                    add(SessionCompletionOutcomeItem("Scheduled", state.reviewItemsReviewed))
                }
            },
            actions = rankedLearningActions + supportingActions
        )
    }
}

internal fun StudyLearningAction.toCompletionIdentity(): CompletionActionIdentity = when (this) {
    StudyLearningAction.CONTINUE -> CompletionActionIdentity.CONTINUE
    StudyLearningAction.REVIEW_LATEST_NEW -> CompletionActionIdentity.REVIEW_LATEST_NEW
    StudyLearningAction.REVIEW_AGAIN_HARD -> CompletionActionIdentity.REVIEW_AGAIN_HARD
    StudyLearningAction.REVIEW_ALL_LEARNED -> CompletionActionIdentity.REVIEW_ALL_LEARNED
    StudyLearningAction.BACK_TO_LIBRARY -> CompletionActionIdentity.BACK_TO_LIBRARY
}
