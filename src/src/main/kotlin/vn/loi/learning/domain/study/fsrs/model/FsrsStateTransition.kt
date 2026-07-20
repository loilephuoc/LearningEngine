package vn.loi.learning.domain.study.fsrs.model

/**
 * Kết quả của một lần tiến hóa trạng thái FSRS.
 *
 * Hiện tại chỉ bao gồm trạng thái mới.
 *
 * Sau này có thể mở rộng thêm:
 *
 * - predictedRetrievability
 * - recommendedInterval
 * - forgettingIndex
 * - confidence
 *
 * mà không thay đổi contract của Evolution.
 */
data class FsrsStateTransition(

    val nextState: FsrsState
)