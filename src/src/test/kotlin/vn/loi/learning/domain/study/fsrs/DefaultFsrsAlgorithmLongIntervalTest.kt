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

class DefaultFsrsAlgorithmLongIntervalTest {

    private val algorithm =
        DefaultFsrsAlgorithm()

    @Test
    fun `GOOD review after one day remains valid`() {
        verifyLongIntervalReview(
            elapsed = TimeSpan.days(1L)
        )
    }

    @Test
    fun `GOOD review after seven days remains valid`() {
        verifyLongIntervalReview(
            elapsed = TimeSpan.days(7L)
        )
    }

    @Test
    fun `GOOD review after thirty days remains valid`() {
        verifyLongIntervalReview(
            elapsed = TimeSpan.days(30L)
        )
    }

    @Test
    fun `GOOD review after one hundred eighty days remains valid`() {
        verifyLongIntervalReview(
            elapsed = TimeSpan.days(180L)
        )
    }

    @Test
    fun `long elapsed reviews preserve rating ordering`() {
        val firstReviewedAt =
            Moment(10_000_000L)

        val reviewState =
            algorithm.schedule(
                currentState =
                    newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            ).nextState

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.days(180L)

        val again =
            algorithm.schedule(
                currentState = reviewState,
                rating = ReviewRating.AGAIN,
                reviewedAt = secondReviewedAt
            )

        val hard =
            algorithm.schedule(
                currentState = reviewState,
                rating = ReviewRating.HARD,
                reviewedAt = secondReviewedAt
            )

        val good =
            algorithm.schedule(
                currentState = reviewState,
                rating = ReviewRating.GOOD,
                reviewedAt = secondReviewedAt
            )

        val easy =
            algorithm.schedule(
                currentState = reviewState,
                rating = ReviewRating.EASY,
                reviewedAt = secondReviewedAt
            )

        assertEquals(
            LearningStage.RELEARNING,
            again.nextState.stage
        )

        assertEquals(
            LearningStage.REVIEW,
            hard.nextState.stage
        )

        assertEquals(
            LearningStage.REVIEW,
            good.nextState.stage
        )

        assertEquals(
            LearningStage.REVIEW,
            easy.nextState.stage
        )

        assertTrue(
            again.nextState.stabilityDays <
                    hard.nextState.stabilityDays
        )

        assertTrue(
            hard.nextState.stabilityDays <
                    good.nextState.stabilityDays
        )

        assertTrue(
            good.nextState.stabilityDays <
                    easy.nextState.stabilityDays
        )

        assertTrue(
            again.scheduledInterval <
                    hard.scheduledInterval
        )

        assertTrue(
            hard.scheduledInterval <=
                    good.scheduledInterval
        )

        assertTrue(
            good.scheduledInterval <=
                    easy.scheduledInterval
        )
    }

    private fun verifyLongIntervalReview(
        elapsed: TimeSpan
    ) {
        val firstReviewedAt =
            Moment(1_000_000L)

        val firstDecision =
            algorithm.schedule(
                currentState =
                    newMemoryState(firstReviewedAt),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            )

        val secondReviewedAt =
            firstReviewedAt + elapsed

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
            secondDecision.nextState.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
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

    private fun newMemoryState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId =
                LearnerId("learner-long-interval"),
            learningItemId =
                LearningItemId("item-long-interval"),
            availableAt = availableAt
        )

    private companion object {

        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0
    }
}