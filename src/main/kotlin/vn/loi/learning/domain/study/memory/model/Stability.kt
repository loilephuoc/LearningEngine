package vn.loi.learning.domain.study.memory.model

import kotlin.math.roundToLong

/**
 * Độ ổn định của ký ức, biểu diễn theo số ngày.
 *
 * Stability luôn:
 * - hữu hạn;
 * - không âm.
 *
 * Value Object này chứa các hành vi nghiệp vụ dùng chung
 * cho các thuật toán scheduling.
 */
@JvmInline
value class Stability private constructor(
    val days: Double
) : Comparable<Stability> {

    init {
        require(days.isFinite()) {
            "Stability days must be finite."
        }

        require(days >= 0.0) {
            "Stability days must not be negative."
        }
    }

    fun isZero(): Boolean =
        days == 0.0

    fun isPositive(): Boolean =
        days > 0.0

    fun multiplyBy(
        multiplier: Double
    ): Stability {
        require(multiplier.isFinite()) {
            "Stability multiplier must be finite."
        }

        require(multiplier >= 0.0) {
            "Stability multiplier must not be negative."
        }

        return of(days * multiplier)
    }

    fun coerceAtLeast(
        minimum: Stability
    ): Stability =
        if (this >= minimum) {
            this
        } else {
            minimum
        }

    fun toTimeSpan(): TimeSpan {
        val millis =
            (days * MILLIS_PER_DAY).roundToLong()

        return TimeSpan(millis)
    }

    override fun compareTo(
        other: Stability
    ): Int =
        days.compareTo(other.days)

    override fun toString(): String =
        "${days}d"

    companion object {
        private const val MILLIS_PER_DAY =
            86_400_000.0

        val ZERO =
            Stability(0.0)

        fun of(
            days: Double
        ): Stability =
            Stability(days)
    }
}