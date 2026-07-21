package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Scheduler decorator áp dụng invariant validation cho mọi decision.
 *
 * Decorator không thay đổi kết quả của delegate. Khi decision hợp lệ,
 * chính SchedulerDecision do delegate tạo ra được trả về nguyên vẹn.
 *
 * Ví dụ:
 *
 * ValidatingScheduler(
 *     delegate = FsrsScheduler()
 * )
 */
class ValidatingScheduler(
    private val delegate: Scheduler,
    private val validator: SchedulerDecisionValidator =
        SchedulerDecisionValidator()
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

        validator.validate(
            inputState = currentState,
            reviewedAt = reviewedAt,
            decision = decision
        )

        return decision
    }
}