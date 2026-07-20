package vn.loi.learning.domain.study.memory.model

/**
 * Mức độ khó của một LearningItem đối với người học.
 *
 * Difficulty luôn nằm trong phạm vi từ MIN_VALUE đến MAX_VALUE.
 * Các phép tăng và giảm tự động clamp để giữ invariant.
 */
@JvmInline
value class Difficulty private constructor(
    val value: Double
) {

    init {
        require(value.isFinite()) {
            "Difficulty must be finite."
        }

        require(value in MIN_VALUE..MAX_VALUE) {
            "Difficulty must be between $MIN_VALUE and $MAX_VALUE, but was $value."
        }
    }

    fun increase(
        delta: Double
    ): Difficulty {
        requireValidDelta(delta)

        return clamped(value + delta)
    }

    fun decrease(
        delta: Double
    ): Difficulty {
        requireValidDelta(delta)

        return clamped(value - delta)
    }

    override fun toString(): String =
        value.toString()

    companion object {
        const val MIN_VALUE: Double = 1.0
        const val MAX_VALUE: Double = 10.0
        const val DEFAULT_VALUE: Double = 5.0

        val DEFAULT: Difficulty =
            Difficulty(DEFAULT_VALUE)

        fun of(
            value: Double
        ): Difficulty =
            Difficulty(value)

        fun clamped(
            value: Double
        ): Difficulty {
            require(value.isFinite()) {
                "Difficulty must be finite."
            }

            return Difficulty(
                value.coerceIn(
                    MIN_VALUE,
                    MAX_VALUE
                )
            )
        }

        private fun requireValidDelta(
            delta: Double
        ) {
            require(delta.isFinite()) {
                "Difficulty delta must be finite."
            }

            require(delta >= 0.0) {
                "Difficulty delta must not be negative."
            }
        }
    }
}