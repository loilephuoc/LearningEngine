package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardForecastCalculatorTest {

    private val calculator =
        LearningDashboardForecastCalculator()

    private val learnerId =
        LearnerId("learner-1")

    @Test
    fun `calculate returns empty forecast when no windows are requested`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(2_000L)
                        )
                    ),
                forecastStart = Moment(1_000L),
                windowEnds = emptyList()
            )

        assertEquals(
            expected = LearningDashboardForecast.EMPTY,
            actual = result
        )
    }

    @Test
    fun `calculate creates contiguous forecast buckets`() {
        val result =
            calculator.calculate(
                memoryStates = emptyList(),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L),
                        Moment(3_000L),
                        Moment(4_000L)
                    )
            )

        assertEquals(
            expected = 3,
            actual = result.buckets.size
        )

        assertEquals(
            expected = Moment(1_000L),
            actual = result.buckets[0].windowStart
        )

        assertEquals(
            expected = Moment(2_000L),
            actual = result.buckets[0].windowEnd
        )

        assertEquals(
            expected = Moment(2_000L),
            actual = result.buckets[1].windowStart
        )

        assertEquals(
            expected = Moment(3_000L),
            actual = result.buckets[1].windowEnd
        )

        assertEquals(
            expected = Moment(3_000L),
            actual = result.buckets[2].windowStart
        )

        assertEquals(
            expected = Moment(4_000L),
            actual = result.buckets[2].windowEnd
        )
    }

    @Test
    fun `calculate distributes memories into matching buckets`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(1_500L)
                        ),
                        memory(
                            itemNumber = 2,
                            dueAt = Moment(2_000L)
                        ),
                        memory(
                            itemNumber = 3,
                            dueAt = Moment(2_001L)
                        ),
                        memory(
                            itemNumber = 4,
                            dueAt = Moment(3_000L)
                        ),
                        memory(
                            itemNumber = 5,
                            dueAt = Moment(3_500L)
                        )
                    ),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L),
                        Moment(3_000L),
                        Moment(4_000L)
                    )
            )

        assertEquals(
            expected = 2,
            actual = result.buckets[0].dueCount
        )

        assertEquals(
            expected = 2,
            actual = result.buckets[1].dueCount
        )

        assertEquals(
            expected = 1,
            actual = result.buckets[2].dueCount
        )

        assertEquals(
            expected = 5,
            actual = result.totalDueCount
        )
    }

    @Test
    fun `memory due exactly at forecast start is excluded`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(1_000L)
                        ),
                        memory(
                            itemNumber = 2,
                            dueAt = Moment(1_001L)
                        )
                    ),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L)
                    )
            )

        assertEquals(
            expected = 1,
            actual = result.totalDueCount
        )
    }

    @Test
    fun `overdue memory before forecast start is excluded`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(500L)
                        )
                    ),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L)
                    )
            )

        assertEquals(
            expected = 0,
            actual = result.totalDueCount
        )
    }

    @Test
    fun `memory after final forecast window is excluded`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(3_001L)
                        )
                    ),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L),
                        Moment(3_000L)
                    )
            )

        assertEquals(
            expected = 0,
            actual = result.totalDueCount
        )
    }

    @Test
    fun `suspended memory is excluded from forecast`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(1_500L),
                            stage = LearningStage.SUSPENDED
                        ),
                        memory(
                            itemNumber = 2,
                            dueAt = Moment(1_500L),
                            stage = LearningStage.REVIEW
                        )
                    ),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L)
                    )
            )

        assertEquals(
            expected = 1,
            actual = result.totalDueCount
        )
    }

    @Test
    fun `calculate accepts every active learning stage`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            itemNumber = 1,
                            dueAt = Moment(1_500L),
                            stage = LearningStage.NEW
                        ),
                        memory(
                            itemNumber = 2,
                            dueAt = Moment(1_500L),
                            stage = LearningStage.LEARNING
                        ),
                        memory(
                            itemNumber = 3,
                            dueAt = Moment(1_500L),
                            stage = LearningStage.REVIEW
                        ),
                        memory(
                            itemNumber = 4,
                            dueAt = Moment(1_500L),
                            stage = LearningStage.RELEARNING
                        ),
                        memory(
                            itemNumber = 5,
                            dueAt = Moment(1_500L),
                            stage = LearningStage.MASTERED
                        )
                    ),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L)
                    )
            )

        assertEquals(
            expected = 5,
            actual = result.totalDueCount
        )
    }

    @Test
    fun `first window end must be after forecast start`() {
        assertFailsWith<IllegalArgumentException> {
            calculator.calculate(
                memoryStates = emptyList(),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(1_000L)
                    )
            )
        }
    }

    @Test
    fun `window ends must be strictly increasing`() {
        assertFailsWith<IllegalArgumentException> {
            calculator.calculate(
                memoryStates = emptyList(),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(3_000L),
                        Moment(2_000L)
                    )
            )
        }
    }

    @Test
    fun `duplicate window ends are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            calculator.calculate(
                memoryStates = emptyList(),
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L),
                        Moment(2_000L)
                    )
            )
        }
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