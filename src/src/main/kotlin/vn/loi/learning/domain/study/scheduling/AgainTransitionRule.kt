package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.evolution.DifficultyEvolution
import vn.loi.learning.domain.study.memory.evolution.StabilityEvolution
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.scheduling.evolution.SimpleDifficultyEvolution
import vn.loi.learning.domain.study.scheduling.evolution.SimpleStabilityEvolution

/**
 * Quy tắc chuyển trạng thái khi người học chọn AGAIN.
 *
 * Rule này:
 * - xác định LearningStage tiếp theo;
 * - gọi các evolution service;
 * - sử dụng learning step cố định từ SchedulerPolicy;
 * - tạo SchedulerTransition.
 *
 * Interval của AGAIN là short-term learning/relearning step,
 * không phải long-term review interval từ FSRS.
 *
 * Rule không trực tiếp chứa công thức Difficulty hoặc Stability.
 */
internal class AgainTransitionRule(
    private val policy: SchedulerPolicy.AgainPolicy =
        SchedulerPolicy.AgainPolicy(),
    private val difficultyEvolution: DifficultyEvolution =
        SimpleDifficultyEvolution(
            SchedulerPolicy(
                again = policy
            )
        ),
    private val stabilityEvolution: StabilityEvolution =
        SimpleStabilityEvolution(
            SchedulerPolicy(
                again = policy
            )
        )
) : TransitionRule {

    override val rating: ReviewRating =
        ReviewRating.AGAIN

    override fun apply(
        state: MemoryState
    ): SchedulerTransition {
        val nextStage =
            when (state.stage) {
                LearningStage.NEW,
                LearningStage.LEARNING ->
                    LearningStage.LEARNING

                LearningStage.REVIEW,
                LearningStage.RELEARNING,
                LearningStage.MASTERED ->
                    LearningStage.RELEARNING

                LearningStage.SUSPENDED ->
                    error(
                        "Suspended state must be rejected before scheduling."
                    )
            }

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

        return SchedulerTransition(
            stage = nextStage,
            difficulty = nextDifficulty.value,
            stabilityDays = nextStability.days,
            interval =
                TimeSpan.minutes(
                    policy.intervalMinutes
                )
        )
    }
}