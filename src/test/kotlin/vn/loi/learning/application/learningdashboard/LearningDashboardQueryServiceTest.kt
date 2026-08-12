package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.SimpleForgettingCurve
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningDashboardQueryServiceFactory

class LearningDashboardQueryServiceTest {

    @Test
    fun `home reads one memory snapshot and remains structurally equivalent to section queries`() {
        val state = reviewedMemory(1, Moment(1_500L), Moment(1_200L), LearningStage.REVIEW)
        memoryStateQuery.memoryStates = listOf(state)
        val query = LearningDashboardQuery(
            learnerId = learnerId,
            activityFrom = Moment(1_000L),
            activityUntil = Moment(2_000L),
            at = Moment(2_000L),
            forecastWindowEnds = emptyList()
        )

        val result = service.queryHome(query)

        assertEquals(1, memoryStateQuery.findAllCalls)
        assertEquals(1, result.memory.stageCounts.reviewCount)
        assertEquals(1, result.scheduling.dueStatistics.dueCount)
        assertEquals(1, result.retention.statistics.evaluatedMemoryCount)
        assertTrue(result.retention.statistics.averageRetrievability != null)
    }

    private val learnerId =
        LearnerId("learner-1")

    private val memoryStateQuery =
        InMemoryMemoryStateQuery()

    private val reviewEventRepository =
        InMemoryReviewEventRepository()

    private val service =
        LearningDashboardQueryServiceFactory.create(
            memoryStateQuery =
                memoryStateQuery,
            reviewEventRepository =
                reviewEventRepository,
            forgettingCurve =
                SimpleForgettingCurve()
        )

    @Test
    fun `query composes empty dashboard when learner has no data`() {
        val result =
            service.query(
                LearningDashboardQuery(
                    learnerId =
                        learnerId,
                    activityFrom =
                        Moment(1_000L),
                    activityUntil =
                        Moment(2_000L),
                    at =
                        Moment(2_000L),
                    forecastWindowEnds =
                        emptyList()
                )
            )

        assertEquals(
            expected =
                LearningDashboardSnapshot.EMPTY,
            actual = result
        )
    }

    @Test
    fun `query composes all dashboard sections from learner data`() {
        val reviewedMemory =
            reviewedMemory(
                itemNumber = 1,
                dueAt = Moment(1_500L),
                lastReviewedAt = Moment(1_200L),
                stage = LearningStage.REVIEW
            )

        val overdueMemory =
            reviewedMemory(
                itemNumber = 2,
                dueAt = Moment(1_000L),
                lastReviewedAt = Moment(900L),
                stage = LearningStage.RELEARNING
            )

        val futureMemory =
            reviewedMemory(
                itemNumber = 3,
                dueAt = Moment(2_500L),
                lastReviewedAt = Moment(1_100L),
                stage = LearningStage.MASTERED
            )

        val newMemory =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId =
                    LearningItemId("item-4"),
                availableAt =
                    Moment(2_000L)
            )

        val suspendedMemory =
            reviewedMemory(
                itemNumber = 5,
                dueAt = Moment(1_000L),
                lastReviewedAt = Moment(800L),
                stage = LearningStage.SUSPENDED
            )

        memoryStateQuery.memoryStates =
            listOf(
                reviewedMemory,
                overdueMemory,
                futureMemory,
                newMemory,
                suspendedMemory
            )

        reviewEventRepository.events =
            listOf(
                reviewEvent(
                    eventNumber = 1,
                    rating = ReviewRating.GOOD,
                    reviewedAt = Moment(1_200L)
                ),
                reviewEvent(
                    eventNumber = 2,
                    rating = ReviewRating.AGAIN,
                    reviewedAt = Moment(1_400L)
                )
            )

        val result =
            service.query(
                LearningDashboardQuery(
                    learnerId =
                        learnerId,
                    activityFrom =
                        Moment(1_000L),
                    activityUntil =
                        Moment(2_000L),
                    at =
                        Moment(2_000L),
                    forecastWindowEnds =
                        listOf(
                            Moment(3_000L)
                        )
                )
            )

        assertEquals(
            expected = 2,
            actual =
                result.activity.progress.totalReviews
        )

