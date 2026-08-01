package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.domain.study.memory.model.ReviewRating

class RatingActionFeedbackTest {
    @Test
    fun `each activation selects only its semantic rating`() {
        ReviewRating.entries.forEach { selected ->
            val feedback = RatingActionFeedback(selected, 1L, RatingFeedbackPhase.ACTIVATED)
            ReviewRating.entries.forEach { candidate ->
                val visual = resolveRatingFeedbackVisual(candidate, feedback)
                assertEquals(candidate == selected, visual.selected)
                assertFalse(visual.confirmed)
            }
        }
    }

    @Test
    fun `confirmation adds a non-color marker only to the committed rating`() {
        val feedback = RatingActionFeedback(ReviewRating.GOOD, 7L, RatingFeedbackPhase.CONFIRMED)

        assertTrue(resolveRatingFeedbackVisual(ReviewRating.GOOD, feedback).confirmed)
        assertFalse(resolveRatingFeedbackVisual(ReviewRating.AGAIN, feedback).confirmed)
        assertFalse(resolveRatingFeedbackVisual(ReviewRating.HARD, feedback).confirmed)
        assertFalse(resolveRatingFeedbackVisual(ReviewRating.EASY, feedback).confirmed)
    }

    @Test
    fun `consecutive same ratings receive unique tokens and confirmation preserves identity`() {
        val tokens = RatingFeedbackTokenGenerator()
        val first = tokens.activate(ReviewRating.GOOD)
        val second = tokens.activate(ReviewRating.GOOD)

        assertNotEquals(first.token, second.token)
        assertEquals(first.token, confirmRatingFeedback(first).token)
        assertEquals(ReviewRating.GOOD, confirmRatingFeedback(first).rating)
    }

    @Test
    fun `semantic roles retain Again Hard Good Easy color identities`() {
        assertEquals(RatingSemanticRole.AGAIN, resolveRatingFeedbackVisual(ReviewRating.AGAIN, null).semanticRole)
        assertEquals(RatingSemanticRole.HARD, resolveRatingFeedbackVisual(ReviewRating.HARD, null).semanticRole)
        assertEquals(RatingSemanticRole.GOOD, resolveRatingFeedbackVisual(ReviewRating.GOOD, null).semanticRole)
        assertEquals(RatingSemanticRole.EASY, resolveRatingFeedbackVisual(ReviewRating.EASY, null).semanticRole)
    }

    @Test
    fun `manual forced and automatic paths publish the final rating through one feedback contract`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyViewModel.kt")
        )

        assertTrue(source.contains("ratingFeedback = rating"))
        assertTrue(source.contains("ratingFeedback = ReviewRating.AGAIN"))
        assertTrue(source.contains("ratingFeedback = request.decision.rating"))
        assertTrue(source.contains("activation?.let(::confirmRatingFeedback)"))
    }
}
