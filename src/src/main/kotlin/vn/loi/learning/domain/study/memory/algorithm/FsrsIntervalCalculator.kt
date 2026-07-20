package vn.loi.learning.domain.study.memory.algorithm

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.memory.science.IntervalSolver

/**
 * IntervalCalculator dành cho trạng thái FSRS.
 *
 * Class này chỉ điều phối dữ liệu giữa FsrsState và IntervalSolver.
 * Công thức toán học cụ thể thuộc trách nhiệm của IntervalSolver.
 *
 * FsrsIntervalCalculator:
 *
 * - không thay đổi FsrsState;
 * - không tự triển khai công thức forgetting curve;
 * - không phụ thuộc Scheduler;
 * - không truy cập persistence;
 * - không sử dụng Difficulty khi chưa có use case thực tế.
 */
class FsrsIntervalCalculator(
    private val intervalSolver: IntervalSolver
) : IntervalCalculator {

    override fun calculate(
        state: FsrsState,
        desiredRetention: DesiredRetention
    ): TimeSpan =
        intervalSolver.solve(
            stability = state.stability,
            desiredRetention = desiredRetention
        )
}