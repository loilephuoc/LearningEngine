package vn.loi.learning.domain.library.model

/**
 * Định danh duy nhất cho một bộ sưu tập (Collection Aggregate Root).
 */
@JvmInline
value class CollectionId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "CollectionId value must not be blank."
        }
    }

    override fun toString(): String = value
}
