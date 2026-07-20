package vn.loi.learning.domain.study.memory.algorithm

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Tính khoảng thời gian ôn tập được đề xuất từ trạng thái memory.
 *
 * Contract này chỉ chịu trách nhiệm chuyển:
 *
 * - trạng thái toán học hiện tại của memory;
 * - mục tiêu tỷ lệ ghi nhớ;
 *
 * thành một khoảng thời gian ôn tập được đề xuất.
 *
 * IntervalCalculator:
 *
 * - không thay đổi FsrsState;
 * - không quyết định thời điểm review tuyệt đối;
 * - không phụ thuộc Scheduler;
 * - không truy cập repository hoặc persistence;
 * - không chứa trách nhiệm cập nhật Difficulty hoặc Stability.
 *
 * Implementation cụ thể có thể sử dụng công thức FSRS hoặc
 * một thuật toán interval khác mà không làm thay đổi scheduler.
 */
fun interface IntervalCalculator {

    /**
     * Tính khoảng thời gian ôn tập được đề xuất.
     *
     * @param state trạng thái toán học hiện tại của memory
     * @param desiredRetention tỷ lệ ghi nhớ mục tiêu
     * @return khoảng thời gian không âm trước lần review tiếp theo
     */
    fun calculate(
        state: FsrsState,
        desiredRetention: DesiredRetention
    ): TimeSpan
}