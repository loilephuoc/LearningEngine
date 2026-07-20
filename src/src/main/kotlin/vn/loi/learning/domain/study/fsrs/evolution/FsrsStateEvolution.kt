package vn.loi.learning.domain.study.fsrs.evolution

import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.fsrs.model.FsrsStateTransition
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Tiến hóa toàn bộ trạng thái FSRS sau một lần review.
 *
 * Implementation nhận trạng thái hiện tại cùng rating
 * và trả về kết quả chuyển trạng thái hoàn chỉnh.
 */
fun interface FsrsStateEvolution {

    fun evolve(
        current: FsrsState,
        rating: ReviewRating
    ): FsrsStateTransition
}