package vn.loi.learning.domain.study.scheduling.evolution

import vn.loi.learning.domain.study.memory.evolution.DifficultyEvolution
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.SchedulerPolicy

/**
 * Quy luật tiến hóa Difficulty của SimpleScheduler.
 *
 * Implementation này bảo toàn đúng hành vi hiện tại:
 * - AGAIN: tăng mạnh;
 * - HARD: tăng nhẹ;
 * - GOOD: giảm nhẹ;
 * - EASY: giảm mạnh.
 *
 * Mọi tham số đều được lấy từ SchedulerPolicy.
 */
internal class SimpleDifficultyEvolution(
    private val policy: SchedulerPolicy
) : DifficultyEvolution {

    override fun evolve(
        current: Difficulty,
        rating: ReviewRating
    ): Difficulty =
        when (rating) {
            ReviewRating.AGAIN ->
                current.increase(
                    policy.again.difficultyIncrease
                )

            ReviewRating.HARD ->
                current.increase(
                    policy.hard.difficultyIncrease
                )

            ReviewRating.GOOD ->
                current.decrease(
                    policy.good.difficultyDecrease
                )

            ReviewRating.EASY ->
                current.decrease(
                    policy.easy.difficultyDecrease
                )
        }
}