package vn.loi.learning.domain.study.fsrs

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.SchedulerDecision

/**
 * Thuật toán FSRS.
 *
 * Chịu trách nhiệm tính toán trạng thái tiếp theo của Memory
 * và khoảng thời gian ôn tập kế tiếp.
 */
fun interface FsrsAlgorithm {

    fun schedule(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision
}