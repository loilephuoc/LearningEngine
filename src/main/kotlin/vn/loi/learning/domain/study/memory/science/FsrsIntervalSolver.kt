package vn.loi.learning.domain.study.memory.science

import kotlin.math.pow
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Tính khoảng thời gian cần thiết để retrievability
 * giảm xuống mức desired retention theo FSRS v6.
 *
 * Đây là phép toán nghịch đảo của FsrsForgettingCurve.
 *
 * Forgetting curve:
 *
 * R = (1 + factor * t / S) ^ decay
 *
 * Inverse:
 *
 * t = S * (R ^ (1 / decay) - 1) / factor
 */
class FsrsIntervalSolver(
    private val parameters: FsrsParameters = FsrsParameters.DEFAULT
) : IntervalSolver {

    private val decay: Double =
        -parameters[DECAY_PARAMETER_INDEX]

    private val factor: Double =
        TARGET_RETRIEVABILITY.pow(1.0 / decay) - 1.0

    override fun solve(
        stability: Stability,
        desiredRetention: DesiredRetention
    ): TimeSpan {

        if (stability.isZero()) {
            return TimeSpan.ZERO
        }

        require(desiredRetention.value > 0.0) {
            "Desired retention must be greater than zero for FSRS interval calculation."
        }

        if (desiredRetention.value == 1.0) {
            return TimeSpan.ZERO
        }

        val intervalDays =
            stability.days *
                    (
                            desiredRetention.value.pow(1.0 / decay) -
                                    1.0
                            ) /
                    factor

        return TimeSpan.days(intervalDays)
    }

    private companion object {

        const val DECAY_PARAMETER_INDEX: Int = 20

        const val TARGET_RETRIEVABILITY: Double = 0.9
    }
}