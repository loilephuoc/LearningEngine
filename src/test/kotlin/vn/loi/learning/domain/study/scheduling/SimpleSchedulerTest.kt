package vn.loi.learning.domain.study.scheduling

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import kotlin.test.assertFailsWith

class SimpleSchedulerTest {

    private val scheduler: Scheduler = SimpleScheduler()

    @Test
    fun `good rating moves new item to review with one day interval`() {
        val currentState =
            MemoryState.new(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                availableAt = AVAILABLE_AT
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.GOOD,
                reviewedAt = REVIEWED_AT
            )

        val expectedInterval = TimeSpan.days(1)

        assertEquals(
            currentState,
            decision.previousState
        )

        assertEquals(
            LearningStage.REVIEW,
            decision.nextState.stage
        )

        assertEquals(
            4.8,
            decision.nextState.difficultyValue.value
        )

        assertEquals(
            1.0,
            decision.nextState.stability.days
        )

        assertEquals(
            expectedInterval,
            decision.scheduledInterval
        )

        assertEquals(
            REVIEWED_AT + expectedInterval,
            decision.nextState.dueAt
        )

        assertEquals(
            REVIEWED_AT,
            decision.nextState.lastReviewedAt
        )

        assertEquals(
            1,
            decision.nextState.reviewCount
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )
    }


    @Test
    fun `again rating moves new item to learning without counting a lapse`() {
        val currentState =
            MemoryState.new(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                availableAt = AVAILABLE_AT
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.AGAIN,
                reviewedAt = REVIEWED_AT
            )

        val expectedInterval = TimeSpan.minutes(10)

        assertEquals(
            currentState,
            decision.previousState
        )

        assertEquals(
            LearningStage.LEARNING,
            decision.nextState.stage
        )

        assertEquals(
            5.8,
            decision.nextState.difficultyValue.value
        )

        assertEquals(
            0.1,
            decision.nextState.stability.days
        )

        assertEquals(
            expectedInterval,
            decision.scheduledInterval
        )

        assertEquals(
            REVIEWED_AT + expectedInterval,
            decision.nextState.dueAt
        )

        assertEquals(
            REVIEWED_AT,
            decision.nextState.lastReviewedAt
        )

        assertEquals(
            1,
            decision.nextState.reviewCount
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )
    }

    @Test
    fun `again rating moves review item to relearning and counts a lapse`() {
        val currentState =
            MemoryState(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 6.0,
                dueAt = REVIEWED_AT,
                lastReviewedAt = PREVIOUS_REVIEWED_AT,
                reviewCount = 3,
                lapseCount = 1
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.AGAIN,
                reviewedAt = REVIEWED_AT
            )

        val expectedInterval = TimeSpan.minutes(10)

        assertEquals(
            currentState,
            decision.previousState
        )

        assertEquals(
            LearningStage.RELEARNING,
            decision.nextState.stage
        )

        assertEquals(
            5.8,
            decision.nextState.difficultyValue.value
        )

        assertEquals(
            3.0,
            decision.nextState.stability.days
        )

        assertEquals(
            expectedInterval,
            decision.scheduledInterval
        )

        assertEquals(
            REVIEWED_AT + expectedInterval,
            decision.nextState.dueAt
        )

        assertEquals(
            REVIEWED_AT,
            decision.nextState.lastReviewedAt
        )

        assertEquals(
            4,
            decision.nextState.reviewCount
        )

        assertEquals(
            2,
            decision.nextState.lapseCount
        )
    }


    @Test
    fun `scheduler rejects review time earlier than the last review time`() {
        val currentState =
            MemoryState(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 5.0,
                dueAt = REVIEWED_AT,
                lastReviewedAt = REVIEWED_AT,
                reviewCount = 1,
                lapseCount = 0
            )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                scheduler.schedule(
                    currentState = currentState,
                    rating = ReviewRating.GOOD,
                    reviewedAt = EARLIER_REVIEWED_AT
                )
            }

