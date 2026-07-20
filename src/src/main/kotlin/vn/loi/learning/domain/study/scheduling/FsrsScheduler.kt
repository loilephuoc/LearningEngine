package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.fsrs.DefaultFsrsAlgorithm
import vn.loi.learning.domain.study.fsrs.FsrsAlgorithm
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Scheduler triển khai thuật toán FSRS.
 *
 * Class này là adapter giữa Scheduler abstraction
 * và FsrsAlgorithm.
 *
 * Application Layer chỉ nhìn thấy Scheduler,
 * không phụ thuộc trực tiếp vào thuật toán FSRS.
 */
class FsrsScheduler(
    private val algorithm: FsrsAlgorithm = DefaultFsrsAlgorithm()
) : Scheduler {

    override fun schedule(
        currentState: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): SchedulerDecision =
        algorithm.schedule(
            currentState = currentState,
            rating = rating,
            reviewedAt = reviewedAt
        )
}