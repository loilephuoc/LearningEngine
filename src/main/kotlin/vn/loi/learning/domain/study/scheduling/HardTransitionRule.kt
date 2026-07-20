package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.memory.evolution.DifficultyEvolution
import vn.loi.learning.domain.study.memory.evolution.StabilityEvolution
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.evolution.SimpleDifficultyEvolution
import vn.loi.learning.domain.study.scheduling.evolution.SimpleStabilityEvolution

/**
 * Quy tắc chuyển trạng thái khi người học chọn HARD.
 *
 * Rule không chứa công thức Difficulty, Stability hoặc interval.
 */
internal class HardTransitionRule(
    private val policy: SchedulerPolicy.HardPolicy =
        SchedulerPolicy.HardPolicy(),
    private val desiredRetention: DesiredRetention =
        DesiredRetention(0.9),
    private val difficultyEvolution: DifficultyEvolution =
        SimpleDifficultyEvolution(
            SchedulerPolicy(
                hard = policy
            )
        ),
    private val stabilityEvolution: StabilityEvolution =
        SimpleStabilityEvolution(
            SchedulerPolicy(
                hard = policy
            )
        ),
    private val intervalCalculator: SchedulingIntervalCalculator =
        SchedulingIntervalCalculator(
            SimpleIntervalCalculator()
        )
) : TransitionRule {

    override val rating: ReviewRating =
        ReviewRating.HARD

    override fun apply(
        state: MemoryState
    ): SchedulerTransition {
        val nextDifficulty =
            difficultyEvolution.evolve(
                current = state.difficultyValue,
                rating = rating
            )

        val nextStability =
            stabilityEvolution.evolve(
                current = state.stability,
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
            difficultyValue = nextDifficulty,
            stability = nextStability,
            interval = interval
        )
    }
}