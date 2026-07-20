package vn.loi.learning.application.contentpackaging

@JvmInline
value class ContentFingerprint(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "Content fingerprint must not be blank."
        }
    }

    override fun toString(): String = value
}