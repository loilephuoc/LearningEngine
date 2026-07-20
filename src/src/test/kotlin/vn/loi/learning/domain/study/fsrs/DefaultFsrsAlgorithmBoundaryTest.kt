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

class DefaultFsrsAlgorithmBoundaryTest {

    private val algorithm =
        DefaultFsrsAlgorithm()

    @Test
    fun `repeated AGAIN reviews in learning keep FSRS values within valid boundaries`() {
        var reviewedAt =
            Moment(1_000_000L)

        var state =
            newMemoryState(reviewedAt)

        repeat(REVIEW_REPETITIONS) {
            val decision =
                algorithm.schedule(
                    currentState = state,
                    rating = ReviewRating.AGAIN,
                    reviewedAt = reviewedAt
                )

            assertValidDecision(
                previousState = state,
                reviewedAt = reviewedAt,
                nextState = decision.nextState,
                scheduledInterval =
                    decision.scheduledInterval
            )

            state = decision.nextState
            reviewedAt += TimeSpan.days(1L)
        }

        assertEquals(
            REVIEW_REPETITIONS,
            state.reviewCount
        )

        /*
         * AGAIN khi item đang NEW hoặc LEARNING
         * không được tính là lapse.
         *
         * Lapse chỉ xảy ra khi một item thuộc REVIEW
         * bị đánh giá AGAIN.
         */
        assertEquals(
            0,
            state.lapseCount
        )

        assertEquals(
            LearningStage.LEARNING,
            state.stage
        )

        assertTrue(
            state.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
        )

        assertTrue(
            state.stabilityDays > 0.0
        )

        assertTrue(
            state.stabilityDays.isFinite()
        )
    }

    @Test
    fun `repeated EASY reviews keep FSRS values within valid boundaries`() {
        var reviewedAt =
            Moment(2_000_000L)

        var state =
            newMemoryState(reviewedAt)

        repeat(REVIEW_REPETITIONS) {
            val decision =
                algorithm.schedule(
                    currentState = state,
                    rating = ReviewRating.EASY,
                    reviewedAt = reviewedAt
                )

            assertValidDecision(
                previousState = state,
                reviewedAt = reviewedAt,
                nextState = decision.nextState,
                scheduledInterval =
                    decision.scheduledInterval
            )

            state = decision.nextState
            reviewedAt += TimeSpan.days(30L)
        }

        assertEquals(
            REVIEW_REPETITIONS,
            state.reviewCount
        )

        assertEquals(
            0,
            state.lapseCount
        )

        assertEquals(
            LearningStage.REVIEW,
            state.stage
        )

        assertTrue(
            state.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
        )

        assertTrue(
            state.stabilityDays > 0.0
        )

        assertTrue(
            state.stabilityDays.isFinite()
        )
    }

    @Test
    fun `very long elapsed review keeps result finite and valid`() {
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
            firstReviewedAt +
                    TimeSpan.days(VERY_LONG_INTERVAL_DAYS)

        ReviewRating.entries.forEach { rating ->
            val decision =
                algorithm.schedule(
                    currentState = firstDecision.nextState,
                    rating = rating,
                    reviewedAt = secondReviewedAt
                )

            assertValidDecision(
                previousState = firstDecision.nextState,
                reviewedAt = secondReviewedAt,
                nextState = decision.nextState,
                scheduledInterval =
                    decision.scheduledInterval
            )

            assertTrue(
                decision.nextState.stabilityDays.isFinite()
            )
        }
    }

    @Test
    fun `many mixed reviews never produce invalid FSRS state`() {
        val ratings =
            listOf(
                ReviewRating.GOOD,
                ReviewRating.HARD,
                ReviewRating.EASY,
                ReviewRating.AGAIN
            )

        var reviewedAt =
            Moment(4_000_000L)

        var state =
            newMemoryState(reviewedAt)

        repeat(MIXED_REVIEW_COUNT) { index ->
            val rating =
                ratings[index % ratings.size]

            val decision =
                algorithm.schedule(
                    currentState = state,
                    rating = rating,
                    reviewedAt = reviewedAt
                )

            assertValidDecision(
                previousState = state,
                reviewedAt = reviewedAt,
                nextState = decision.nextState,
                scheduledInterval =
                    decision.scheduledInterval
            )

            state = decision.nextState
            reviewedAt +=
                TimeSpan.days(
                    (index % MAX_ELAPSED_DAYS + 1)
                        .toLong()
                )
        }

        assertEquals(
            MIXED_REVIEW_COUNT,
            state.reviewCount
        )

        /*
         * Chuỗi hỗn hợp luôn đưa item vào REVIEW
         * trước khi gặp AGAIN, nên phải có lapse.
         */
        assertTrue(
            state.lapseCount > 0
        )

        assertTrue(
            state.lapseCount <= state.reviewCount
        )

        assertTrue(
            state.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
        )

        assertTrue(
            state.stabilityDays > 0.0
        )

        assertTrue(
            state.stabilityDays.isFinite()
        )
    }

    private fun assertValidDecision(
        previousState: MemoryState,
        reviewedAt: Moment,
        nextState: MemoryState,
        scheduledInterval: TimeSpan
    ) {
        assertEquals(
            previousState.reviewCount + 1,
            nextState.reviewCount
        )

        assertTrue(
            nextState.lapseCount >=
                    previousState.lapseCount
        )

        assertTrue(
            nextState.lapseCount <=
                    nextState.reviewCount
        )

        assertEquals(
            reviewedAt,
            nextState.lastReviewedAt
        )

        assertTrue(
            nextState.difficulty in
                    MIN_DIFFICULTY..MAX_DIFFICULTY
        )

        assertTrue(
            nextState.difficulty.isFinite()
        )

        assertTrue(
            nextState.stabilityDays > 0.0
        )

        assertTrue(
            nextState.stabilityDays.isFinite()
        )

        assertTrue(
            scheduledInterval > TimeSpan.ZERO
        )

        assertEquals(
            reviewedAt + scheduledInterval,
            nextState.dueAt
        )
    }

    private fun newMemoryState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId =
                LearnerId("learner-boundary"),
            learningItemId =
                LearningItemId("item-boundary"),
            availableAt = availableAt
        )

    private companion object {

        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0

        const val REVIEW_REPETITIONS = 50
        const val MIXED_REVIEW_COUNT = 100
        const val MAX_ELAPSED_DAYS = 30

        const val VERY_LONG_INTERVAL_DAYS = 10_000L
    }
}