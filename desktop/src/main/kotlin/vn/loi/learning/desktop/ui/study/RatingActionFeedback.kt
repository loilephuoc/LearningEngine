package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import vn.loi.learning.domain.study.memory.model.ReviewRating

enum class RatingFeedbackPhase { ACTIVATED, CONFIRMED }

@Immutable
data class RatingActionFeedback(
    val rating: ReviewRating,
    val token: Long,
    val phase: RatingFeedbackPhase
)

internal class RatingFeedbackTokenGenerator {
    private var lastToken = 0L

    fun activate(rating: ReviewRating): RatingActionFeedback =
        RatingActionFeedback(rating, ++lastToken, RatingFeedbackPhase.ACTIVATED)
}

internal fun confirmRatingFeedback(activation: RatingActionFeedback): RatingActionFeedback {
    require(activation.phase == RatingFeedbackPhase.ACTIVATED)
    return activation.copy(phase = RatingFeedbackPhase.CONFIRMED)
}

internal fun StudyUiState.consumeRatingActionFeedback(token: Long): StudyUiState =
    if (ratingActionFeedback?.token == token) copy(ratingActionFeedback = null) else this

internal enum class RatingSemanticRole { AGAIN, HARD, GOOD, EASY }

@Immutable
internal data class RatingFeedbackVisualState(
    val selected: Boolean,
    val confirmed: Boolean,
    val semanticRole: RatingSemanticRole
)

internal fun resolveRatingFeedbackVisual(
    rating: ReviewRating,
    feedback: RatingActionFeedback?
): RatingFeedbackVisualState = RatingFeedbackVisualState(
    selected = feedback?.rating == rating,
    confirmed = feedback?.rating == rating && feedback.phase == RatingFeedbackPhase.CONFIRMED,
    semanticRole = when (rating) {
        ReviewRating.AGAIN -> RatingSemanticRole.AGAIN
        ReviewRating.HARD -> RatingSemanticRole.HARD
        ReviewRating.GOOD -> RatingSemanticRole.GOOD
        ReviewRating.EASY -> RatingSemanticRole.EASY
    }
)
