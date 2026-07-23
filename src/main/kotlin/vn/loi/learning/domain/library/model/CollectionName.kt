package vn.loi.learning.domain.library.model

/**
 * Tên của một bộ sưu tập (Collection).
 */
data class CollectionName(val value: String) {
    val trimmedValue: String = value.trim()

    init {
        require(trimmedValue.isNotBlank()) {
            "CollectionName must not be blank."
        }
        require(trimmedValue.length <= 100) {
            "CollectionName length must not exceed 100 characters."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CollectionName) return false
        return trimmedValue.equals(other.trimmedValue, ignoreCase = true)
    }

    override fun hashCode(): Int = trimmedValue.lowercase().hashCode()

    override fun toString(): String = trimmedValue
}
