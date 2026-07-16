package vn.loi.learning.domain.study.session.model

/**
 * Chính sách giới hạn của một phiên học.
 */
data class SessionPolicy(
    val newItemLimit: Int = 20,
    val reviewItemLimit: Int = 100,
    val allowRepeatInSameSession: Boolean = false
) {

    init {
        require(newItemLimit >= 0) {
            "New item limit must not be negative."
        }

        require(reviewItemLimit >= 0) {
            "Review item limit must not be negative."
        }

        require(newItemLimit > 0 || reviewItemLimit > 0) {
            "A session must allow at least one new or review item."
        }
    }
}