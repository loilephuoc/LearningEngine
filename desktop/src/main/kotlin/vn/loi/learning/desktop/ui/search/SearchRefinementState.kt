package vn.loi.learning.desktop.ui.search

data class SearchRefinementState(
    val hasQuery: Boolean,
    val hasFilter: Boolean,
    val hasSort: Boolean
) {
    val activeCount: Int
        get() = listOf(hasQuery, hasFilter, hasSort).count { it }

    val isDefault: Boolean
        get() = activeCount == 0
}
