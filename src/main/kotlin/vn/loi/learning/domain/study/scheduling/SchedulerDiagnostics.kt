package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Diagnostics đầy đủ của một lần scheduling.
 *
 * decision giữ kết quả domain nguyên bản.
 * metrics cung cấp số liệu tổng hợp cho logging hoặc telemetry.
 */
data class SchedulerDiagnostics(
    val rating: ReviewRating,
    val decision: SchedulerDecision,
    val metrics: SchedulerMetrics
) {

    init {
        require(
            metrics.scheduledInterval ==
                    decision.scheduledInterval
        ) {
            "Scheduler diagnostics interval must match decision interval."
        }

        require(
            metrics.previousStage ==
                    decision.previousState.stage
        ) {
            "Scheduler diagnostics previous stage must match decision."
        }

        require(
            metrics.nextStage ==
                    decision.nextState.stage
        ) {
            "Scheduler diagnostics next stage must match decision."
        }

        require(
            metrics.reviewCountIncrement ==
                    decision.nextState.reviewCount -
                    decision.previousState.reviewCount
        ) {
            "Scheduler diagnostics review count increment must match decision."
        }

        require(
            metrics.lapseCountIncrement ==
                    decision.nextState.lapseCount -
                    decision.previousState.lapseCount
        ) {
            "Scheduler diagnostics lapse count increment must match decision."
        }
    }
}