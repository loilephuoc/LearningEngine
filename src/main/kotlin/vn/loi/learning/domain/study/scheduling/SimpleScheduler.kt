package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.algorithm.FsrsIntervalCalculator
import vn.loi.learning.domain.study.memory.algorithm.IntervalCalculator
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.science.FsrsIntervalSolver
import vn.loi.learning.domain.study.scheduling.evolution.SimpleDifficultyEvolution
import vn.loi.learning.domain.study.scheduling.evolution.SimpleStabilityEvolution

/**
 * Scheduler đơn giản dành cho MVP.
 *
 * Scheduler điều phối transition rule và lựa chọn interval algorithm
 * từ SchedulerPolicy. Nó không chứa công thức toán học.
 */
class SimpleScheduler(
    policy: SchedulerPolicy = SchedulerPolicy()
) : Scheduler {

    private val difficultyEvolution =
        SimpleDifficultyEvolution(policy)

    private val stabilityEvolution =
        SimpleStabilityEvolution(policy)

    private val intervalCalculator: IntervalCalculator =
        when (policy.algorithm) {
            SchedulingAlgorithm.SIMPLE ->
                SimpleIntervalCalculator()

            SchedulingAlgorithm.FSRS ->
                FsrsIntervalCalculator(
                    intervalSolver =
                        FsrsIntervalSolver()
                )
        }

    private val schedulingIntervalCalculator =
        SchedulingIntervalCalculator(
            intervalCalculator = intervalCalculator
        )

    private val registry =
        TransitionRuleRegistry(
            listOf(
                AgainTransitionRule(
                    policy = policy.again,
                    difficultyEvolution = difficultyEvolution,
                    stabilityEvolution = stabilityEvolution
                ),
                HardTransitionRule(
                    policy = policy.hard,
                    desiredRetention = policy.desiredRetention,
                    difficultyEvolution = difficultyEvolution,
                    stabilityEvolution = stabilityEvolution,
                    intervalCalculator = schedulingIntervalCalculator
                ),
                GoodTransitionRule(
                    policy = policy.good,
                    desiredRetention = policy.desiredRetention,
                    difficultyEvolution = difficultyEvolution,
                    stabilityEvolution = stabilityEvolution,
                    intervalCalculator = schedulingIntervalCalculator
                ),
                EasyTransitionRule(
                    policy = policy.easy,
                    desiredRetention = policy.desiredRetention,
                    difficultyEvolution = difficultyEvolution,
                    stabilityEvolution = stabilityEvolution,
                    intervalCalculator = schedulingIntervalCalculator
                )
            )
        )

    override fun schedule(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision {
        require(currentState.stage != LearningStage.SUSPENDED) {
            "A suspended item cannot be reviewed."
        }

        currentState.lastReviewedAt?.let { lastReviewedAt ->
            require(reviewedAt >= lastReviewedAt) {
                "Review time must not be earlier than the last review time."
            }
        }

        val transition =
            registry
                .ruleFor(rating)
                .apply(currentState)

        val nextState =
            currentState.copy(
                stage = transition.stage,
                difficulty = transition.difficulty,
                stabilityDays = transition.stabilityDays,
                dueAt = reviewedAt + transition.interval,
                lastReviewedAt = reviewedAt,
                reviewCount = currentState.reviewCount + 1,
                lapseCount =
                    currentState.lapseCount +
                            if (
                                isLapse(
                                    currentState,
                                    rating
                                )
                            ) {
                                1
                            } else {
                                0
                            }
            )

        return SchedulerDecision(
            previousState = currentState,
            nextState = nextState,
            scheduledInterval = transition.interval
        )
    }

    private fun isLapse(
        state: MemoryState,
        rating: ReviewRating
    ): Boolean =
        rating == ReviewRating.AGAIN &&
                state.stage in LAPSE_STAGES

    private companion object {
        val LAPSE_STAGES =
            setOf(
                LearningStage.REVIEW,
                LearningStage.RELEARNING,
                LearningStage.MASTERED
            )
    }
}
