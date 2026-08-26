package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.application.typing.TypingAutoRatingDecision
import vn.loi.learning.application.typing.TypingAutoRatingReason
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome

class TypingRatingTransitionTest {
    @Test
    fun `presentation maps NEW and canonical rating transitions`() {
        val cases = listOf(
            Triple(null, ReviewRating.GOOD, "New item rated Good"),
            Triple(ReviewRating.AGAIN, ReviewRating.HARD, "Rating changed from Again to Hard"),
            Triple(ReviewRating.HARD, ReviewRating.GOOD, "Rating changed from Hard to Good"),
            Triple(ReviewRating.GOOD, ReviewRating.EASY, "Rating changed from Good to Easy")
        )
        cases.forEach { (previous, final, description) ->
            val presentation = requireNotNull(resolve(previous, final))
            assertEquals(previous?.name ?: "NEW", presentation.previousLabel)
            assertEquals(final, presentation.finalRating)
            assertEquals(description, presentation.accessibilityDescription)
        }
    }

    @Test
    fun `manual rating is final authority and does not present automatic reason`() {
        val presentation = requireNotNull(
            resolveTypingRatingTransition(
                ReviewRating.AGAIN,
                decision(ReviewRating.GOOD, TypingAutoRatingReason.FAST_CLEAN_REVIEW),
                ReviewRating.HARD,
                true, true, false, RecallOutcome.CORRECT, false
            )
        )
        assertEquals(ReviewRating.HARD, presentation.finalRating)
        assertEquals("Manual rating", presentation.reasonLabel)
        assertEquals("Rating changed from Again to Hard. Manual rating.", presentation.accessibilityDescription)
    }

    @Test
    fun `automatic reasons have stable Android labels`() {
        val expected = mapOf(
            TypingAutoRatingReason.REVEAL_USED to "Answer revealed",
            TypingAutoRatingReason.SLOW_ACTIVE_TYPING to "Slow typing",
            TypingAutoRatingReason.VERY_SLOW_RECALL to "Slow recall",
            TypingAutoRatingReason.SIGNIFICANT_TYPING_ERROR to "Significant typing errors",
            TypingAutoRatingReason.REPEATED_TYPING_ERRORS to "Repeated typing errors",
            TypingAutoRatingReason.MINOR_TYPO_CORRECTED to "Minor typo corrected",
            TypingAutoRatingReason.SHORT_TERM_MEMORY_GUARD to "Easy needs spaced evidence",
            TypingAutoRatingReason.CONFIDENCE_BELOW_HIGH to "Easy needs higher confidence",
            TypingAutoRatingReason.CONFIDENCE_UNAVAILABLE to "Easy needs confidence evidence",
            TypingAutoRatingReason.CONFIDENCE_UNRELIABLE to "Easy needs reliable confidence",
            TypingAutoRatingReason.FAST_CLEAN_REVIEW to "Fast, clean recall",
            TypingAutoRatingReason.STANDARD_EXACT to "Exact recall"
        )
        assertEquals(TypingAutoRatingReason.entries.toSet(), expected.keys)
        expected.forEach { (reason, label) -> assertEquals(label, typingAutoRatingReasonLabel(reason)) }
    }

    @Test
    fun `forced reveal and every practice flow suppress canonical transition`() {
        assertNull(resolve(ReviewRating.AGAIN, ReviewRating.HARD, revealed = true))
        assertNull(resolve(ReviewRating.AGAIN, ReviewRating.HARD, eligible = false))
        assertNull(resolve(ReviewRating.AGAIN, ReviewRating.HARD, focusedPractice = true))
    }

    @Test
    fun `transition is projection owned non clickable and Compose never queries canonical state`() {
        val facade = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt"))
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/TypingRatingTransition.kt"))
        val modes = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/TypedAnswerStages.kt"))
        assertTrue(facade.substringAfter("fun typingPresentation").contains("previousCanonicalRating"))
        assertFalse(modes.contains("getContentLearningState"))
        assertFalse(screen.contains("getContentLearningState"))
        assertFalse(screen.contains("clickable"))
        assertTrue(modes.contains("typedResultPresentation(state)"))
        assertTrue(modes.contains("resolveTypingRatingTransition("))
    }

    private fun resolve(
        previous: ReviewRating?,
        final: ReviewRating,
        revealed: Boolean = false,
        eligible: Boolean = true,
        focusedPractice: Boolean = false
    ) = resolveTypingRatingTransition(
        previous,
        decision(final, TypingAutoRatingReason.STANDARD_EXACT),
        null,
        eligible,
        true,
        revealed,
        RecallOutcome.CORRECT,
        focusedPractice
    )

    private fun decision(rating: ReviewRating, reason: TypingAutoRatingReason) =
        TypingAutoRatingDecision(rating, reason, 1_000)
}
