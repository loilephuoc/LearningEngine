package vn.loi.learning.application.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.domain.study.scheduling.Scheduler
import vn.loi.learning.domain.study.scheduling.SchedulerDecision
import vn.loi.learning.domain.study.scheduling.ValidatingScheduler
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository

class ReviewLearningItemSchedulingIntegrationTest {

    private val learnerId =
        LearnerId(
            "review-scheduling-learner"
        )

    private val learningItemId =
        LearningItemId(
            "review-scheduling-item"
        )

    @Test
    fun `review flows through validated fsrs scheduling and persistence`() {
        val memoryStateRepository =
            InMemoryMemoryStateRepository()

        val reviewEventRepository =
            InMemoryReviewEventRepository()

        val useCase =
            ReviewLearningItemUseCase(
                memoryStateRepository =
                    memoryStateRepository,
                reviewEventRepository =
                    reviewEventRepository,
                scheduler =
                    ValidatingScheduler(
                        delegate =
                            FsrsScheduler()
                    )
            )

        val reviewedAt =
            Moment(
                1_000_000L
            )

        val result =
            useCase.execute(
                ReviewCommand(
                    reviewEventId =
                        ReviewEventId(
                            "review-event-1"
                        ),
                    learnerId =
                        learnerId,
                    learningItemId =
                        learningItemId,
                    rating =
                        ReviewRating.GOOD,
                    reviewedAt =
                        reviewedAt,
                    responseTime =
                        TimeSpan(
                            2_000L
                        )
                )
            )

        val persistedState =
            assertNotNull(
                memoryStateRepository.find(
                    learnerId =
                        learnerId,
                    learningItemId =
                        learningItemId
                )
            )

        val persistedEvents =
            reviewEventRepository.findAll(
                learnerId =
                    learnerId,
                learningItemId =
                    learningItemId
            )

        assertEquals(
            expected = 1,
            actual =
                memoryStateRepository.count()
        )

        assertEquals(
            expected = 1,
            actual =
                reviewEventRepository.count()
        )

        assertEquals(
            expected = 1,
            actual =
                persistedEvents.size
        )

        assertEquals(
            expected =
                result.memoryState,
            actual =
                persistedState
        )

        assertEquals(
            expected =
                result.reviewEvent,
            actual =
                persistedEvents.single()
        )

        assertEquals(
            expected =
                learnerId,
            actual =
                result.memoryState.learnerId
        )

        assertEquals(
            expected =
                learningItemId,
            actual =
                result.memoryState.learningItemId
        )

        assertEquals(
            expected = 1,
            actual =
                result.memoryState.reviewCount
        )

        assertEquals(
            expected =
                reviewedAt,
            actual =
                result.memoryState.lastReviewedAt
        )

        assertEquals(
            expected =
                result.memoryState,
            actual =
                result.reviewEvent.stateAfter
        )

        assertEquals(
            expected =
                result.reviewEvent.stateBefore
                    .reviewCount + 1,
            actual =
                result.reviewEvent.stateAfter
                    .reviewCount
        )

        assertEquals(
            expected =
                reviewedAt +
                        result.scheduledInterval,
            actual =
                result.memoryState.dueAt
        )

        assertTrue(
            result.scheduledInterval >
                    TimeSpan.ZERO
        )
    }

    @Test
    fun `successive reviews continue from persisted memory state`() {
        val memoryStateRepository =
            InMemoryMemoryStateRepository()

        val reviewEventRepository =
            InMemoryReviewEventRepository()

        val useCase =
            ReviewLearningItemUseCase(
                memoryStateRepository =
                    memoryStateRepository,
                reviewEventRepository =
                    reviewEventRepository,
                scheduler =
                    ValidatingScheduler(
                        delegate =
                            FsrsScheduler()
                    )
            )

        val firstReviewedAt =
            Moment(
                2_000_000L
            )

        val firstResult =
            useCase.execute(
                ReviewCommand(
                    reviewEventId =
                        ReviewEventId(
                            "review-event-1"
                        ),
                    learnerId =
                        learnerId,
                    learningItemId =
                        learningItemId,
                    rating =
                        ReviewRating.GOOD,
                    reviewedAt =
                        firstReviewedAt
                )
            )

        val secondReviewedAt =
            firstResult.memoryState.dueAt

        val secondResult =
            useCase.execute(
                ReviewCommand(
                    reviewEventId =
                        ReviewEventId(
                            "review-event-2"
                        ),
                    learnerId =
                        learnerId,
                    learningItemId =
                        learningItemId,
                    rating =
                        ReviewRating.AGAIN,
                    reviewedAt =
                        secondReviewedAt
                )
            )

        val persistedState =
            assertNotNull(
                memoryStateRepository.find(
                    learnerId =
                        learnerId,
                    learningItemId =
                        learningItemId
                )
            )

        val persistedEvents =
            reviewEventRepository.findAll(
                learnerId =
                    learnerId,
                learningItemId =
                    learningItemId
            )

        assertEquals(
            expected = 1,
            actual =
                memoryStateRepository.count()
        )

        assertEquals(
            expected = 2,
            actual =
                reviewEventRepository.count()
        )

        assertEquals(
            expected = 2,
            actual =
                persistedEvents.size
        )

        assertEquals(
            expected =
                firstResult.memoryState,
            actual =
                secondResult.reviewEvent
                    .stateBefore
        )

        assertEquals(
            expected =
                secondResult.memoryState,
            actual =
                persistedState
        )

        assertEquals(
            expected = 2,
            actual =
                persistedState.reviewCount
        )

        assertEquals(
            expected = 1,
            actual =
                persistedState.lapseCount
        )

        assertEquals(
            expected =
                ReviewEventId(
                    "review-event-1"
                ),
            actual =
                persistedEvents[0].id
        )

        assertEquals(
            expected =
                ReviewEventId(
                    "review-event-2"
                ),
            actual =
                persistedEvents[1].id
        )

        assertEquals(
            expected =
                secondReviewedAt,
            actual =
                persistedEvents[1]
                    .reviewedAt
        )
    }

