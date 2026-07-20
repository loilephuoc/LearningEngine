package vn.loi.learning.domain.study.memory

import vn.loi.learning.domain.study.memory.model.Retrievability
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Mô hình suy giảm khả năng truy hồi theo thời gian.
 *
 * ForgettingCurve sử dụng độ ổn định của ký ức và khoảng
 * thời gian đã trôi qua để tính xác suất truy hồi hiện tại.
 *
 * Contract này không phụ thuộc Scheduler, MemoryState
 * hoặc persistence.
 */
fun interface ForgettingCurve {

    fun calculate(
        stability: Stability,
        elapsedTime: TimeSpan
    ): Retrievability
}