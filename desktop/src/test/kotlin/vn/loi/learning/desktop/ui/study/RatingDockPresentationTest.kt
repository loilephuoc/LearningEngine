package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

class RatingDockPresentationTest {
    @Test
    fun `question new exposes four equal read-only segments`() {
        val result = resolveRatingDockPresentation(
            RatingDockMode.QUESTION_CONTEXT,
            CurrentStudyItemReviewContext(SessionItemOrigin.NEW, null)
        )

        assertEquals(4, result.size)
        assertTrue(result.none(RatingSegmentPresentation::isPreviousRating))
        assertTrue(result.none(RatingSegmentPresentation::isSubdued))
    }

    @Test
    fun `question previous Good emphasizes only Good`() {
        val result = resolveRatingDockPresentation(
            RatingDockMode.QUESTION_CONTEXT,
            CurrentStudyItemReviewContext(SessionItemOrigin.REVIEW, ReviewRating.GOOD)
        )

        val good = result.single { it.control == StudyActionControl.REVIEW_GOOD }
        assertTrue(good.isPreviousRating)
        assertFalse(good.isSubdued)
        assertTrue(result.filterNot { it == good }.all(RatingSegmentPresentation::isSubdued))
    }

    @Test
    fun `all previous ratings map to their matching segment`() {
        ReviewRating.entries.forEach { rating ->
            val result = resolveRatingDockPresentation(
                RatingDockMode.QUESTION_CONTEXT,
                CurrentStudyItemReviewContext(SessionItemOrigin.REVIEW, rating)
            )
            assertEquals(rating, result.single(RatingSegmentPresentation::isPreviousRating).control.ratingOrNull())
        }
    }

    @Test
    fun `answer mode keeps non previous actions fully emphasized`() {
        val result = resolveRatingDockPresentation(
            RatingDockMode.ANSWER_ACTIONS,
            CurrentStudyItemReviewContext(SessionItemOrigin.REVIEW, ReviewRating.GOOD)
        )
        assertTrue(result.none(RatingSegmentPresentation::isSubdued))
    }
}
