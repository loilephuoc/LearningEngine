package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.ForgettingCurve
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.Retrievability

class LearningDashboardRetentionStatisticsCalculatorTest {

    private val forgettingCurve =
        ForgettingCurve { stability, elapsedTime ->
            Retrievability(
                value =
                    stability.days /
                            (
                                    stability.days +
                                            elapsedTime.toDays()
                                    )
            )
        }

    private val calculator =
        LearningDashboardRetentionStatisticsCalculator(
            forgettingCurve = forgettingCurve
        )

    private val learnerId =
        LearnerId("learner-1")

    @Test
    fun `calculate returns empty statistics for empty memories`() {
        val result =
            calculator.calculate(
                memoryStates = emptyList(),
                at = Moment(1_000L)
            )

        assertEquals(
            expected =
                LearningDashboardRetentionStatistics.EMPTY,
            actual = result
        )
    }

    @Test
    fun `calculate averages retrievability of eligible memories`() {
        val at =
            Moment(172_800_000L)

        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        reviewedMemory(
                            itemNumber = 1,
                            stabilityDays = 2.0,
                            lastReviewedAt = Moment(86_400_000L)
                        ),
                        reviewedMemory(
                            itemNumber = 2,
                            stabilityDays = 3.0,
                            lastReviewedAt = Moment(0L)
                        )
                    ),
                at = at
            )

        assertEquals(
            expected = 2,
            actual = result.evaluatedMemoryCount
        )

        assertEquals(
            expected = 0.6333333333333333,
            actual =
                result.averageRetrievability!!.value,
            absoluteTolerance = 0.0000001
        )
    }

    @Test
    fun `calculate excludes unreviewed memories`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        MemoryState.new(
                            learnerId = learnerId,
                            learningItemId =
                                LearningItemId("item-new"),
                            availableAt = Moment(0L)
                        )
                    ),
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected = 0,
            actual = result.evaluatedMemoryCount
        )

        assertNull(
            actual = result.averageRetrievability
        )
    }

    @Test
    fun `calculate excludes suspended memories`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        reviewedMemory(
                            itemNumber = 1,
                            stabilityDays = 2.0,
                            lastReviewedAt = Moment(0L),
                            stage = LearningStage.SUSPENDED
                        )
                    ),
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected =
                LearningDashboardRetentionStatistics.EMPTY,
            actual = result
        )
    }

    @Test
    fun `calculate excludes memories with zero stability`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        reviewedMemory(
                            itemNumber = 1,
                            stabilityDays = 0.0,
                            lastReviewedAt = Moment(0L)
                        )
                    ),
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected =
                LearningDashboardRetentionStatistics.EMPTY,
            actual = result
        )
    }

    @Test
    fun `calculate excludes memories reviewed after calculation time`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        reviewedMemory(
                            itemNumber = 1,
                            stabilityDays = 2.0,
                            lastReviewedAt = Moment(2_000L)
                        )
                    ),
                at = Moment(1_000L)
            )

        assertEquals(
            expected =
                LearningDashboardRetentionStatistics.EMPTY,
            actual = result
        )
    }

    @Test
    fun `memory reviewed exactly at calculation time has full elapsed zero result`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        reviewedMemory(
                            itemNumber = 1,
                            stabilityDays = 2.0,
                            lastReviewedAt = Moment(1_000L)
                        )
                    ),
                at = Moment(1_000L)
            )

        assertEquals(
            expected = 1,
            actual = result.evaluatedMemoryCount
        )

        assertEquals(
            expected = 1.0,
            actual =
                result.averageRetrievability!!.value,
            absoluteTolerance = 0.0000001
        )
    }

    private fun reviewedMemory(
        itemNumber: Int,
        stabilityDays: Double,
        lastReviewedAt: Moment,
        stage: LearningStage = LearningStage.REVIEW
    ): MemoryState =
        MemoryState(
            learnerId = learnerId,
            learningItemId =
                LearningItemId(
                    "item-$itemNumber"
                ),
            stage = stage,
            difficulty = 5.0,
            stabilityDays = stabilityDays,
            dueAt = lastReviewedAt,
            lastReviewedAt = lastReviewedAt,
            reviewCount = 1,
            lapseCount = 0
        )
}