package vn.loi.learning.domain.study.fsrs.evolution

import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.fsrs.model.FsrsStateTransition
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Triển khai mặc định của thuật toán FSRS State Evolution.
 *
 * Hiện tại implementation chỉ trả về trạng thái hiện tại để
 * hoàn thiện kiến trúc. Logic cập nhật Difficulty và Stability
 * sẽ được bổ sung dần ở các bước tiếp theo.
 */
class DefaultFsrsStateEvolution : FsrsStateEvolution {

    override fun evolve(
        current: FsrsState,
        rating: ReviewRating
    ): FsrsStateTransition {

        return FsrsStateTransition(
            nextState = current
        )
    }
}