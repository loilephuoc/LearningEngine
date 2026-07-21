package vn.loi.learning.domain.study.scheduling

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

class ValidatingSchedulerTest {

    private val learnerId =
        LearnerId("validating-scheduler-learner")

    private val learningItemId =
        LearningItemId("validating-scheduler-item")

    @Test
    fun `returns exact delegate decision when it is valid`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val interval =
            TimeSpan.days(1)

        val expectedDecision =
            SchedulerDecision(
                previousState = currentState,
                nextState =
                    currentState.copy(
                        stage =
                            LearningStage.REVIEW,
                        difficulty =
                            4.8,
                        stabilityDays =
                            1.0,
                        dueAt =
                            reviewedAt + interval,
                        lastReviewedAt =
                            reviewedAt,
                        reviewCount =
                            1
                    ),
                scheduledInterval = interval
            )

        val scheduler =
            ValidatingScheduler(
                delegate =
                    Scheduler { _, _, _ ->
                        expectedDecision
                    }
            )

        val actualDecision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )

        assertSame(
            expected = expectedDecision,
            actual = actualDecision
        )
    }

    @Test
    fun `forwards scheduler inputs unchanged to delegate`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val rating =
            ReviewRating.EASY

        var capturedState: MemoryState? =
            null

        var capturedRating: ReviewRating? =
            null

        var capturedReviewedAt: Moment? =
            null

        val interval =
            TimeSpan.days(4)

        val scheduler =
            ValidatingScheduler(
                delegate =
                    Scheduler {
                            state,
                            suppliedRating,
                            suppliedReviewedAt ->

                        capturedState =
                            state

                        capturedRating =
                            suppliedRating

                        capturedReviewedAt =
                            suppliedReviewedAt

                        SchedulerDecision(
                            previousState = state,
                            nextState =
                                state.copy(
                                    stage =
                                        LearningStage.REVIEW,
                                    difficulty =
                                        4.5,
                                    stabilityDays =
                                        4.0,
                                    dueAt =
                                        suppliedReviewedAt +
                                                interval,
                                    lastReviewedAt =
                                        suppliedReviewedAt,
                                    reviewCount =
                                        state.reviewCount + 1
                                ),
                            scheduledInterval =
                                interval
                        )
                    }
            )

        scheduler.schedule(
            currentState = currentState,
            rating = rating,
            reviewedAt = reviewedAt
        )

        assertEquals(
            expected = currentState,
            actual = capturedState
        )

        assertEquals(
            expected = rating,
            actual = capturedRating
        )

        assertEquals(
            expected = reviewedAt,
            actual = capturedReviewedAt
        )
    }

    @Test
    fun `rejects invalid decision returned by delegate`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val interval =
            TimeSpan.days(1)

        val scheduler =
            ValidatingScheduler(
                delegate =
                    Scheduler { state, _, suppliedReviewedAt ->
                        SchedulerDecision(
                            previousState = state,
                            nextState =
                                state.copy(
                                    stage =
                                        LearningStage.REVIEW,
                                    difficulty =
                                        4.8,
                                    stabilityDays =
                                        1.0,
                                    dueAt =
                                        suppliedReviewedAt +
                                                TimeSpan.days(2),
                                    lastReviewedAt =
                                        suppliedReviewedAt,
                                    reviewCount =
                                        state.reviewCount + 1
                                ),
                            scheduledInterval =
                                interval
                        )
                    }
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                scheduler.schedule(
                    currentState = currentState,
                    rating = ReviewRating.GOOD,
                    reviewedAt = reviewedAt
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "dueAt"
        )
    }

    @Test
    fun `validates real simple scheduler decisions`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val scheduler =
            ValidatingScheduler(
                delegate =
                    SimpleScheduler()
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )

        assertEquals(
            expected =
                currentState.reviewCount + 1,
            actual =
                decision.nextState.reviewCount
        )

        assertEquals(
            expected =
                reviewedAt +
                        decision.scheduledInterval,
            actual =
                decision.nextState.dueAt
        )
    }

    @Test
    fun `validates real fsrs scheduler decisions`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val scheduler =
            ValidatingScheduler(
                delegate =
                    FsrsScheduler()
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )

        assertEquals(
            expected =
                currentState.reviewCount + 1,
            actual =
                decision.nextState.reviewCount
        )

        assertEquals(
            expected =
                reviewedAt +
                        decision.scheduledInterval,
            actual =
                decision.nextState.dueAt
        )
    }
}