package vn.loi.learning.domain.study.fsrs.calibration.model

/**
 * Định danh duy nhất của một FSRS Parameter Profile.
 */
@JvmInline
value class FsrsParameterProfileId(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "FSRS parameter profile ID must not be blank."
        }

        require(value == value.trim()) {
            "FSRS parameter profile ID must not contain surrounding whitespace."
        }
    }

    override fun toString(): String =
        value
}