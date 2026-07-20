package vn.loi.learning.domain.study.memory

import kotlin.math.exp
import vn.loi.learning.domain.study.memory.model.Retrievability
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Mô hình forgetting curve đơn giản sử dụng exponential decay.
 *
 * Đây là implementation nền tảng cho MVP.
 * Nó không phải công thức FSRS.
 */
class SimpleForgettingCurve : ForgettingCurve {

    override fun calculate(
        stability: Stability,
        elapsedTime: TimeSpan
    ): Retrievability {

        if (stability.isZero()) {
            return if (elapsedTime == TimeSpan.ZERO) {
                Retrievability.FULLY_RETRIEVABLE
            } else {
                Retrievability.FORGOTTEN
            }
        }

        val elapsedDays =
            elapsedTime.toDays()

        val stabilityDays =
            stability.days

        val probability =
            exp(-elapsedDays / stabilityDays)

        return Retrievability(probability)
    }
}