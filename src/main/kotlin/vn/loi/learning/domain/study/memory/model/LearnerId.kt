package vn.loi.learning.domain.study.memory.model

@JvmInline
value class LearnerId(val value: String) {

    init {
        require(value.isNotBlank()) {
            "LearnerId must not be blank."
        }
    }

    override fun toString(): String = value
}