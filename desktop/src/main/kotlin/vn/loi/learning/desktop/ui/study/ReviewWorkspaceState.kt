package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.memory.model.ReviewRating

/** Deterministic Desktop projection of the authoritative learning session. */
sealed interface ReviewWorkspaceState {

    val allowedActions: Set<ReviewWorkspaceAction>

    fun allows(action: ReviewWorkspaceAction): Boolean =
        action in allowedActions

    data object Idle : ReviewWorkspaceState {
        override val allowedActions = setOf(ReviewWorkspaceAction.Start)
    }

    data object Preparing : ReviewWorkspaceState {
        override val allowedActions = emptySet<ReviewWorkspaceAction>()
    }

    data object Question : ReviewWorkspaceState {
        override val allowedActions = setOf(ReviewWorkspaceAction.ShowAnswer)
    }

    data object AnswerRevealed : ReviewWorkspaceState {
        override val allowedActions = ReviewWorkspaceAction.ratingActions
    }

    data class Feedback(
        val rating: ReviewRating
    ) : ReviewWorkspaceState {
        override val allowedActions = emptySet<ReviewWorkspaceAction>()
    }

    data object Transitioning : ReviewWorkspaceState {
        override val allowedActions = emptySet<ReviewWorkspaceAction>()
    }

    data object Completed : ReviewWorkspaceState {
        override val allowedActions = setOf(ReviewWorkspaceAction.Start)
    }

    data object RecoverableFailure : ReviewWorkspaceState {
        override val allowedActions = setOf(ReviewWorkspaceAction.Retry)
    }

    companion object {
        fun projectLegacy(
            hasActiveSession: Boolean,
            canRevealAnswer: Boolean,
            canReview: Boolean,
            sessionCompleted: Boolean,
            hasLoadError: Boolean
        ): ReviewWorkspaceState =
            when {
                hasLoadError -> RecoverableFailure
                sessionCompleted -> Completed
                hasActiveSession && canReview -> AnswerRevealed
                hasActiveSession && canRevealAnswer -> Question
                hasActiveSession -> Preparing
                else -> Idle
            }
    }
}

sealed interface ReviewWorkspaceAction {
    data object Start : ReviewWorkspaceAction
    data object ShowAnswer : ReviewWorkspaceAction
    data class Rate(val rating: ReviewRating) : ReviewWorkspaceAction
    data object Retry : ReviewWorkspaceAction

    companion object {
        val ratingActions: Set<ReviewWorkspaceAction> =
            ReviewRating.entries
                .mapTo(linkedSetOf(), ReviewWorkspaceAction::Rate)
    }
}

object ReviewWorkspaceStateMachine {

    fun dispatch(
        state: ReviewWorkspaceState,
        action: ReviewWorkspaceAction
    ): ReviewWorkspaceState {
        require(state.allows(action)) {
            "Action $action is not allowed in workspace state $state."
        }

        return when (action) {
            ReviewWorkspaceAction.Start,
            ReviewWorkspaceAction.Retry -> ReviewWorkspaceState.Preparing

            ReviewWorkspaceAction.ShowAnswer -> ReviewWorkspaceState.AnswerRevealed

            is ReviewWorkspaceAction.Rate -> ReviewWorkspaceState.Feedback(action.rating)
        }
    }

    fun beginTransition(
        state: ReviewWorkspaceState
    ): ReviewWorkspaceState {
        require(state is ReviewWorkspaceState.Feedback) {
            "Only feedback can transition to the next item."
        }
        return ReviewWorkspaceState.Transitioning
    }

    fun projectNext(
        state: ReviewWorkspaceState,
        hasNextItem: Boolean,
        answerRevealed: Boolean = false
    ): ReviewWorkspaceState {
        require(
            state == ReviewWorkspaceState.Preparing ||
                    state == ReviewWorkspaceState.Transitioning
        ) {
            "Only preparation or transition can project the next item."
        }
        return when {
            !hasNextItem -> ReviewWorkspaceState.Completed
            answerRevealed -> ReviewWorkspaceState.AnswerRevealed
            else -> ReviewWorkspaceState.Question
        }
    }
}
