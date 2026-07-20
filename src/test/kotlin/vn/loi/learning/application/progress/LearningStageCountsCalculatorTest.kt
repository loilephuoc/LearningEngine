package vn.loi.learning.application.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningStageCountsCalculatorTest {

    private val calculator =
        LearningStageCountsCalculator()

    @Test
    fun `calculate returns empty counts for empty memories`() {
        val result =
            calculator.calculate(
                memoryStates = emptyList()
            )

        assertEquals(
            expected = LearningStageCounts.EMPTY,
            actual = result
        )
    }

    @Test
    fun `calculate counts every learning stage`() {
        val learnerId =
            LearnerId("learner-1")

        val memoryStates =
            listOf(
                memory(
                    learnerId = learnerId,
                    itemNumber = 1,
                    stage = LearningStage.NEW
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 2,
                    stage = LearningStage.LEARNING
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 3,
                    stage = LearningStage.LEARNING
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 4,
                    stage = LearningStage.REVIEW
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 5,
                    stage = LearningStage.REVIEW
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 6,
                    stage = LearningStage.REVIEW
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 7,
                    stage = LearningStage.RELEARNING
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 8,
                    stage = LearningStage.MASTERED
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 9,
                    stage = LearningStage.SUSPENDED
                )
            )

        val result =
            calculator.calculate(
                memoryStates = memoryStates
            )

        assertEquals(
            expected = 1,
            actual = result.newCount
        )

        assertEquals(
            expected = 2,
            actual = result.learningCount
        )

        assertEquals(
            expected = 3,
            actual = result.reviewCount
        )

        assertEquals(
            expected = 1,
            actual = result.relearningCount
        )

        assertEquals(
            expected = 1,
            actual = result.masteredCount
        )

        assertEquals(
            expected = 1,
            actual = result.suspendedCount
        )

        assertEquals(
            expected = 9,
            actual = result.totalMemories
        )

        assertEquals(
            expected = 8,
            actual = result.activeMemories
        )
    }

    @Test
    fun `calculate returns zero for stages not present`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            learnerId =
                                LearnerId("learner-1"),
                            itemNumber = 1,
                            stage =
                                LearningStage.REVIEW
                        )
                    )
            )

        assertEquals(
            expected = 0,
            actual = result.newCount
        )

        assertEquals(
            expected = 0,
            actual = result.learningCount
        )

        assertEquals(
            expected = 1,
            actual = result.reviewCount
        )

        assertEquals(
            expected = 0,
            actual = result.relearningCount
        )

        assertEquals(
            expected = 0,
            actual = result.masteredCount
        )

        assertEquals(
            expected = 0,
            actual = result.suspendedCount
        )
    }

    @Test
    fun `calculate counts memories regardless of learner identity`() {
        val result =
            calculator.calculate(
                memoryStates =
                    listOf(
                        memory(
                            learnerId =
                                LearnerId("learner-1"),
                            itemNumber = 1,
                            stage =
                                LearningStage.NEW
                        ),
                        memory(
                            learnerId =
                                LearnerId("learner-2"),
                            itemNumber = 2,
                            stage =
                                LearningStage.NEW
                        )
                    )
            )

        assertEquals(
            expected = 2,
            actual = result.newCount
        )
    }

    private fun memory(
        learnerId: LearnerId,
        itemNumber: Int,
        stage: LearningStage
    ): MemoryState =
        MemoryState.new(
            learnerId = learnerId,
            learningItemId =
                LearningItemId(
                    "item-$itemNumber"
                ),
            availableAt =
                Moment(1_000L)
        ).copy(
            stage = stage
        )
}