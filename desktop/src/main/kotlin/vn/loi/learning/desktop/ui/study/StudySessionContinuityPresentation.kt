package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import vn.loi.learning.domain.study.memory.model.ReviewRating

enum class StudySessionTransitionPhase {
    RESULT_SHOWN,
    EXITING_CURRENT,
    ENTERING_NEXT
}

enum class StudySessionTransitionDestination { NEXT_ITEM, COMPLETION }

internal fun nextStudySessionTransitionPhase(
    phase: StudySessionTransitionPhase
): StudySessionTransitionPhase? = when (phase) {
    StudySessionTransitionPhase.RESULT_SHOWN -> StudySessionTransitionPhase.EXITING_CURRENT
    StudySessionTransitionPhase.EXITING_CURRENT -> StudySessionTransitionPhase.ENTERING_NEXT
    StudySessionTransitionPhase.ENTERING_NEXT -> null
}

@Immutable
data class StudySessionContinuityTransition(
    val token: Long,
    val sourceItemId: String,
    val destinationItemId: String?,
    val destination: StudySessionTransitionDestination,
    val finalRating: ReviewRating,
    val schedulerFeedback: StudySchedulerFeedback?,
    val phase: StudySessionTransitionPhase = StudySessionTransitionPhase.RESULT_SHOWN
) {
    init {
        require(destination == StudySessionTransitionDestination.COMPLETION || destinationItemId != null)
        require(destinationItemId != sourceItemId)
    }
}

@Immutable
data class StudySessionContinuityPresentation(
    val destinationVisible: Boolean,
    val destinationArriving: Boolean,
    val overlayVisible: Boolean,
    val consequenceVisible: Boolean,
    val retainOverlayDuringExit: Boolean
)

internal fun resolveStudySessionContinuityPresentation(
    transition: StudySessionContinuityTransition?
): StudySessionContinuityPresentation {
    if (transition == null) {
        return StudySessionContinuityPresentation(
            destinationVisible = true,
            destinationArriving = true,
            overlayVisible = false,
            consequenceVisible = false,
            retainOverlayDuringExit = true
        )
    }
    val destinationVisible = transition.phase != StudySessionTransitionPhase.EXITING_CURRENT
    return StudySessionContinuityPresentation(
        destinationVisible = destinationVisible,
        destinationArriving =
            transition.phase == StudySessionTransitionPhase.ENTERING_NEXT,
        overlayVisible = transition.phase == StudySessionTransitionPhase.RESULT_SHOWN,
        consequenceVisible = transition.phase == StudySessionTransitionPhase.RESULT_SHOWN,
        retainOverlayDuringExit =
            transition.destination == StudySessionTransitionDestination.COMPLETION
    )
}

internal fun createStudySessionContinuityTransition(
    activation: RatingActionFeedback,
    sourceItemId: String,
    committedState: StudyUiState
): StudySessionContinuityTransition {
    require(activation.phase == RatingFeedbackPhase.ACTIVATED)
    val consequence = committedState.schedulerFeedback
    val finalRating = consequence?.committedRating ?: activation.rating
    return StudySessionContinuityTransition(
        token = activation.token,
        sourceItemId = sourceItemId,
        destinationItemId = committedState.currentLearningItemId,
        destination =
            if (committedState.sessionCompleted) StudySessionTransitionDestination.COMPLETION
            else StudySessionTransitionDestination.NEXT_ITEM,
        finalRating = finalRating,
        schedulerFeedback = consequence
    )
}