        assertEquals(
            expected = 1,
            actual =
                result.activity.progress.againCount
        )

        assertEquals(
            expected = 1,
            actual =
                result.activity.progress.goodCount
        )

        assertEquals(
            expected = 1,
            actual =
                result.memory.stageCounts.newCount
        )

        assertEquals(
            expected = 1,
            actual =
                result.memory.stageCounts.reviewCount
        )

        assertEquals(
            expected = 1,
            actual =
                result.memory.stageCounts.relearningCount
        )

        assertEquals(
            expected = 1,
            actual =
                result.memory.stageCounts.masteredCount
        )

        assertEquals(
            expected = 1,
            actual =
                result.memory.stageCounts.suspendedCount
        )

        assertEquals(
            expected = 3,
            actual =
                result.scheduling.dueStatistics.dueCount
        )

        assertEquals(
            expected = 2,
            actual =
                result.scheduling.dueStatistics.overdueCount
        )

        assertEquals(
            expected = 3,
            actual =
                result.retention.statistics.evaluatedMemoryCount
        )

        assertNotNull(
            actual =
                result.retention.statistics.averageRetrievability
        )

        assertEquals(
            expected = 1,
            actual =
                result.forecast.forecast.totalDueCount
        )

        assertEquals(
            expected = 1,
            actual =
                result.forecast.forecast.buckets.size
        )
    }

    private fun reviewEvent(
        eventNumber: Int,
        rating: ReviewRating,
        reviewedAt: Moment
    ): ReviewEvent {
        val itemId =
            LearningItemId(
                "review-item-$eventNumber"
            )

        val stateBefore =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = itemId,
                availableAt =
                    Moment(1_000L)
            )

        val stateAfter =
            MemoryState(
                learnerId = learnerId,
                learningItemId = itemId,
                stage = LearningStage.LEARNING,
                difficulty = 5.0,
                stabilityDays = 1.0,
                dueAt =
                    Moment(
                        reviewedAt.epochMillis +
                                1_000L
                    ),
                lastReviewedAt =
                    reviewedAt,
                reviewCount = 1,
                lapseCount =
                    if (rating == ReviewRating.AGAIN) {
                        1
                    } else {
                        0
                    }
            )

        return ReviewEvent(
            id =
                ReviewEventId(
                    "review-event-$eventNumber"
                ),
            rating =
                rating,
            reviewedAt =
                reviewedAt,
            responseTime =
                null,
            stateBefore =
                stateBefore,
            stateAfter =
                stateAfter
        )
    }

    private fun reviewedMemory(
        itemNumber: Int,
        dueAt: Moment,
        lastReviewedAt: Moment,
        stage: LearningStage
    ): MemoryState =
        MemoryState(
            learnerId = learnerId,
            learningItemId =
                LearningItemId(
                    "item-$itemNumber"
                ),
            stage =
                stage,
            difficulty =
                5.0,
            stabilityDays =
                1.0,
            dueAt =
                dueAt,
            lastReviewedAt =
                lastReviewedAt,
            reviewCount =
                1,
            lapseCount =
                if (stage == LearningStage.RELEARNING) {
                    1
                } else {
                    0
                }
        )

    private class InMemoryMemoryStateQuery :
        MemoryStateQuery {

        var memoryStates: List<MemoryState> =
            emptyList()

        var findAllCalls: Int = 0

        override fun findAll(
            learnerId: LearnerId
        ): List<MemoryState> {
            findAllCalls += 1
            return memoryStates.filter { memoryState ->
                memoryState.learnerId == learnerId
            }
        }
    }

    private class InMemoryReviewEventRepository :
        ReviewEventRepository {

        var events: List<ReviewEvent> =
            emptyList()

        override fun append(
            event: ReviewEvent
        ) {
            events =
                events + event
        }

        override fun findAll(
            learnerId: LearnerId
        ): List<ReviewEvent> =
            events.filter { event ->
                event.learnerId == learnerId
            }

        override fun findAll(
            learnerId: LearnerId,
            learningItemId: LearningItemId
        ): List<ReviewEvent> =
            events.filter { event ->
                event.learnerId == learnerId &&
                        event.learningItemId ==
                        learningItemId
            }
    }
}
