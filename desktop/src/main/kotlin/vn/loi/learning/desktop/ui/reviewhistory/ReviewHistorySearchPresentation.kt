package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.SearchResultSummary

fun reviewHistorySearchSummary(state: ReviewHistoryUiState) = SearchResultSummary(state.visibleItems.size,state.items.size,state.query)
fun reviewHistoryEmptySearchMessage(state: ReviewHistoryUiState): String = when {
    state.items.isEmpty() -> "Complete a study review to create your first history entry."
    state.query.isNotBlank() -> "No review events match \"${state.query.trim()}\". Clear the search or change the rating filter."
    state.filter != ReviewHistoryFilter.ALL -> "No ${state.filter.label} review events are available."
    else -> "No review events are available."
}
