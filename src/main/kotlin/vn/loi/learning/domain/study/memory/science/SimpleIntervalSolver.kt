package vn.loi.learning.domain.study.memory.science

import kotlin.math.ln
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * IntervalSolver tương ứng với SimpleForgettingCurve.
 *
 * SimpleForgettingCurve sử dụng:
 *
 * R(t) = exp(-t / S)
 *
 * Phép toán nghịch đảo là:
 *
 * t = -S * ln(R)
 *
 * Trong đó:
 *
 * - S là Stability theo số ngày;
 * - R là DesiredRetention;
 * - t là khoảng thời gian theo số ngày.
 *
 * Implementation này:
 *
 * - không phụ thuộc Scheduler;
 * - không thay đổi trạng thái memory;
 * - không truy cập persistence;
 * - chỉ hỗ trợ DesiredRetention lớn hơn 0.0.
 */
class SimpleIntervalSolver : IntervalSolver {

    override fun solve(
        stability: Stability,
        desiredRetention: DesiredRetention
    ): TimeSpan {
        if (stability.isZero()) {
            return TimeSpan.ZERO
        }

        require(desiredRetention.value > 0.0) {
            "SimpleIntervalSolver requires DesiredRetention greater than 0.0."
        }

        val intervalDays =
            -stability.days * ln(desiredRetention.value)

        return TimeSpan.days(intervalDays)
    }
}