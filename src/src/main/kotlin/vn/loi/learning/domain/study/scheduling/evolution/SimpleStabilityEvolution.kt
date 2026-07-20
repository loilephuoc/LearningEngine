package vn.loi.learning.domain.study.scheduling.evolution

import vn.loi.learning.domain.study.memory.evolution.StabilityEvolution
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.scheduling.SchedulerPolicy

/**
 * Quy luật tiến hóa Stability của SimpleScheduler.
 *
 * Implementation này bảo toàn đúng hành vi scheduling hiện tại
 * và lấy toàn bộ tham số từ SchedulerPolicy.
 */
internal class SimpleStabilityEvolution(
    private val policy: SchedulerPolicy
) : StabilityEvolution {

    override fun evolve(
        current: Stability,
        rating: ReviewRating
    ): Stability =
        when (rating) {
            ReviewRating.AGAIN ->
                current
                    .multiplyBy(
                        policy.again.stabilityMultiplier
                    )
                    .coerceAtLeast(
                        Stability.of(
                            policy.again.minimumStabilityDays
                        )
                    )

            ReviewRating.HARD ->
                if (current.isZero()) {
                    Stability.of(
                        policy.hard.initialStabilityDays
                    )
                } else {
                    current
                        .multiplyBy(
                            policy.hard.stabilityMultiplier
                        )
                        .coerceAtLeast(
                            Stability.of(
                                policy.hard.minimumStabilityDays
                            )
                        )
                }

            ReviewRating.GOOD ->
                if (current.isZero()) {
                    Stability.of(
                        policy.good.initialStabilityDays
                    )
                } else {
                    current.multiplyBy(
                        policy.good.stabilityMultiplier
                    )
                }

            ReviewRating.EASY ->
                if (current.isZero()) {
                    Stability.of(
                        policy.easy.initialStabilityDays
                    )
                } else {
                    current.multiplyBy(
                        policy.easy.stabilityMultiplier
                    )
                }
        }
}