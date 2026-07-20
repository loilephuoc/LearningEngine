package vn.loi.learning.domain.study.memory.model

/**
 * Định danh duy nhất của một sự kiện review.
 */
@JvmInline
value class ReviewEventId(val value: String) {

    init {
        require(value.isNotBlank()) {
            "ReviewEventId must not be blank."
        }
    }

    override fun toString(): String = value
}