    @Test
    fun `invalid scheduler decision is rejected before persistence`() {
        val memoryStateRepository =
            InMemoryMemoryStateRepository()

        val reviewEventRepository =
            InMemoryReviewEventRepository()

        val invalidDelegate =
            object : Scheduler {

                override fun schedule(
                    currentState:
                    MemoryState,
                    rating:
                    ReviewRating,
                    reviewedAt:
                    Moment
                ): SchedulerDecision {
                    val interval =
                        TimeSpan.days(
                            1
                        )

                    val invalidNextState =
                        currentState.copy(
                            dueAt =
                                reviewedAt,
                            lastReviewedAt =
                                reviewedAt,
                            reviewCount =
                                currentState
                                    .reviewCount + 1
                        )

                    return SchedulerDecision(
                        previousState =
                            currentState,
                        nextState =
                            invalidNextState,
                        scheduledInterval =
                            interval
                    )
                }
            }

        val useCase =
            ReviewLearningItemUseCase(
                memoryStateRepository =
                    memoryStateRepository,
                reviewEventRepository =
                    reviewEventRepository,
                scheduler =
                    ValidatingScheduler(
                        delegate =
                            invalidDelegate
                    )
            )

        assertFailsWith<
                IllegalStateException
                > {
            useCase.execute(
                ReviewCommand(
                    reviewEventId =
                        ReviewEventId(
                            "invalid-review-event"
                        ),
                    learnerId =
                        learnerId,
                    learningItemId =
                        learningItemId,
                    rating =
                        ReviewRating.GOOD,
                    reviewedAt =
                        Moment(
                            3_000_000L
                        )
                )
            )
        }

        assertNull(
            memoryStateRepository.find(
                learnerId =
                    learnerId,
                learningItemId =
                    learningItemId
            )
        )

        assertTrue(
            reviewEventRepository
                .findAll(
                    learnerId =
                        learnerId,
                    learningItemId =
                        learningItemId
                )
                .isEmpty()
        )

        assertEquals(
            expected = 0,
            actual =
                memoryStateRepository.count()
        )

        assertEquals(
            expected = 0,
            actual =
                reviewEventRepository.count()
        )
    }

    @Test
    fun `validated scheduler returns exact fsrs decision`() {
        val currentState =
            MemoryState.new(
                learnerId =
                    learnerId,
                learningItemId =
                    learningItemId,
                availableAt =
                    Moment(
                        4_000_000L
                    )
            )

        val reviewedAt =
            Moment(
                4_000_000L
            )

        val delegate =
            FsrsScheduler()

        val directDecision =
            delegate.schedule(
                currentState =
                    currentState,
                rating =
                    ReviewRating.EASY,
                reviewedAt =
                    reviewedAt
            )

        var capturedDecision:
                SchedulerDecision? =
            null

        val capturingDelegate =
            object : Scheduler {

                override fun schedule(
                    currentState:
                    MemoryState,
                    rating:
                    ReviewRating,
                    reviewedAt:
                    Moment
                ): SchedulerDecision {
                    assertSame(
                        expected =
                            currentState,
                        actual =
                            currentState
                    )

                    return directDecision
                        .also {
                            capturedDecision =
                                it
                        }
                }
            }

        val validatingScheduler =
            ValidatingScheduler(
                delegate =
                    capturingDelegate
            )

        val validatedDecision =
            validatingScheduler.schedule(
                currentState =
                    currentState,
                rating =
                    ReviewRating.EASY,
                reviewedAt =
                    reviewedAt
            )

        assertSame(
            expected =
                capturedDecision,
            actual =
                validatedDecision
        )

        assertSame(
            expected =
                directDecision,
            actual =
                validatedDecision
        )
    }
}