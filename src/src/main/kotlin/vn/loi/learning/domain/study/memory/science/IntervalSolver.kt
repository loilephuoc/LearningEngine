package vn.loi.learning.domain.study.memory.science

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Giải bài toán ngược của ForgettingCurve.
 *
 * IntervalSolver chuyển:
 *
 * - Stability
 * - DesiredRetention
 *
 * thành khoảng thời gian tương ứng mà memory sẽ giảm
 * xuống mức ghi nhớ mong muốn.
 *
 * Đây là thuật toán khoa học thuần túy:
 *
 * - không phụ thuộc Scheduler;
 * - không biết FsrsState;
 * - không truy cập persistence;
 * - không thay đổi trạng thái memory.
 *
 * Các implementation có thể sử dụng công thức FSRS hoặc
 * các mô hình forgetting curve khác.
 */
fun interface IntervalSolver {

    /**
     * Tính khoảng thời gian để memory đạt mức retention mục tiêu.
     *
     * @param stability độ ổn định hiện tại của memory
     * @param desiredRetention tỷ lệ ghi nhớ mong muốn
     * @return khoảng thời gian tương ứng
     */
    fun solve(
        stability: Stability,
        desiredRetention: DesiredRetention
    ): TimeSpan
}