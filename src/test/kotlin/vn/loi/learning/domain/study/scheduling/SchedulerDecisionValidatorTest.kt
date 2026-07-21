package vn.loi.learning.domain.study.scheduling

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

class SchedulerDecisionValidatorTest {

    private val validator =
        SchedulerDecisionValidator()

    private val learnerId =
        LearnerId("scheduler-validator-learner")

    private val learningItemId =
        LearningItemId("scheduler-validator-item")

    @Test
    fun `accepts valid scheduler decision`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newState(
                availableAt = reviewedAt
            )

        val interval =
            TimeSpan.days(1)

        val nextState =
            reviewedState(
                currentState = currentState,
                reviewedAt = reviewedAt,
                interval = interval
            )

        validator.validate(
            inputState = currentState,
            reviewedAt = reviewedAt,
            decision =
                SchedulerDecision(
                    previousState = currentState,
                    nextState = nextState,
                    scheduledInterval = interval
                )
        )
    }

    @Test
    fun `rejects decision whose previous state differs from input`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newState(
                availableAt = reviewedAt
            )

        val differentPreviousState =
            currentState.copy(
                difficulty = 6.0
            )

        val interval =
            TimeSpan.days(1)

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                differentPreviousState,
                            nextState =
                                reviewedState(
                                    currentState =
                                        currentState,
                                    reviewedAt =
                                        reviewedAt,
                                    interval =
                                        interval
                                ),
                            scheduledInterval =
                                interval
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "previousState"
        )
    }

    @Test
    fun `rejects decision that changes learner identity`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newState(
                availableAt = reviewedAt
            )

        val interval =
            TimeSpan.days(1)

        val invalidNextState =
            reviewedState(
                currentState = currentState,
                reviewedAt = reviewedAt,
                interval = interval
            ).copy(
                learnerId =
                    LearnerId(
                        "different-learner"
                    )
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                currentState,
                            nextState =
                                invalidNextState,
                            scheduledInterval =
                                interval
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "learnerId"
        )
    }

    @Test
    fun `rejects decision that changes learning item identity`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newState(
                availableAt = reviewedAt
            )

        val interval =
            TimeSpan.days(1)

        val invalidNextState =
            reviewedState(
                currentState = currentState,
                reviewedAt = reviewedAt,
                interval = interval
            ).copy(
                learningItemId =
                    LearningItemId(
                        "different-item"
                    )
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                currentState,
                            nextState =
                                invalidNextState,
                            scheduledInterval =
                                interval
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "learningItemId"
        )
    }

    @Test
    fun `rejects decision that increments review count by more than one`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newState(
                availableAt = reviewedAt
            )

        val interval =
            TimeSpan.days(1)

        val invalidNextState =
            reviewedState(
                currentState = currentState,
                reviewedAt = reviewedAt,
                interval = interval
            ).copy(
                reviewCount =
                    currentState.reviewCount + 2
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                currentState,
                            nextState =
                                invalidNextState,
                            scheduledInterval =
                                interval
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "reviewCount"
        )
    }

    @Test
    fun `rejects decision that increments lapse count by more than one`() {
        val reviewedAt =
            Moment(2_000_000L)

        val currentState =
            reviewedBaseState(
                reviewedAt =
                    Moment(1_000_000L)
            )

        val interval =
            TimeSpan.days(1)

        val invalidNextState =
            currentState.copy(
                stage =
                    LearningStage.RELEARNING,
                dueAt =
                    reviewedAt + interval,
                lastReviewedAt =
                    reviewedAt,
                reviewCount =
                    currentState.reviewCount + 1,
                lapseCount =
                    currentState.lapseCount + 2
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                currentState,
                            nextState =
                                invalidNextState,
                            scheduledInterval =
                                interval
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "lapseCount"
        )
    }

    @Test
    fun `rejects decision with incorrect last reviewed time`() {
        val reviewedAt =
            Moment(2_000_000L)

        val currentState =
            reviewedBaseState(
                reviewedAt =
                    Moment(1_000_000L)
            )

        val interval =
            TimeSpan.days(1)

        val invalidNextState =
            currentState.copy(
                dueAt =
                    reviewedAt + interval,
                lastReviewedAt =
                    Moment(1_500_000L),
                reviewCount =
                    currentState.reviewCount + 1
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                currentState,
                            nextState =
                                invalidNextState,
                            scheduledInterval =
                                interval
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "lastReviewedAt"
        )
    }

    @Test
    fun `rejects decision whose due time does not match interval`() {
        val reviewedAt =
            Moment(1_000_000L)

        val currentState =
            newState(
                availableAt = reviewedAt
            )

        val interval =
            TimeSpan.days(1)

        val invalidNextState =
            reviewedState(
                currentState = currentState,
                reviewedAt = reviewedAt,
                interval = interval
            ).copy(
                dueAt =
                    reviewedAt +
                            TimeSpan.days(2)
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                currentState,
                            nextState =
                                invalidNextState,
                            scheduledInterval =
                                interval
                        )
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
    fun `rejects scheduling suspended state`() {
        val reviewedAt =
            Moment(2_000_000L)

        val currentState =
            reviewedBaseState(
                reviewedAt =
                    Moment(1_000_000L)
            ).copy(
                stage =
                    LearningStage.SUSPENDED
            )

        val interval =
            TimeSpan.days(1)

        val nextState =
            currentState.copy(
                dueAt =
                    reviewedAt + interval,
                lastReviewedAt =
                    reviewedAt,
                reviewCount =
                    currentState.reviewCount + 1
            )

        val exception =
            assertFailsWith<IllegalStateException> {
                validator.validate(
                    inputState = currentState,
                    reviewedAt = reviewedAt,
                    decision =
                        SchedulerDecision(
                            previousState =
                                currentState,
                            nextState =
                                nextState,
                            scheduledInterval =
                                interval
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "suspended"
        )
    }

    private fun newState(
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId = learnerId,
            learningItemId = learningItemId,
            availableAt = availableAt
        )

    private fun reviewedBaseState(
        reviewedAt: Moment
    ): MemoryState =
        MemoryState(
            learnerId = learnerId,
            learningItemId = learningItemId,
            stage = LearningStage.REVIEW,
            difficulty = 5.0,
            stabilityDays = 1.0,
            dueAt = reviewedAt,
            lastReviewedAt = reviewedAt,
            reviewCount = 1,
            lapseCount = 0
        )

    private fun reviewedState(
        currentState: MemoryState,
        reviewedAt: Moment,
        interval: TimeSpan
    ): MemoryState =
        currentState.copy(
            stage = LearningStage.REVIEW,
            difficulty = 4.8,
            stabilityDays = 1.0,
            dueAt = reviewedAt + interval,
            lastReviewedAt = reviewedAt,
            reviewCount =
                currentState.reviewCount + 1
        )
}