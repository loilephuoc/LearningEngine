package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardSchedulingQueryServiceTest {

    private val learnerId =
        LearnerId("learner-1")

    private val memoryStateQuery =
        InMemoryMemoryStateQuery()

    private val service =
        LearningDashboardSchedulingQueryService(
            memoryStateQuery =
                memoryStateQuery,
            dueStatisticsCalculator =
                LearningDashboardDueStatisticsCalculator()
        )

    @Test
    fun `query returns empty scheduling snapshot when learner has no memories`() {
        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(2_000L)
            )

        assertEquals(
            expected =
                LearningDashboardSchedulingSnapshot.EMPTY,
            actual = result
        )
    }

    @Test
    fun `query calculates due and overdue statistics from learner memories`() {
        val evaluatedAt =
            Moment(2_000L)

        memoryStateQuery.memoryStates =
            listOf(
                memory(
                    itemNumber = 1,
                    dueAt = Moment(1_000L)
                ),
                memory(
                    itemNumber = 2,
                    dueAt = Moment(2_000L)
                ),
                memory(
                    itemNumber = 3,
                    dueAt = Moment(3_000L)
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                at = evaluatedAt
            )

        assertEquals(
            expected = 2,
            actual = result.dueStatistics.dueCount
        )

        assertEquals(
            expected = 1,
            actual = result.dueStatistics.overdueCount
        )
    }

    @Test
    fun `query excludes suspended memories through due calculator`() {
        memoryStateQuery.memoryStates =
            listOf(
                memory(
                    itemNumber = 1,
                    dueAt = Moment(1_000L),
                    stage = LearningStage.REVIEW
                ),
                memory(
                    itemNumber = 2,
                    dueAt = Moment(1_000L),
                    stage = LearningStage.SUSPENDED
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(2_000L)
            )

        assertEquals(
            expected = 1,
            actual = result.dueStatistics.dueCount
        )

        assertEquals(
            expected = 1,
            actual = result.dueStatistics.overdueCount
        )
    }

    @Test
    fun `query only uses memories belonging to requested learner`() {
        val anotherLearner =
            LearnerId("learner-2")

        memoryStateQuery.memoryStates =
            listOf(
                memory(
                    learnerId = learnerId,
                    itemNumber = 1,
                    dueAt = Moment(1_000L)
                ),
                memory(
                    learnerId = anotherLearner,
                    itemNumber = 2,
                    dueAt = Moment(1_000L)
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(2_000L)
            )

        assertEquals(
            expected = 1,
            actual = result.dueStatistics.dueCount
        )

        assertEquals(
            expected = 1,
            actual = result.dueStatistics.overdueCount
        )
    }

    private fun memory(
        learnerId: LearnerId = this.learnerId,
        itemNumber: Int,
        dueAt: Moment,
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
            stabilityDays = 10.0,
            dueAt = dueAt,
            lastReviewedAt = Moment(500L),
            reviewCount = 1,
            lapseCount = 0
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