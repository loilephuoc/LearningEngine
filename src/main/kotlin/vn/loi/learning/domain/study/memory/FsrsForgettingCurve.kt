package vn.loi.learning.domain.study.memory

import kotlin.math.pow
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters
import vn.loi.learning.domain.study.memory.model.Retrievability
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Forgetting curve theo mô hình FSRS v6.
 *
 * Công thức:
 *
 * R(t, S) = (1 + factor * t / S) ^ decay
 *
 * Trong đó:
 *
 * decay  = -w20
 * factor = 0.9 ^ (1 / decay) - 1
 *
 * Stability được định nghĩa là khoảng thời gian
 * mà retrievability giảm xuống còn 0.9.
 */
class FsrsForgettingCurve(
    private val parameters: FsrsParameters = FsrsParameters.DEFAULT
) : ForgettingCurve {

    private val decay: Double =
        -parameters[DECAY_PARAMETER_INDEX]

    private val factor: Double =
        TARGET_RETRIEVABILITY.pow(1.0 / decay) - 1.0

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

        val probability =
            (
                    1.0 +
                            factor *
                            elapsedDays /
                            stability.days
                    ).pow(decay)

        return Retrievability(
            probability.coerceIn(
                minimumValue = 0.0,
                maximumValue = 1.0
            )
        )
    }

    private companion object {

        const val DECAY_PARAMETER_INDEX: Int = 20

        const val TARGET_RETRIEVABILITY: Double = 0.9
    }
}