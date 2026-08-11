package vn.loi.learning.android.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import vn.loi.learning.android.ui.LearningEngineThemeTokens
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.application.typing.TypingAutoRatingReason
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome

data class TypingRatingTransitionPresentation(
    val previousLabel: String,
    val previousRating: ReviewRating?,
    val finalRating: ReviewRating,
    val reasonLabel: String?,
    val accessibilityDescription: String
)

internal fun typingRatingTransitionPresentation(
    state: AndroidStudyState.Typing
): TypingRatingTransitionPresentation? = resolveTypingRatingTransition(
    previousCanonicalRating = state.previousCanonicalRating,
    automaticRating = state.automaticRating,
    manualRating = state.manualRating,
    eligible = state.canonicalRatingTransitionEligible,
    completionPending = state.completionPending,
    revealed = state.revealed,
    outcome = state.outcome,
    focusedPractice = state.hud?.focusedPractice == true
)

internal fun resolveTypingRatingTransition(
    previousCanonicalRating: ReviewRating?,
    automaticRating: vn.loi.learning.application.typing.TypingAutoRatingDecision?,
    manualRating: ReviewRating?,
    eligible: Boolean,
    completionPending: Boolean,
    revealed: Boolean,
    outcome: RecallOutcome?,
    focusedPractice: Boolean
): TypingRatingTransitionPresentation? {
    if (!eligible || !completionPending || revealed || outcome != RecallOutcome.CORRECT || focusedPractice) return null
    val finalRating = manualRating ?: automaticRating?.rating ?: return null
    val previousLabel = previousCanonicalRating?.name ?: "NEW"
    val manual = manualRating != null
    val reason = if (manual) "Manual rating" else automaticRating?.reason?.let(::typingAutoRatingReasonLabel)
    val transitionDescription = if (previousCanonicalRating == null) {
        "New item rated ${finalRating.displayName()}"
    } else {
        "Rating changed from ${previousCanonicalRating.displayName()} to ${finalRating.displayName()}"
    }
    return TypingRatingTransitionPresentation(
        previousLabel,
        previousCanonicalRating,
        finalRating,
        reason,
        if (manual) "$transitionDescription. Manual rating." else transitionDescription
    )
}

internal fun typingAutoRatingReasonLabel(reason: TypingAutoRatingReason): String = when (reason) {
    TypingAutoRatingReason.REVEAL_USED -> "Answer revealed"
    TypingAutoRatingReason.SLOW_ACTIVE_TYPING -> "Slow typing"
    TypingAutoRatingReason.VERY_SLOW_RECALL -> "Slow recall"
    TypingAutoRatingReason.SIGNIFICANT_TYPING_ERROR -> "Significant typing errors"
    TypingAutoRatingReason.REPEATED_TYPING_ERRORS -> "Repeated typing errors"
    TypingAutoRatingReason.MINOR_TYPO_CORRECTED -> "Minor typo corrected"
    TypingAutoRatingReason.SHORT_TERM_MEMORY_GUARD -> "Easy needs spaced evidence"
    TypingAutoRatingReason.CONFIDENCE_BELOW_HIGH -> "Easy needs higher confidence"
    TypingAutoRatingReason.CONFIDENCE_UNAVAILABLE -> "Easy needs confidence evidence"
    TypingAutoRatingReason.CONFIDENCE_UNRELIABLE -> "Easy needs reliable confidence"
    TypingAutoRatingReason.FAST_CLEAN_REVIEW -> "Fast, clean recall"
    TypingAutoRatingReason.STANDARD_EXACT -> "Exact recall"
}

@Composable
internal fun TypingRatingTransition(
    presentation: TypingRatingTransitionPresentation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = presentation.accessibilityDescription
        },
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            RatingTransitionBadge(
                presentation.previousLabel,
                presentation.previousRating?.let { ratingTone(it) }
                    ?: MaterialTheme.colorScheme.primaryContainer
            )
            Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
            RatingTransitionBadge(presentation.finalRating.name, ratingTone(presentation.finalRating))
        }
        presentation.reasonLabel?.let { reason ->
            Text(reason, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RatingTransitionBadge(label: String, color: Color) {
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(label, modifier = Modifier.padding(horizontal = LearningSpacing.small, vertical = LearningSpacing.extraSmall),
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ratingTone(rating: ReviewRating): Color = when (rating) {
    ReviewRating.AGAIN -> MaterialTheme.colorScheme.errorContainer
    ReviewRating.HARD -> LearningEngineThemeTokens.semanticColors.warning
    ReviewRating.GOOD -> LearningEngineThemeTokens.semanticColors.success
    ReviewRating.EASY -> MaterialTheme.colorScheme.tertiaryContainer
}

private fun ReviewRating.displayName(): String = name.lowercase().replaceFirstChar(Char::uppercase)
