package vn.loi.learning.domain.study.fsrs.model

/**
 * Immutable set of parameters used by the FSRS v6 model.
 *
 * The internal array is copied on creation so callers cannot mutate
 * the parameter set after construction.
 */
class FsrsParameters private constructor(
    values: DoubleArray
) {

    private val values: DoubleArray = values.copyOf()

    /** Returns the parameter at [index]. */
    operator fun get(index: Int): Double = values[index]

    /** Number of parameters in this set. */
    val size: Int
        get() = values.size

    companion object {

        const val PARAMETER_COUNT: Int = 21

        /**
         * Creates an immutable FSRS parameter set.
         *
         * @throws IllegalArgumentException when [values] does not contain
         * exactly [PARAMETER_COUNT] entries.
         */
        fun of(values: DoubleArray): FsrsParameters {
            require(values.size == PARAMETER_COUNT) {
                "FSRS requires exactly $PARAMETER_COUNT parameters, but was ${values.size}."
            }

            return FsrsParameters(values)
        }

        /** Official default parameter set used by FSRS v6. */
        val DEFAULT: FsrsParameters = of(
            doubleArrayOf(
                0.2120,
                1.2931,
                2.3065,
                8.2956,
                6.4133,
                0.8334,
                3.0194,
                0.0010,
                1.8722,
                0.1666,
                0.7960,
                1.4835,
                0.0614,
                0.2629,
                1.6483,
                0.6014,
                1.8729,
                0.5425,
                0.0912,
                0.0658,
                0.1542
            )
        )
    }
}
