package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Contract của một scheduling transition rule.
 *
 * Mỗi implementation chịu trách nhiệm xử lý đúng một ReviewRating
 * và trả về SchedulerTransition tương ứng.
 */
internal interface TransitionRule {

    val rating: ReviewRating

    fun apply(
        state: MemoryState
    ): SchedulerTransition
}