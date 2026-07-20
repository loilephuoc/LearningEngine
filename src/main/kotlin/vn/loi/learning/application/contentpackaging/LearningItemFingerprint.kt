package vn.loi.learning.application.contentpackaging

@JvmInline
value class LearningItemFingerprint(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "Learning item fingerprint must not be blank."
        }
    }

    override fun toString(): String = value
}