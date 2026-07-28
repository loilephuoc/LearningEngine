package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.application.study.ContentLearningState
import vn.loi.learning.domain.content.model.ContentId

class CurrentStudyItemReviewContextTest {
    @Test
    fun `New origin never exposes a previous rating`() {
        val context = resolveCurrentStudyItemReviewContext(
            SessionItemOrigin.NEW,
            listOf(event(ReviewRating.GOOD, 1))
        )

        assertNull(context.previousRating)
        ReviewRating.entries.forEach { rating ->
            assertFalse(isPreviousRatingIndicator(control(rating), context))
        }
    }

    @Test
    fun `Review origin exposes only the latest authoritative rating`() {
        ReviewRating.entries.forEach { latest ->
            val context = resolveCurrentStudyItemReviewContext(
                SessionItemOrigin.REVIEW,
                listOf(event(ReviewRating.AGAIN, 1), event(latest, 2))
            )

            assertEquals(latest, context.previousRating)
            ReviewRating.entries.forEach { candidate ->
                assertEquals(
                    candidate == latest,
                    isPreviousRatingIndicator(control(candidate), context)
                )
            }
        }
    }

    @Test
    fun `Review origin without history is safe and has no indicator`() {
        val context = resolveCurrentStudyItemReviewContext(
            SessionItemOrigin.REVIEW,
            emptyList()
        )

        assertNull(context.previousRating)
        assertTrue(ReviewRating.entries.none {
            isPreviousRatingIndicator(control(it), context)
        })
    }

    @Test
    fun `Content projection supplies previous rating from a sibling item`() {
        val sibling = LearningItemId("sibling")
        val latest = event(ReviewRating.EASY, 3)
        val context = resolveCurrentStudyItemReviewContext(
            SessionItemOrigin.REVIEW,
            ContentLearningState(
                ContentId("content"),
                setOf(sibling, latest.learningItemId),
                latest
            )
        )

        assertEquals(ReviewRating.EASY, context.previousRating)
        assertTrue(isPreviousRatingIndicator(StudyActionControl.REVIEW_EASY, context))
        assertFalse(isPreviousRatingIndicator(StudyActionControl.REVIEW_GOOD, context))
    }

    private fun control(rating: ReviewRating): StudyActionControl = when (rating) {
        ReviewRating.AGAIN -> StudyActionControl.REVIEW_AGAIN
        ReviewRating.HARD -> StudyActionControl.REVIEW_HARD
        ReviewRating.GOOD -> StudyActionControl.REVIEW_GOOD
        ReviewRating.EASY -> StudyActionControl.REVIEW_EASY
    }

    private fun event(rating: ReviewRating, reviewedAt: Long): ReviewEvent {
        val learner = LearnerId("learner")
        val item = LearningItemId("item")
        val before = MemoryState(
            learner,
            item,
            LearningStage.REVIEW,
            5.0,
            1.0,
            Moment(0),
            Moment(reviewedAt - 1),
            reviewedAt.toInt(),
            0
        )
        return ReviewEvent(
            ReviewEventId("event-$reviewedAt-$rating"),
            rating,
            Moment(reviewedAt),
            null,
            before,
            before.copy(
                dueAt = Moment(reviewedAt + 100),
                lastReviewedAt = Moment(reviewedAt),
                reviewCount = before.reviewCount + 1
            )
        )
    }
}
