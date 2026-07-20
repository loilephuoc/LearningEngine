package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.progress.LearningStageCounts
import vn.loi.learning.application.progress.LearningStageCountsCalculator
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardMemoryQueryServiceTest {

    private val learnerId =
        LearnerId("learner-1")

    private val memoryStateQuery =
        InMemoryMemoryStateQuery()

    private val service =
        LearningDashboardMemoryQueryService(
            memoryStateQuery =
                memoryStateQuery,
            learningStageCountsCalculator =
                LearningStageCountsCalculator()
        )

    @Test
    fun `query returns empty memory snapshot when learner has no memories`() {
        val result =
            service.query(
                learnerId = learnerId
            )

        assertEquals(
            expected =
                LearningDashboardMemorySnapshot.EMPTY,
            actual = result
        )
    }

    @Test
    fun `query calculates stage counts from learner memories`() {
        memoryStateQuery.memoryStates =
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
                    stage = LearningStage.REVIEW
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 4,
                    stage = LearningStage.REVIEW
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 5,
                    stage = LearningStage.RELEARNING
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 6,
                    stage = LearningStage.MASTERED
                ),
                memory(
                    learnerId = learnerId,
                    itemNumber = 7,
                    stage = LearningStage.SUSPENDED
                )
            )

        val result =
            service.query(
                learnerId = learnerId
            )

        assertEquals(
            expected =
                LearningStageCounts(
                    newCount = 1,
                    learningCount = 1,
                    reviewCount = 2,
                    relearningCount = 1,
                    masteredCount = 1,
                    suspendedCount = 1
                ),
            actual = result.stageCounts
        )
    }

    @Test
    fun `query requests memories for the required learner`() {
        val anotherLearner =
            LearnerId("learner-2")

        memoryStateQuery.memoryStates =
            listOf(
                memory(
                    learnerId = learnerId,
                    itemNumber = 1,
                    stage = LearningStage.REVIEW
                ),
                memory(
                    learnerId = anotherLearner,
                    itemNumber = 2,
                    stage = LearningStage.SUSPENDED
                )
            )

        val result =
            service.query(
                learnerId = learnerId
            )

        assertEquals(
            expected =
                LearningStageCounts(
                    newCount = 0,
                    learningCount = 0,
                    reviewCount = 1,
                    relearningCount = 0,
                    masteredCount = 0,
                    suspendedCount = 0
                ),
            actual = result.stageCounts
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

    private class InMemoryMemoryStateQuery :
        MemoryStateQuery {

        var memoryStates: List<MemoryState> =
            emptyList()

        override fun findAll(
            learnerId: LearnerId
        ): List<MemoryState> =
            memoryStates.filter { memoryState ->
                memoryState.learnerId == learnerId
            }
    }
}