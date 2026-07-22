package vn.loi.learning.desktop.ui.search

data class SearchResultStatusPresentation(
    val label: String,
    val contentDescription: String,
    val isFiltered: Boolean,
    val hasResults: Boolean
)

fun presentSearchResultStatus(
    summary: SearchResultSummary,
    noun: String,
    hasNonQueryRefinement: Boolean
): SearchResultStatusPresentation {
    require(noun.isNotBlank()) { "noun must not be blank" }
    val isFiltered = summary.query.isNotBlank() || hasNonQueryRefinement
    val label =
        when {
            summary.totalCount == 0 -> "No $noun yet"
            !isFiltered -> "${summary.totalCount} $noun"
            summary.visibleCount == 0 -> "No matching $noun"
            else -> "${summary.visibleCount} of ${summary.totalCount} $noun"
        }
    val description =
        when {
            summary.totalCount == 0 -> "No $noun are available."
            !isFiltered -> "All ${summary.totalCount} $noun are shown."
            summary.visibleCount == 0 -> "No $noun match the active search options."
            else -> "Showing ${summary.visibleCount} of ${summary.totalCount} $noun after applying search options."
        }
    return SearchResultStatusPresentation(
        label = label,
        contentDescription = description,
        isFiltered = isFiltered,
        hasResults = summary.visibleCount > 0
    )
}
