package vn.loi.learning.domain.study.scheduling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

class SchedulerMetricsCalculatorTest {

    private val calculator =
        SchedulerMetricsCalculator()

    private val learnerId =
        LearnerId(
            "scheduler-metrics-learner"
        )

    private val learningItemId =
        LearningItemId(
            "scheduler-metrics-item"
        )

    @Test
    fun `calculates scheduler decision deltas`() {
        val reviewedAt =
            Moment(
                1_000_000L
            )

        val interval =
            TimeSpan.days(
                2
            )

        val previousState =
            MemoryState(
                learnerId = learnerId,
                learningItemId = learningItemId,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 2.0,
                dueAt = reviewedAt,
                lastReviewedAt =
                    Moment(
                        500_000L
                    ),
                reviewCount = 3,
                lapseCount = 1
            )

        val nextState =
            previousState.copy(
                stage =
                    LearningStage.RELEARNING,
                difficulty =
                    5.8,
                stabilityDays =
                    1.0,
                dueAt =
                    reviewedAt + interval,
                lastReviewedAt =
                    reviewedAt,
                reviewCount =
                    4,
                lapseCount =
                    2
            )

        val metrics =
            calculator.calculate(
                SchedulerDecision(
                    previousState =
                        previousState,
                    nextState =
                        nextState,
                    scheduledInterval =
                        interval
                )
            )

        assertEquals(
            expected = interval,
            actual =
                metrics.scheduledInterval
        )

        assertEquals(
            expected =
                LearningStage.REVIEW,
            actual =
                metrics.previousStage
        )

        assertEquals(
            expected =
                LearningStage.RELEARNING,
            actual =
                metrics.nextStage
        )

        assertEquals(
            expected = 0.8,
            actual =
                metrics.difficultyDelta,
            absoluteTolerance =
                0.000_000_1
        )

        assertEquals(
            expected = -1.0,
            actual =
                metrics.stabilityDeltaDays,
            absoluteTolerance =
                0.000_000_1
        )

        assertEquals(
            expected = 1,
            actual =
                metrics.reviewCountIncrement
        )

        assertEquals(
            expected = 1,
            actual =
                metrics.lapseCountIncrement
        )

        assertTrue(
            metrics.stageChanged
        )

        assertTrue(
            metrics.difficultyIncreased
        )

        assertFalse(
            metrics.difficultyDecreased
        )

        assertFalse(
            metrics.stabilityIncreased
        )

        assertTrue(
            metrics.stabilityDecreased
        )

        assertTrue(
            metrics.lapseOccurred
        )
    }

    @Test
    fun `reports unchanged stage and no lapse`() {
        val reviewedAt =
            Moment(
                2_000_000L
            )

        val interval =
            TimeSpan.days(
                3
            )

        val previousState =
            MemoryState(
                learnerId = learnerId,
                learningItemId = learningItemId,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 3.0,
                dueAt = reviewedAt,
                lastReviewedAt =
                    Moment(
                        1_000_000L
                    ),
                reviewCount = 2,
                lapseCount = 0
            )

        val nextState =
            previousState.copy(
                difficulty =
                    4.7,
                stabilityDays =
                    6.0,
                dueAt =
                    reviewedAt + interval,
                lastReviewedAt =
                    reviewedAt,
                reviewCount =
                    3
            )

        val metrics =
            calculator.calculate(
                SchedulerDecision(
                    previousState =
                        previousState,
                    nextState =
                        nextState,
                    scheduledInterval =
                        interval
                )
            )

        assertFalse(
            metrics.stageChanged
        )

        assertFalse(
            metrics.difficultyIncreased
        )

        assertTrue(
            metrics.difficultyDecreased
        )

        assertTrue(
            metrics.stabilityIncreased
        )

        assertFalse(
            metrics.stabilityDecreased
        )

        assertFalse(
            metrics.lapseOccurred
        )
    }
}