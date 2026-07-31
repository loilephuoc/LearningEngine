package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

class TypingRatingStatusPresentationTest {
    @Test
    fun `previous rating is the sole current status and following rating is upcoming`() {
        ReviewRating.entries.forEach { rating ->
            val segments =
                resolveTypingRatingStatusPresentation(
                    CurrentStudyItemReviewContext(SessionItemOrigin.REVIEW, rating)
                )
            val activeIndex = studyRatingOrder.indexOfFirst { it.ratingOrNull() == rating }

            assertEquals(4, segments.size)
            assertEquals(1, segments.count(TypingRatingStatusSegment::isActive))
            assertEquals(TypingRatingStatus.CURRENT, segments[activeIndex].status)
            segments.getOrNull(activeIndex + 1)?.let {
                assertEquals(TypingRatingStatus.UPCOMING, it.status)
            }
            assertTrue(
                segments.filterIndexed { index, _ ->
                    index != activeIndex && index != activeIndex + 1
                }.all { it.status == TypingRatingStatus.AVAILABLE }
            )
        }
    }

    @Test
    fun `new item and missing rating do not invent an active status`() {
        assertTrue(
            resolveTypingRatingStatusPresentation(
                CurrentStudyItemReviewContext(SessionItemOrigin.NEW, null)
            ).isEmpty()
        )
        assertTrue(resolveTypingRatingStatusPresentation(null).isEmpty())
    }
}
