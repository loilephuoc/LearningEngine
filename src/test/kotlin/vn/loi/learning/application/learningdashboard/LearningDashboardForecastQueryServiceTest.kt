package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardForecastQueryServiceTest {

    private val learnerId =
        LearnerId("learner-1")

    private val memoryStateQuery =
        InMemoryMemoryStateQuery()

    private val service =
        LearningDashboardForecastQueryService(
            memoryStateQuery =
                memoryStateQuery,
            forecastCalculator =
                LearningDashboardForecastCalculator()
        )

    @Test
    fun `query returns empty forecast snapshot when no windows are requested`() {
        val result =
            service.query(
                learnerId = learnerId,
                forecastStart = Moment(1_000L),
                windowEnds = emptyList()
            )

        assertEquals(
            expected =
                LearningDashboardForecastSnapshot.EMPTY,
            actual = result
        )
    }

    @Test
    fun `query calculates ordered forecast buckets from learner memories`() {
        memoryStateQuery.memoryStates =
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
                    dueAt = Moment(2_500L)
                ),
                memory(
                    itemNumber = 4,
                    dueAt = Moment(3_000L)
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L),
                        Moment(3_000L)
                    )
            )

        assertEquals(
            expected = 2,
            actual = result.forecast.buckets.size
        )

        assertEquals(
            expected = 2,
            actual = result.forecast.buckets[0].dueCount
        )

        assertEquals(
            expected = 2,
            actual = result.forecast.buckets[1].dueCount
        )

        assertEquals(
            expected = 4,
            actual = result.forecast.totalDueCount
        )
    }

    @Test
    fun `query excludes memories due at or before forecast start`() {
        memoryStateQuery.memoryStates =
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
            )

        val result =
            service.query(
                learnerId = learnerId,
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L)
                    )
            )

        assertEquals(
            expected = 1,
            actual = result.forecast.totalDueCount
        )
    }

    @Test
    fun `query excludes suspended memories through forecast calculator`() {
        memoryStateQuery.memoryStates =
            listOf(
                memory(
                    itemNumber = 1,
                    dueAt = Moment(1_500L),
                    stage = LearningStage.REVIEW
                ),
                memory(
                    itemNumber = 2,
                    dueAt = Moment(1_500L),
                    stage = LearningStage.SUSPENDED
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L)
                    )
            )

        assertEquals(
            expected = 1,
            actual = result.forecast.totalDueCount
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
                    dueAt = Moment(1_500L)
                ),
                memory(
                    learnerId = anotherLearner,
                    itemNumber = 2,
                    dueAt = Moment(1_500L)
                )
            )

        val result =
            service.query(
                learnerId = learnerId,
                forecastStart = Moment(1_000L),
                windowEnds =
                    listOf(
                        Moment(2_000L)
                    )
            )

        assertEquals(
            expected = 1,
            actual = result.forecast.totalDueCount
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