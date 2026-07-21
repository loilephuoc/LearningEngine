package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Kiểm tra invariant chung của SchedulerDecision.
 *
 * Validator độc lập với thuật toán scheduling cụ thể. Vì vậy cùng một
 * contract được áp dụng cho SimpleScheduler, FsrsScheduler và các
 * Scheduler implementation được bổ sung trong tương lai.
 *
 * Validator không:
 *
 * - tính difficulty;
 * - tính stability;
 * - quyết định stage;
 * - quyết định interval;
 * - thay đổi MemoryState;
 * - persist dữ liệu.
 */
class SchedulerDecisionValidator {

    fun validate(
        inputState: MemoryState,
        reviewedAt: Moment,
        decision: SchedulerDecision
    ) {
        validateInput(
            inputState = inputState,
            reviewedAt = reviewedAt
        )

        validatePreviousState(
            inputState = inputState,
            decision = decision
        )

        validateIdentity(
            inputState = inputState,
            nextState = decision.nextState
        )

        validateReviewProgress(
            inputState = inputState,
            nextState = decision.nextState,
            reviewedAt = reviewedAt
        )

        validateSchedule(
            reviewedAt = reviewedAt,
            decision = decision
        )
    }

    private fun validateInput(
        inputState: MemoryState,
        reviewedAt: Moment
    ) {
        check(
            inputState.stage !=
                    LearningStage.SUSPENDED
        ) {
            "Scheduler invariant violated: " +
                    "a suspended MemoryState cannot be scheduled."
        }

        inputState.lastReviewedAt?.let { lastReviewedAt ->
            check(reviewedAt >= lastReviewedAt) {
                "Scheduler invariant violated: reviewedAt must not " +
                        "be earlier than lastReviewedAt."
            }
        }
    }

    private fun validatePreviousState(
        inputState: MemoryState,
        decision: SchedulerDecision
    ) {
        check(
            decision.previousState ==
                    inputState
        ) {
            "Scheduler invariant violated: previousState must equal " +
                    "the MemoryState supplied to Scheduler."
        }
    }

    private fun validateIdentity(
        inputState: MemoryState,
        nextState: MemoryState
    ) {
        check(
            nextState.learnerId ==
                    inputState.learnerId
        ) {
            "Scheduler invariant violated: learnerId must not change."
        }

        check(
            nextState.learningItemId ==
                    inputState.learningItemId
        ) {
            "Scheduler invariant violated: learningItemId must not change."
        }
    }

    private fun validateReviewProgress(
        inputState: MemoryState,
        nextState: MemoryState,
        reviewedAt: Moment
    ) {
        check(
            nextState.reviewCount ==
                    inputState.reviewCount + 1
        ) {
            "Scheduler invariant violated: reviewCount must increase " +
                    "by exactly one."
        }

        val lapseIncrement =
            nextState.lapseCount -
                    inputState.lapseCount

        check(lapseIncrement in 0..1) {
            "Scheduler invariant violated: lapseCount may increase " +
                    "by zero or one only."
        }

        check(
            nextState.lastReviewedAt ==
                    reviewedAt
        ) {
            "Scheduler invariant violated: nextState.lastReviewedAt " +
                    "must equal reviewedAt."
        }
    }

    private fun validateSchedule(
        reviewedAt: Moment,
        decision: SchedulerDecision
    ) {
        check(
            decision.scheduledInterval >=
                    TimeSpan.ZERO
        ) {
            "Scheduler invariant violated: scheduledInterval " +
                    "must not be negative."
        }

        check(
            decision.nextState.dueAt ==
                    reviewedAt +
                    decision.scheduledInterval
        ) {
            "Scheduler invariant violated: nextState.dueAt must equal " +
                    "reviewedAt plus scheduledInterval."
        }
    }
}