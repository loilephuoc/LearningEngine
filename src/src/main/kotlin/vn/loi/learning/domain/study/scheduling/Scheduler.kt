package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Chính sách tính trạng thái trí nhớ sau một lần review.
 *
 * Application Layer chỉ phụ thuộc interface này.
 * SimpleScheduler, FSRS hoặc AI Scheduler đều có thể triển khai nó.
 */
fun interface Scheduler {

    fun schedule(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision
}