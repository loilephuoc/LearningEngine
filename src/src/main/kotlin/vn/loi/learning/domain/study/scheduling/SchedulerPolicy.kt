package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention

/**
 * Bộ tham số bất biến điều khiển hành vi của Scheduler.
 *
 * Policy chỉ chứa cấu hình, không sở hữu công thức.
 */
data class SchedulerPolicy(
    val algorithm: SchedulingAlgorithm =
        SchedulingAlgorithm.FSRS,
    val desiredRetention: DesiredRetention =
        DesiredRetention(0.9),
    val again: AgainPolicy =
        AgainPolicy(),
    val hard: HardPolicy =
        HardPolicy(),
    val good: GoodPolicy =
        GoodPolicy(),
    val easy: EasyPolicy =
        EasyPolicy()
) {

    data class AgainPolicy(
        val difficultyIncrease: Double = 0.8,
        val stabilityMultiplier: Double = 0.5,
        val minimumStabilityDays: Double = 0.1,
        val intervalMinutes: Long = 10L
    )

    data class HardPolicy(
        val difficultyIncrease: Double = 0.3,
        val initialStabilityDays: Double = 0.5,
        val stabilityMultiplier: Double = 1.2,
        val minimumStabilityDays: Double = 0.5
    )

    data class GoodPolicy(
        val difficultyDecrease: Double = 0.2,
        val initialStabilityDays: Double = 1.0,
        val stabilityMultiplier: Double = 2.5
    )

    data class EasyPolicy(
        val difficultyDecrease: Double = 0.5,
        val initialStabilityDays: Double = 4.0,
        val stabilityMultiplier: Double = 3.5
    )
}
