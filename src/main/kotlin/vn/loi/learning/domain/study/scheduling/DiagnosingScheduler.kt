package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Scheduler decorator phát diagnostics sau mỗi decision thành công.
 *
 * Decorator:
 *
 * - không thay đổi Scheduler contract;
 * - không thay đổi decision của delegate;
 * - không sở hữu persistence hoặc logging;
 * - chỉ chuyển diagnostics tới callback được cung cấp.
 */
class DiagnosingScheduler(
    private val delegate: Scheduler,
    private val diagnosticsConsumer:
        (SchedulerDiagnostics) -> Unit,
    private val metricsCalculator:
    SchedulerMetricsCalculator =
        SchedulerMetricsCalculator()
) : Scheduler {

    override fun schedule(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision {
        val decision =
            delegate.schedule(
                currentState = currentState,
                rating = rating,
                reviewedAt = reviewedAt
            )

        val diagnostics =
            SchedulerDiagnostics(
                rating = rating,
                decision = decision,
                metrics =
                    metricsCalculator.calculate(
                        decision
                    )
            )

        diagnosticsConsumer(
            diagnostics
        )

        return decision
    }
}
