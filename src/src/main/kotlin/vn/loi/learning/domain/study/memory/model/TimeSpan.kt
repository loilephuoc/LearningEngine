package vn.loi.learning.domain.study.memory.model

import kotlin.math.roundToLong

/**
 * Một khoảng thời gian không âm.
 */
@JvmInline
value class TimeSpan(
    val millis: Long
) : Comparable<TimeSpan> {

    init {
        require(millis >= 0L) {
            "TimeSpan millis must not be negative."
        }
    }

    override fun compareTo(
        other: TimeSpan
    ): Int =
        millis.compareTo(other.millis)

    fun toSeconds(): Long =
        millis / MILLIS_PER_SECOND

    fun toMinutes(): Long =
        millis / MILLIS_PER_MINUTE

    fun toHours(): Long =
        millis / MILLIS_PER_HOUR

    fun toDays(): Double =
        millis.toDouble() / MILLIS_PER_DAY

    override fun toString(): String =
        "${millis}ms"

    companion object {
        private const val MILLIS_PER_SECOND =
            1_000L

        private const val MILLIS_PER_MINUTE =
            60_000L

        private const val MILLIS_PER_HOUR =
            3_600_000L

        private const val MILLIS_PER_DAY =
            86_400_000L

        val ZERO: TimeSpan =
            TimeSpan(0L)

        fun seconds(
            value: Long
        ): TimeSpan {
            require(value >= 0L) {
                "Seconds must not be negative."
            }

            return TimeSpan(
                Math.multiplyExact(
                    value,
                    MILLIS_PER_SECOND
                )
            )
        }

        fun minutes(
            value: Long
        ): TimeSpan {
            require(value >= 0L) {
                "Minutes must not be negative."
            }

            return TimeSpan(
                Math.multiplyExact(
                    value,
                    MILLIS_PER_MINUTE
                )
            )
        }

        fun hours(
            value: Long
        ): TimeSpan {
            require(value >= 0L) {
                "Hours must not be negative."
            }

            return TimeSpan(
                Math.multiplyExact(
                    value,
                    MILLIS_PER_HOUR
                )
            )
        }

        fun days(
            value: Long
        ): TimeSpan {
            require(value >= 0L) {
                "Days must not be negative."
            }

            return TimeSpan(
                Math.multiplyExact(
                    value,
                    MILLIS_PER_DAY
                )
            )
        }

        fun days(
            value: Double
        ): TimeSpan {
            require(value.isFinite()) {
                "Days must be finite."
            }

            require(value >= 0.0) {
                "Days must not be negative."
            }

            return TimeSpan(
                (value * MILLIS_PER_DAY).roundToLong()
            )
        }
    }
}