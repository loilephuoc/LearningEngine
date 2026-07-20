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

/**
 * Golden-value regression tests cho FSRS-6.
 *
 * Những giá trị trong file này là reference cố định.
 * Nếu công thức hoặc bộ parameters thay đổi, test sẽ báo lỗi để
 * sự thay đổi không xảy ra âm thầm.
 */
class DefaultFsrsAlgorithmReferenceTest {

    private val algorithm =
        DefaultFsrsAlgorithm()

    @Test
    fun `first AGAIN review matches FSRS reference values`() {
        assertFirstReviewReference(
            rating = ReviewRating.AGAIN,
            expectedStage = LearningStage.LEARNING,
            expectedDifficulty = 6.4133,
            expectedStabilityDays = 0.2120,
            expectedIntervalDays = 1L
        )
    }

    @Test
    fun `first HARD review matches FSRS reference values`() {
        assertFirstReviewReference(
            rating = ReviewRating.HARD,
            expectedStage = LearningStage.REVIEW,
            expectedDifficulty = 5.112170705601055,
            expectedStabilityDays = 1.2931,
            expectedIntervalDays = 1L
        )
    }

    @Test
    fun `first GOOD review matches FSRS reference values`() {
        assertFirstReviewReference(
            rating = ReviewRating.GOOD,
            expectedStage = LearningStage.REVIEW,
            expectedDifficulty = 2.118103970459015,
            expectedStabilityDays = 2.3065,
            expectedIntervalDays = 2L
        )
    }

    @Test
    fun `first EASY review matches FSRS reference values`() {
        assertFirstReviewReference(
            rating = ReviewRating.EASY,
            expectedStage = LearningStage.REVIEW,
            expectedDifficulty = 1.0,
            expectedStabilityDays = 8.2956,
            expectedIntervalDays = 8L
        )
    }

    private fun assertFirstReviewReference(
        rating: ReviewRating,
        expectedStage: LearningStage,
        expectedDifficulty: Double,
        expectedStabilityDays: Double,
        expectedIntervalDays: Long
    ) {
        val reviewedAt =
            Moment(1_000_000L)

        val initialState =
            newMemoryState(reviewedAt)

        val decision =
            algorithm.schedule(
                currentState = initialState,
                rating = rating,
                reviewedAt = reviewedAt
            )

        assertEquals(
            initialState,
            decision.previousState
        )

        assertEquals(
            expectedStage,
            decision.nextState.stage
        )

        assertEquals(
            expectedDifficulty,
            decision.nextState.difficulty,
            DOUBLE_TOLERANCE
        )

        assertEquals(
            expectedStabilityDays,
            decision.nextState.stabilityDays,
            DOUBLE_TOLERANCE
        )

        assertEquals(
            TimeSpan.days(expectedIntervalDays),
            decision.scheduledInterval
        )

        assertEquals(
            reviewedAt +
                    TimeSpan.days(expectedIntervalDays),
            decision.nextState.dueAt
        )

        assertEquals(
            reviewedAt,
            decision.nextState.lastReviewedAt
        )

        assertEquals(
            1,
            decision.nextState.reviewCount
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )

        assertTrue(
            decision.nextState.difficulty.isFinite()
        )

        assertTrue(
            decision.nextState.stabilityDays.isFinite()
        )
    }

    private fun newMemoryState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId =
                LearnerId("learner-reference"),
            learningItemId =
                LearningItemId("item-reference"),
            availableAt = availableAt
        )

    private companion object {

        const val DOUBLE_TOLERANCE =
            1e-12
    }
}