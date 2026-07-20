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
import vn.loi.learning.domain.study.scheduling.SchedulerDecision

class DefaultFsrsAlgorithmOrderingTest {

    private val algorithm =
        DefaultFsrsAlgorithm()

    @Test
    fun `subsequent review orders stability and interval by rating`() {
        val firstReviewedAt =
            Moment(1_000_000L)

        val reviewState =
            algorithm.schedule(
                currentState =
                    newMemoryState(
                        availableAt = firstReviewedAt
                    ),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            ).nextState

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.days(5L)

        val againDecision =
            scheduleFromSameState(
                currentState = reviewState,
                rating = ReviewRating.AGAIN,
                reviewedAt = secondReviewedAt
            )

        val hardDecision =
            scheduleFromSameState(
                currentState = reviewState,
                rating = ReviewRating.HARD,
                reviewedAt = secondReviewedAt
            )

        val goodDecision =
            scheduleFromSameState(
                currentState = reviewState,
                rating = ReviewRating.GOOD,
                reviewedAt = secondReviewedAt
            )

        val easyDecision =
            scheduleFromSameState(
                currentState = reviewState,
                rating = ReviewRating.EASY,
                reviewedAt = secondReviewedAt
            )

        assertEquals(
            LearningStage.RELEARNING,
            againDecision.nextState.stage
        )

        assertEquals(
            LearningStage.REVIEW,
            hardDecision.nextState.stage
        )

        assertEquals(
            LearningStage.REVIEW,
            goodDecision.nextState.stage
        )

        assertEquals(
            LearningStage.REVIEW,
            easyDecision.nextState.stage
        )

        assertTrue(
            againDecision.nextState.stabilityDays <
                    hardDecision.nextState.stabilityDays
        )

        assertTrue(
            hardDecision.nextState.stabilityDays <
                    goodDecision.nextState.stabilityDays
        )

        assertTrue(
            goodDecision.nextState.stabilityDays <
                    easyDecision.nextState.stabilityDays
        )

        assertTrue(
            againDecision.scheduledInterval <
                    hardDecision.scheduledInterval
        )

        assertTrue(
            hardDecision.scheduledInterval <=
                    goodDecision.scheduledInterval
        )

        assertTrue(
            goodDecision.scheduledInterval <=
                    easyDecision.scheduledInterval
        )
    }

    @Test
    fun `all rating results preserve valid FSRS boundaries`() {
        val firstReviewedAt =
            Moment(2_000_000L)

        val reviewState =
            algorithm.schedule(
                currentState =
                    newMemoryState(
                        availableAt = firstReviewedAt
                    ),
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewedAt
            ).nextState

        val secondReviewedAt =
            firstReviewedAt + TimeSpan.days(30L)

        ReviewRating.entries.forEach { rating ->
            val decision =
                scheduleFromSameState(
                    currentState = reviewState,
                    rating = rating,
                    reviewedAt = secondReviewedAt
                )

            assertEquals(
                reviewState,
                decision.previousState
            )

            assertEquals(
                2,
                decision.nextState.reviewCount
            )

            assertTrue(
                decision.nextState.difficulty in
                        MIN_DIFFICULTY..MAX_DIFFICULTY
            )

            assertTrue(
                decision.nextState.stabilityDays > 0.0
            )

            assertTrue(
                decision.scheduledInterval > TimeSpan.ZERO
            )

            assertEquals(
                secondReviewedAt,
                decision.nextState.lastReviewedAt
            )

            assertEquals(
                secondReviewedAt +
                        decision.scheduledInterval,
                decision.nextState.dueAt
            )
        }
    }

    private fun scheduleFromSameState(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision =
        algorithm.schedule(
            currentState = currentState,
            rating = rating,
            reviewedAt = reviewedAt
        )

    private fun newMemoryState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId = LearnerId("learner-ordering"),
            learningItemId =
                LearningItemId("item-ordering"),
            availableAt = availableAt
        )

    private companion object {

        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0
    }
}