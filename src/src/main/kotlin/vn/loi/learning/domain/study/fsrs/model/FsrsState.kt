package vn.loi.learning.domain.study.fsrs.model

import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.Stability

/**
 * Trạng thái toán học của một memory theo mô hình FSRS.
 *
 * FsrsState chỉ chứa các đại lượng nội tại của memory:
 *
 * - Difficulty: độ khó tương đối của memory
 * - Stability: độ ổn định của memory theo thời gian
 *
 * Retrievability không được lưu trong state vì nó là giá trị
 * phụ thuộc thời gian và được tính bởi ForgettingCurve.
 *
 * DesiredRetention không thuộc state vì nó là mục tiêu
 * scheduling do policy hoặc learner profile cung cấp.
 */
data class FsrsState(
    val difficulty: Difficulty,
    val stability: Stability
)