package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.memory.algorithm.IntervalCalculator
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Thuật toán interval đơn giản tương thích hành vi MVP cũ:
 * interval bằng Stability hiện tại.
 */
class SimpleIntervalCalculator : IntervalCalculator {

    override fun calculate(
        state: FsrsState,
        desiredRetention: DesiredRetention
    ): TimeSpan =
        state.stability.toTimeSpan()
}
