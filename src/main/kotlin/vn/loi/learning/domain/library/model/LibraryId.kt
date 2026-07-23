package vn.loi.learning.domain.library.model

/**
 * Định danh duy nhất cho Library Aggregate Root.
 */
@JvmInline
value class LibraryId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "LibraryId value must not be blank."
        }
    }

    override fun toString(): String = value
}
