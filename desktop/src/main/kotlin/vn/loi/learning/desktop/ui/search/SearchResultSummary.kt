package vn.loi.learning.desktop.ui.search

data class SearchResultSummary(
    val visibleCount: Int,
    val totalCount: Int,
    val query: String
) {
    val label: String
        get() =
            if (query.isBlank()) {
                "$totalCount results"
            } else {
                "$visibleCount of $totalCount results"
            }

    val contentDescription: String
        get() =
            if (query.isBlank()) {
                "$totalCount results available."
            } else {
                "$visibleCount of $totalCount results match ${query.trim()}."
            }
}
