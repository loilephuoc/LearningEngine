package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.memory.evolution.DifficultyEvolution
import vn.loi.learning.domain.study.memory.evolution.StabilityEvolution
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.scheduling.evolution.SimpleDifficultyEvolution
import vn.loi.learning.domain.study.scheduling.evolution.SimpleStabilityEvolution

/**
 * Quy tắc chuyển trạng thái khi người học chọn GOOD.
 *
 * Rule không chứa công thức Difficulty, Stability hoặc interval.
 */
internal class GoodTransitionRule(
    private val policy: SchedulerPolicy.GoodPolicy =
        SchedulerPolicy.GoodPolicy(),
    private val desiredRetention: DesiredRetention =
        DesiredRetention(0.9),
    private val difficultyEvolution: DifficultyEvolution =
        SimpleDifficultyEvolution(
            SchedulerPolicy(
                good = policy
            )
        ),
    private val stabilityEvolution: StabilityEvolution =
        SimpleStabilityEvolution(
            SchedulerPolicy(
                good = policy
            )
        ),
    private val intervalCalculator: SchedulingIntervalCalculator =
        SchedulingIntervalCalculator(
            SimpleIntervalCalculator()
        )
) : TransitionRule {

    override val rating: ReviewRating =
        ReviewRating.GOOD

    override fun apply(
        state: MemoryState
    ): SchedulerTransition {
        val nextDifficulty =
            difficultyEvolution.evolve(
                current =
                    Difficulty.of(
                        state.difficulty
                    ),
                rating = rating
            )

        val nextStability =
            stabilityEvolution.evolve(
                current =
                    Stability.of(
                        state.stabilityDays
                    ),
                rating = rating
            )

        val interval =
            intervalCalculator.calculate(
                difficulty = nextDifficulty,
                stability = nextStability,
                desiredRetention = desiredRetention
            )

        return SchedulerTransition(
            stage = LearningStage.REVIEW,
            difficulty = nextDifficulty.value,
            stabilityDays = nextStability.days,
            interval = interval
        )
    }
}
