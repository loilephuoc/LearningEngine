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
 * Quy tắc chuyển trạng thái khi người học chọn EASY.
 *
 * Rule không chứa công thức Difficulty, Stability hoặc interval.
 */
internal class EasyTransitionRule(
    private val policy: SchedulerPolicy.EasyPolicy =
        SchedulerPolicy.EasyPolicy(),
    private val desiredRetention: DesiredRetention =
        DesiredRetention(0.9),
    private val difficultyEvolution: DifficultyEvolution =
        SimpleDifficultyEvolution(
            SchedulerPolicy(
                easy = policy
            )
        ),
    private val stabilityEvolution: StabilityEvolution =
        SimpleStabilityEvolution(
            SchedulerPolicy(
                easy = policy
            )
        ),
    private val intervalCalculator: SchedulingIntervalCalculator =
        SchedulingIntervalCalculator(
            SimpleIntervalCalculator()
        )
) : TransitionRule {

    override val rating: ReviewRating =
        ReviewRating.EASY

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
