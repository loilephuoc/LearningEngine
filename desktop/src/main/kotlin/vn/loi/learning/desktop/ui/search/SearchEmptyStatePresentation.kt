package vn.loi.learning.desktop.ui.search

data class SearchEmptyStatePresentation(
    val message: String,
    val contentDescription: String,
    val showClearQuery: Boolean,
    val showResetView: Boolean,
    val clearQueryDescription: String,
    val resetViewDescription: String
)

fun presentSearchEmptyState(
    message: String,
    noun: String,
    hasSourceItems: Boolean,
    hasQuery: Boolean,
    hasNonQueryRefinement: Boolean
): SearchEmptyStatePresentation {
    val normalizedMessage = message.trim().ifBlank { "No $noun are available." }

    if (!hasSourceItems) {
        return SearchEmptyStatePresentation(
            message = normalizedMessage,
            contentDescription = "$normalizedMessage No recovery action is available.",
            showClearQuery = false,
            showResetView = false,
            clearQueryDescription = "Clear the $noun search. No search query is active.",
            resetViewDescription = "Reset the $noun view. No refinements are active."
        )
    }

    val showClearQuery = hasQuery
    val showResetView = hasNonQueryRefinement
    val recoverySummary =
        when {
            showClearQuery && showResetView ->
                "Clear the search or reset every refinement to show more $noun."

            showClearQuery ->
                "Clear the search to show all $noun."

            showResetView ->
                "Reset the view to show all $noun."

            else ->
                "No recovery action is available."
        }

    return SearchEmptyStatePresentation(
        message = normalizedMessage,
        contentDescription = "$normalizedMessage $recoverySummary",
        showClearQuery = showClearQuery,
        showResetView = showResetView,
        clearQueryDescription = "Clear the $noun search and show results from the current filters.",
        resetViewDescription = "Reset the $noun search, filter, and sort to the default view."
    )
}
