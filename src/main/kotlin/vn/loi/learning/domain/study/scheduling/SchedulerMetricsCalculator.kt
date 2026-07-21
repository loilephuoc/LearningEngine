package vn.loi.learning.domain.study.scheduling

/**
 * Chuyển SchedulerDecision thành số liệu diagnostics nhỏ gọn.
 *
 * Calculator không thay đổi decision hoặc MemoryState.
 */
class SchedulerMetricsCalculator {

    fun calculate(
        decision: SchedulerDecision
    ): SchedulerMetrics =
        SchedulerMetrics(
            scheduledInterval =
                decision.scheduledInterval,
            previousStage =
                decision.previousState.stage,
            nextStage =
                decision.nextState.stage,
            difficultyDelta =
                decision.nextState
                    .difficultyValue
                    .value -
                        decision.previousState
                            .difficultyValue
                            .value,
            stabilityDeltaDays =
                decision.nextState
                    .stability
                    .days -
                        decision.previousState
                            .stability
                            .days,
            reviewCountIncrement =
                decision.nextState.reviewCount -
                        decision.previousState.reviewCount,
            lapseCountIncrement =
                decision.nextState.lapseCount -
                        decision.previousState.lapseCount
        )
}