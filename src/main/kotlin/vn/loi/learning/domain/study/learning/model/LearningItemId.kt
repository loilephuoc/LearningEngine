package vn.loi.learning.domain.study.learning.model

@JvmInline
value class LearningItemId(val value: String) {

    init {
        require(value.isNotBlank()) {
            "LearningItemId must not be blank."
        }
    }

    override fun toString(): String = value
}