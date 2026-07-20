package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardDueStatisticsCalculatorTest {

    private val calculator =
        LearningDashboardDueStatisticsCalculator()

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
            expected = LearningDashboardDueStatistics.EMPTY,
            actual = result
        )
    }

    @Test
    fun `calculate counts due overdue and future memories`() {
        val at =
            Moment(1_000L)

        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(999L)
                        ),
                        memory(
                            itemNumber = 2,
                            dueAt = Moment(1_000L)
                        ),
                        memory(
                            itemNumber = 3,
                            dueAt = Moment(1_001L)
                        )
                    ),
                at = at
            )

        assertEquals(
            expected = 2,
            actual = result.dueCount
        )

        assertEquals(
            expected = 1,
            actual = result.overdueCount
        )

        assertEquals(
            expected = 1,
            actual = result.dueNowCount
        )
    }

    @Test
    fun `memory due exactly at calculation time is not overdue`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(1_000L)
                        )
                    ),
                at = Moment(1_000L)
            )

        assertEquals(
            expected = 1,
            actual = result.dueCount
        )

        assertEquals(
            expected = 0,
            actual = result.overdueCount
        )

        assertEquals(
            expected = 1,
            actual = result.dueNowCount
        )
    }

    @Test
    fun `suspended memory is excluded even when its due time has passed`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(500L),
                            stage = LearningStage.SUSPENDED
                        ),
                        memory(
                            itemNumber = 2,
                            dueAt = Moment(500L),
                            stage = LearningStage.REVIEW
                        )
                    ),
                at = Moment(1_000L)
            )

        assertEquals(
            expected = 1,
            actual = result.dueCount
        )

        assertEquals(
            expected = 1,
            actual = result.overdueCount
        )
    }

    @Test
    fun `calculate includes due memories from every active learning stage`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(500L),
                            stage = LearningStage.NEW
                        ),
                        memory(
                            itemNumber = 2,
                            dueAt = Moment(500L),
                            stage = LearningStage.LEARNING
                        ),
                        memory(
                            itemNumber = 3,
                            dueAt = Moment(500L),
                            stage = LearningStage.REVIEW
                        ),
                        memory(
                            itemNumber = 4,
                            dueAt = Moment(500L),
                            stage = LearningStage.RELEARNING
                        ),
                        memory(
                            itemNumber = 5,
                            dueAt = Moment(500L),
                            stage = LearningStage.MASTERED
                        )
                    ),
                at = Moment(1_000L)
            )

        assertEquals(
            expected = 5,
            actual = result.dueCount
        )

        assertEquals(
            expected = 5,
            actual = result.overdueCount
        )
    }

    private fun memory(
        itemNumber: Int,
        dueAt: Moment,
        stage: LearningStage = LearningStage.REVIEW
    ): MemoryState =
        MemoryState.new(
            learnerId = learnerId,
            learningItemId =
                LearningItemId(
                    "item-$itemNumber"
                ),
            availableAt = dueAt
        ).copy(
            stage = stage,
            dueAt = dueAt
        )
}