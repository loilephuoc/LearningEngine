package vn.loi.learning.domain.study.scheduling

import kotlin.math.max
import kotlin.math.roundToLong
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Scheduler đơn giản dành cho MVP.
 *
 * Nó cố ý nằm sau Scheduler interface để có thể được thay bằng FSRS
 * mà không sửa Application Layer, UI hoặc repository.
 */
class SimpleScheduler : Scheduler {

    override fun schedule(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision {
        require(currentState.stage != LearningStage.SUSPENDED) {
            "A suspended item cannot be reviewed."
        }

        val transition = when (rating) {
            ReviewRating.AGAIN -> scheduleAgain(currentState)
            ReviewRating.HARD -> scheduleHard(currentState)
            ReviewRating.GOOD -> scheduleGood(currentState)
            ReviewRating.EASY -> scheduleEasy(currentState)
        }

        val nextState = currentState.copy(
            stage = transition.stage,
            difficulty = transition.difficulty,
            stabilityDays = transition.stabilityDays,
            dueAt = reviewedAt + transition.interval,
            lastReviewedAt = reviewedAt,
            reviewCount = currentState.reviewCount + 1,
            lapseCount = currentState.lapseCount +
                    if (isLapse(currentState, rating)) 1 else 0
        )

        return SchedulerDecision(
            previousState = currentState,
            nextState = nextState,
            scheduledInterval = transition.interval
        )
    }

    private fun scheduleAgain(state: MemoryState): Transition {
        val nextStage =
            if (state.reviewCount == 0) {
                LearningStage.LEARNING
            } else {
                LearningStage.RELEARNING
            }

        return Transition(
            stage = nextStage,
            difficulty = clampDifficulty(state.difficulty + 0.8),
            stabilityDays = max(0.1, state.stabilityDays * 0.5),
            interval = TimeSpan.minutes(10)
        )
    }

    private fun scheduleHard(state: MemoryState): Transition {
        val stability =
            if (state.stabilityDays == 0.0) {
                0.5
            } else {
                max(0.5, state.stabilityDays * 1.2)
            }

        return Transition(
            stage = LearningStage.REVIEW,
            difficulty = clampDifficulty(state.difficulty + 0.3),
            stabilityDays = stability,
            interval = daysToTimeSpan(stability)
        )
    }

    private fun scheduleGood(state: MemoryState): Transition {
        val stability =
            if (state.stabilityDays == 0.0) {
                1.0
            } else {
                state.stabilityDays * 2.5
            }

        return Transition(
            stage = LearningStage.REVIEW,
            difficulty = clampDifficulty(state.difficulty - 0.2),
            stabilityDays = stability,
            interval = daysToTimeSpan(stability)
        )
    }

    private fun scheduleEasy(state: MemoryState): Transition {
        val stability =
            if (state.stabilityDays == 0.0) {
                4.0
            } else {
                state.stabilityDays * 3.5
            }

        return Transition(
            stage = LearningStage.REVIEW,
            difficulty = clampDifficulty(state.difficulty - 0.5),
            stabilityDays = stability,
            interval = daysToTimeSpan(stability)
        )
    }

    private fun isLapse(
        state: MemoryState,
        rating: ReviewRating
    ): Boolean =
        rating == ReviewRating.AGAIN &&
                state.reviewCount > 0

    private fun clampDifficulty(value: Double): Double =
        value.coerceIn(
            MemoryState.MIN_DIFFICULTY,
            MemoryState.MAX_DIFFICULTY
        )

    private fun daysToTimeSpan(days: Double): TimeSpan {
        require(days >= 0.0) {
            "Scheduled days must not be negative."
        }

        val millis = (days * MILLIS_PER_DAY).roundToLong()
        return TimeSpan(millis)
    }

    private data class Transition(
        val stage: LearningStage,
        val difficulty: Double,
        val stabilityDays: Double,
        val interval: TimeSpan
    )

    private companion object {
        const val MILLIS_PER_DAY: Double = 86_400_000.0
    }
}