package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import vn.loi.learning.domain.study.memory.model.ReviewRating

enum class StudySessionTransitionPhase {
    ACTION_CONFIRMED,
    CONSEQUENCE_VISIBLE,
    DESTINATION_ARRIVING
}

enum class StudySessionTransitionDestination { NEXT_ITEM, COMPLETION }

@Immutable
data class StudySessionContinuityTransition(
    val token: Long,
    val sourceItemId: String,
    val destinationItemId: String?,
    val destination: StudySessionTransitionDestination,
    val finalRating: ReviewRating,
    val schedulerFeedback: StudySchedulerFeedback,
    val phase: StudySessionTransitionPhase = StudySessionTransitionPhase.ACTION_CONFIRMED
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
    val destinationVisible =
        transition.destination == StudySessionTransitionDestination.COMPLETION ||
            transition.phase == StudySessionTransitionPhase.DESTINATION_ARRIVING
    return StudySessionContinuityPresentation(
        destinationVisible = destinationVisible,
        destinationArriving =
            transition.phase == StudySessionTransitionPhase.DESTINATION_ARRIVING,
        overlayVisible = transition.phase != StudySessionTransitionPhase.DESTINATION_ARRIVING,
        consequenceVisible = transition.phase == StudySessionTransitionPhase.CONSEQUENCE_VISIBLE,
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
    val consequence = requireNotNull(committedState.schedulerFeedback)
    val finalRating = requireNotNull(consequence.committedRating)
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

internal fun StudyUiState.advanceSessionContinuity(token: Long): StudyUiState {
    val transition = sessionContinuityTransition?.takeIf { it.token == token } ?: return this
    val nextPhase = when (transition.phase) {
        StudySessionTransitionPhase.ACTION_CONFIRMED ->
            StudySessionTransitionPhase.CONSEQUENCE_VISIBLE
        StudySessionTransitionPhase.CONSEQUENCE_VISIBLE ->
            StudySessionTransitionPhase.DESTINATION_ARRIVING
        StudySessionTransitionPhase.DESTINATION_ARRIVING -> null
    }
    return if (nextPhase == null) {
        copy(
            sessionContinuityTransition = null,
            schedulerFeedback =
                if (transition.destination == StudySessionTransitionDestination.NEXT_ITEM) null
                else schedulerFeedback
        )
    } else {
        copy(sessionContinuityTransition = transition.copy(phase = nextPhase))
    }
}
