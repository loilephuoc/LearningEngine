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

class DefaultFsrsAlgorithmTest {

    private val algorithm =
        DefaultFsrsAlgorithm()

    @Test
    fun `first AGAIN review initializes learning state without counting a lapse`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newMemoryState(reviewedAt)

        val decision =
            algorithm.schedule(
                currentState = currentState,
                rating = ReviewRating.AGAIN,
                reviewedAt = reviewedAt
            )

        assertEquals(
            currentState,
            decision.previousState
        )

        assertEquals(
            LearningStage.LEARNING,
            decision.nextState.stage
        )

        assertEquals(
            1,
            decision.nextState.reviewCount
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )

        assertEquals(
            reviewedAt,
            decision.nextState.lastReviewedAt
        )

        assertTrue(
            decision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            decision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            decision.scheduledInterval > TimeSpan.ZERO
        )

        assertEquals(
            reviewedAt + decision.scheduledInterval,
            decision.nextState.dueAt
        )
    }

    @Test
    fun `first HARD review initializes review state and schedules next review`() {
        val reviewedAt =
            Moment(2_000_000L)

        val currentState =
            newMemoryState(reviewedAt)

        val decision =
            algorithm.schedule(
                currentState = currentState,
                rating = ReviewRating.HARD,
                reviewedAt = reviewedAt
            )

        assertEquals(
            currentState,
            decision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            decision.nextState.stage
        )

        assertEquals(
            1,
            decision.nextState.reviewCount
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )

        assertEquals(
            reviewedAt,
            decision.nextState.lastReviewedAt
        )

        assertTrue(
            decision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            decision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            decision.scheduledInterval >= TimeSpan.days(1L)
        )

        assertEquals(
            reviewedAt + decision.scheduledInterval,
            decision.nextState.dueAt
        )
    }

    @Test
    fun `first GOOD review initializes review state and schedules next review`() {
        val reviewedAt =
            Moment(3_000_000L)

        val currentState =
            newMemoryState(reviewedAt)

        val decision =
            algorithm.schedule(
                currentState = currentState,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )

        assertEquals(
            currentState,
            decision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            decision.nextState.stage
        )

        assertEquals(
            1,
            decision.nextState.reviewCount
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )

        assertEquals(
            reviewedAt,
            decision.nextState.lastReviewedAt
        )

        assertTrue(
            decision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            decision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            decision.scheduledInterval >= TimeSpan.days(1L)
        )

        assertEquals(
            reviewedAt + decision.scheduledInterval,
            decision.nextState.dueAt
        )
    }

    @Test
    fun `first EASY review initializes review state and schedules next review`() {
        val reviewedAt =
            Moment(4_000_000L)

        val currentState =
            newMemoryState(reviewedAt)

        val decision =
            algorithm.schedule(
                currentState = currentState,
                rating = ReviewRating.EASY,
                reviewedAt = reviewedAt
            )

        assertEquals(
            currentState,
            decision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            decision.nextState.stage
        )

        assertEquals(
            1,
            decision.nextState.reviewCount
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )

        assertEquals(
            reviewedAt,
            decision.nextState.lastReviewedAt
        )

        assertTrue(
            decision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            decision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            decision.scheduledInterval >= TimeSpan.days(1L)
        )

        assertEquals(
            reviewedAt + decision.scheduledInterval,
            decision.nextState.dueAt
        )
    }

    @Test
    fun `subsequent AGAIN review from review stage counts one lapse`() {
        val firstReviewedAt =
            Moment(5_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState = newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.days(5L)

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
            secondDecision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            secondDecision.scheduledInterval > TimeSpan.ZERO
        )

        assertEquals(
            secondReviewedAt +
                    secondDecision.scheduledInterval,
            secondDecision.nextState.dueAt
        )
    }

    @Test
    fun `subsequent HARD review remains in review stage without lapse`() {
        val firstReviewedAt =
            Moment(6_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState = newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.days(5L)

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.HARD,
                reviewedAt = secondReviewedAt
            )

        assertEquals(
            firstDecision.nextState,
            secondDecision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            secondDecision.nextState.stage
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
            secondReviewedAt,
            secondDecision.nextState.lastReviewedAt
        )

        assertTrue(
            secondDecision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            secondDecision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            secondDecision.scheduledInterval >=
                    TimeSpan.days(1L)
        )

        assertEquals(
            secondReviewedAt +
                    secondDecision.scheduledInterval,
            secondDecision.nextState.dueAt
        )
    }

    @Test
    fun `subsequent GOOD review remains in review stage without lapse`() {
        val firstReviewedAt =
            Moment(7_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState = newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.days(5L)

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.GOOD,
                reviewedAt = secondReviewedAt
            )

        assertEquals(
            firstDecision.nextState,
            secondDecision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            secondDecision.nextState.stage
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
            secondReviewedAt,
            secondDecision.nextState.lastReviewedAt
        )

        assertTrue(
            secondDecision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            secondDecision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            secondDecision.scheduledInterval >=
                    TimeSpan.days(1L)
        )

        assertEquals(
            secondReviewedAt +
                    secondDecision.scheduledInterval,
            secondDecision.nextState.dueAt
        )
    }

    @Test
    fun `subsequent EASY review remains in review stage without lapse`() {
        val firstReviewedAt =
            Moment(8_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState = newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.days(5L)

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.EASY,
                reviewedAt = secondReviewedAt
            )

        assertEquals(
            firstDecision.nextState,
            secondDecision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            secondDecision.nextState.stage
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
            secondReviewedAt,
            secondDecision.nextState.lastReviewedAt
        )

        assertTrue(
            secondDecision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            secondDecision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            secondDecision.scheduledInterval >=
                    TimeSpan.days(1L)
        )

        assertEquals(
            secondReviewedAt +
                    secondDecision.scheduledInterval,
            secondDecision.nextState.dueAt
        )
    }

    @Test
    fun `same-day GOOD review keeps stability positive`() {
        val firstReviewedAt =
            Moment(9_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState = newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.hours(2L)

        val secondDecision =
            algorithm.schedule(
                currentState = firstDecision.nextState,
                rating = ReviewRating.GOOD,
                reviewedAt = secondReviewedAt
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
            secondReviewedAt,
            secondDecision.nextState.lastReviewedAt
        )

        assertTrue(
            secondDecision.nextState.stabilityDays > 0.0
        )

        assertTrue(
            secondDecision.nextState.difficulty in 1.0..10.0
        )

        assertTrue(
            secondDecision.scheduledInterval >=
                    TimeSpan.days(1L)
        )

        assertEquals(
            secondReviewedAt +
                    secondDecision.scheduledInterval,
            secondDecision.nextState.dueAt
        )
    }

    @Test
    fun `review before last review time is rejected`() {
        val firstReviewedAt =
            Moment(10_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState = newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val invalidReviewTime =
            Moment(firstReviewedAt.epochMillis - 1L)

        val result =
            runCatching {
                algorithm.schedule(
                    currentState = firstDecision.nextState,
                    rating = ReviewRating.GOOD,
                    reviewedAt = invalidReviewTime
                )
            }

        assertTrue(
            result.exceptionOrNull() is IllegalArgumentException
        )
    }

    private fun newMemoryState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1"),
            availableAt = availableAt
        )
}