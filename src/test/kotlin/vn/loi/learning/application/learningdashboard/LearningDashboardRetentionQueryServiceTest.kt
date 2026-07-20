package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.SimpleForgettingCurve
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardRetentionQueryServiceTest {

    private val learnerId =
        LearnerId("learner-1")

    private val memoryStateQuery =
        InMemoryMemoryStateQuery()

    private val service =
        LearningDashboardRetentionQueryService(
            memoryStateQuery =
                memoryStateQuery,
            retentionStatisticsCalculator =
                LearningDashboardRetentionStatisticsCalculator(
                    forgettingCurve =
                        SimpleForgettingCurve()
                )
        )

    @Test
    fun `query returns empty retention snapshot when learner has no memories`() {
        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected =
                LearningDashboardRetentionSnapshot.EMPTY,
            actual = result
        )
    }

    @Test
    fun `query calculates retention statistics from learner memories`() {
        memoryStateQuery.memoryStates =
            listOf(
                reviewedMemory(
                    itemNumber = 1,
                    stabilityDays = 1.0,
                    lastReviewedAt = Moment(0L)
                ),
                reviewedMemory(
                    itemNumber = 2,
                    stabilityDays = 2.0,
                    lastReviewedAt = Moment(0L)
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected = 2,
            actual =
                result.statistics.evaluatedMemoryCount
        )

        assertNotNull(
            actual =
                result.statistics.averageRetrievability
        )
    }

    @Test
    fun `query excludes suspended memories through retention calculator`() {
        memoryStateQuery.memoryStates =
            listOf(
                reviewedMemory(
                    itemNumber = 1,
                    stage = LearningStage.REVIEW
                ),
                reviewedMemory(
                    itemNumber = 2,
                    stage = LearningStage.SUSPENDED
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected = 1,
            actual =
                result.statistics.evaluatedMemoryCount
        )
    }

    @Test
    fun `query excludes unreviewed memories through retention calculator`() {
        memoryStateQuery.memoryStates =
            listOf(
                reviewedMemory(
                    itemNumber = 1
                ),
                MemoryState.new(
                    learnerId = learnerId,
                    learningItemId =
                        LearningItemId("item-2"),
                    availableAt =
                        Moment(0L)
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected = 1,
            actual =
                result.statistics.evaluatedMemoryCount
        )
    }

    @Test
    fun `query only uses memories belonging to requested learner`() {
        val anotherLearner =
            LearnerId("learner-2")

        memoryStateQuery.memoryStates =
            listOf(
                reviewedMemory(
                    learnerId = learnerId,
                    itemNumber = 1
                ),
                reviewedMemory(
                    learnerId = anotherLearner,
                    itemNumber = 2
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                at = Moment(86_400_000L)
            )

        assertEquals(
            expected = 1,
            actual =
                result.statistics.evaluatedMemoryCount
        )
    }

    private fun reviewedMemory(
        learnerId: LearnerId = this.learnerId,
        itemNumber: Int,
        stage: LearningStage = LearningStage.REVIEW,
        stabilityDays: Double = 1.0,
        lastReviewedAt: Moment = Moment(0L)
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
            dueAt = Moment(86_400_000L),
            lastReviewedAt = lastReviewedAt,
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