        assertEquals(
            "Review time must not be earlier than the last review time.",
            exception.message
        )
    }


    @Test
    fun `again rating keeps learning item in learning without counting a lapse`() {
        val currentState =
            MemoryState(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                stage = LearningStage.LEARNING,
                difficulty = 5.8,
                stabilityDays = 0.1,
                dueAt = REVIEWED_AT,
                lastReviewedAt = PREVIOUS_REVIEWED_AT,
                reviewCount = 1,
                lapseCount = 0
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.AGAIN,
                reviewedAt = REVIEWED_AT
            )

        assertEquals(
            LearningStage.LEARNING,
            decision.nextState.stage
        )

        assertEquals(
            0,
            decision.nextState.lapseCount
        )
    }

    @Test
    fun `hard rating keeps review item in review`() {

        val currentState =
            MemoryState(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 5.0,
                dueAt = REVIEWED_AT,
                lastReviewedAt = PREVIOUS_REVIEWED_AT,
                reviewCount = 4,
                lapseCount = 1
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.HARD,
                reviewedAt = REVIEWED_AT
            )

        val expectedInterval = TimeSpan.days(6)

        assertEquals(
            LearningStage.REVIEW,
            decision.nextState.stage
        )

        assertEquals(
            5.3,
            decision.nextState.difficultyValue.value
        )

        assertEquals(
            6.0,
            decision.nextState.stability.days
        )

        assertEquals(
            expectedInterval,
            decision.scheduledInterval
        )

        assertEquals(
            REVIEWED_AT + expectedInterval,
            decision.nextState.dueAt
        )

        assertEquals(
            5,
            decision.nextState.reviewCount
        )

        assertEquals(
            1,
            decision.nextState.lapseCount
        )
    }


    @Test
    fun `good rating keeps review item in review`() {

        val currentState =
            MemoryState(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 4.0,
                dueAt = REVIEWED_AT,
                lastReviewedAt = PREVIOUS_REVIEWED_AT,
                reviewCount = 2,
                lapseCount = 1
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.GOOD,
                reviewedAt = REVIEWED_AT
            )

        val expectedInterval = TimeSpan.days(10)

        assertEquals(
            LearningStage.REVIEW,
            decision.nextState.stage
        )

        assertEquals(
            4.8,
            decision.nextState.difficultyValue.value
        )

        assertEquals(
            10.0,
            decision.nextState.stability.days
        )

        assertEquals(
            expectedInterval,
            decision.scheduledInterval
        )

        assertEquals(
            REVIEWED_AT + expectedInterval,
            decision.nextState.dueAt
        )

        assertEquals(
            3,
            decision.nextState.reviewCount
        )

        assertEquals(
            1,
            decision.nextState.lapseCount
        )
    }


    @Test
    fun `easy rating keeps review item in review`() {

        val currentState =
            MemoryState(
                learnerId = LEARNER_ID,
                learningItemId = LEARNING_ITEM_ID,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 4.0,
                dueAt = REVIEWED_AT,
                lastReviewedAt = PREVIOUS_REVIEWED_AT,
                reviewCount = 2,
                lapseCount = 1
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.EASY,
                reviewedAt = REVIEWED_AT
            )

        val expectedInterval = TimeSpan.days(14)

        assertEquals(
            LearningStage.REVIEW,
            decision.nextState.stage
        )

        assertEquals(
            4.5,
            decision.nextState.difficultyValue.value
        )

        assertEquals(
            14.0,
            decision.nextState.stability.days
        )

        assertEquals(
            expectedInterval,
            decision.scheduledInterval
        )

        assertEquals(
            REVIEWED_AT + expectedInterval,
            decision.nextState.dueAt
        )

        assertEquals(
            3,
            decision.nextState.reviewCount
        )

        assertEquals(
            1,
            decision.nextState.lapseCount
        )
    }



    private companion object {
        val LEARNER_ID = LearnerId("learner-1")
        val LEARNING_ITEM_ID = LearningItemId("item-1")

        val AVAILABLE_AT = Moment(1_000L)
        val REVIEWED_AT = Moment(5_000L)
        val PREVIOUS_REVIEWED_AT = Moment(3_000L)
        val EARLIER_REVIEWED_AT = Moment(4_000L)
    }
}