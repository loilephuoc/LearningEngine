package vn.loi.learning.domain.study.session.model

@JvmInline
value class SessionId(val value: String) {

    init {
        require(value.isNotBlank()) {
            "SessionId must not be blank."
        }
    }

    override fun toString(): String = value
}