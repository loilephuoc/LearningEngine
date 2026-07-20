package vn.loi.learning.domain.study.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

class DefaultFsrsAlgorithmSameDayTest {

    private val algorithm =
        DefaultFsrsAlgorithm()

    @Test
    fun `review at the same moment is accepted`() {
        val firstReviewedAt =
            Moment(1_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState =
                    newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        assertEquals(
            firstDecision.nextState,
            secondDecision.previousState
        )

        assertEquals(
            2,
            secondDecision.nextState.reviewCount
        )

        assertEquals(
            0,
            secondDecision.nextState.lapseCount
        )

        assertEquals(
            LearningStage.REVIEW,
            secondDecision.nextState.stage
        )

        assertEquals(
            firstReviewedAt,
            secondDecision.nextState.lastReviewedAt
        )

        assertTrue(
            secondDecision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            secondDecision.nextState.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
        )

        assertTrue(
            secondDecision.scheduledInterval >=
                    TimeSpan.days(1L)
        )

        assertEquals(
            firstReviewedAt +
                    secondDecision.scheduledInterval,
            secondDecision.nextState.dueAt
        )
    }

    @Test
    fun `GOOD review after five minutes keeps valid review state`() {
        val firstReviewedAt =
            Moment(2_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState =
                    newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.minutes(5L)

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.GOOD,
                reviewedAt = secondReviewedAt
            )

        assertValidSuccessfulSameDayReview(
            previousState = firstDecision.nextState,
            reviewedAt = secondReviewedAt,
            nextDecision = secondDecision
        )
    }

    @Test
    fun `GOOD review after thirty minutes keeps valid review state`() {
        val firstReviewedAt =
            Moment(3_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState =
                    newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.minutes(30L)

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.GOOD,
                reviewedAt = secondReviewedAt
            )

        assertValidSuccessfulSameDayReview(
            previousState = firstDecision.nextState,
            reviewedAt = secondReviewedAt,
            nextDecision = secondDecision
        )
    }

    @Test
    fun `same-day AGAIN review enters relearning and counts lapse`() {
        val firstReviewedAt =
            Moment(4_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState =
                    newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.minutes(30L)

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.AGAIN,
                reviewedAt = secondReviewedAt
            )

        assertEquals(
            firstDecision.nextState,
            secondDecision.previousState
        )

        assertEquals(
            LearningStage.RELEARNING,
            secondDecision.nextState.stage
        )

        assertEquals(
            2,
            secondDecision.nextState.reviewCount
        )

        assertEquals(
            1,
            secondDecision.nextState.lapseCount
        )

        assertEquals(
            secondReviewedAt,
            secondDecision.nextState.lastReviewedAt
        )

        assertTrue(
            secondDecision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            secondDecision.nextState.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
        )

        assertTrue(
            secondDecision.scheduledInterval >
                    TimeSpan.ZERO
        )

        assertEquals(
            secondReviewedAt +
                    secondDecision.scheduledInterval,
            secondDecision.nextState.dueAt
        )
    }

    @Test
    fun `all successful ratings remain valid during same-day review`() {
        val firstReviewedAt =
            Moment(5_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState =
                    newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.hours(2L)

        listOf(
            ReviewRating.HARD,
            ReviewRating.GOOD,
            ReviewRating.EASY
        ).forEach { rating ->
            val decision =
                algorithm.schedule(
                    currentState = firstDecision.nextState,
                    rating = rating,
                    reviewedAt = secondReviewedAt
                )

            assertValidSuccessfulSameDayReview(
                previousState = firstDecision.nextState,
                reviewedAt = secondReviewedAt,
                nextDecision = decision
            )
        }
    }

    private fun assertValidSuccessfulSameDayReview(
        previousState: MemoryState,
        reviewedAt: Moment,
        nextDecision:
        vn.loi.learning.domain.study.scheduling.SchedulerDecision
    ) {
        assertEquals(
            previousState,
            nextDecision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            nextDecision.nextState.stage
        )

        assertEquals(
            previousState.reviewCount + 1,
            nextDecision.nextState.reviewCount
        )

        assertEquals(
            previousState.lapseCount,
            nextDecision.nextState.lapseCount
        )

        assertEquals(
            reviewedAt,
            nextDecision.nextState.lastReviewedAt
        )

        assertTrue(
            nextDecision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            nextDecision.nextState.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
        )

        assertTrue(
            nextDecision.scheduledInterval >=
                    TimeSpan.days(1L)
        )

        assertEquals(
            reviewedAt +
                    nextDecision.scheduledInterval,
            nextDecision.nextState.dueAt
        )
    }

    private fun newMemoryState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId =
                LearnerId("learner-same-day"),
            learningItemId =
                LearningItemId("item-same-day"),
            availableAt = availableAt
        )

    private companion object {

        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0
    }
}