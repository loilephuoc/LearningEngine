package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.SearchEmptyStatePresentation
import vn.loi.learning.desktop.ui.search.SearchResultSummary
import vn.loi.learning.desktop.ui.search.presentSearchEmptyState

fun reviewHistorySearchSummary(state: ReviewHistoryUiState) =
    SearchResultSummary(
        visibleCount = state.visibleItems.size,
        totalCount = state.items.size,
        query = state.query
    )

fun reviewHistoryEmptySearchMessage(state: ReviewHistoryUiState): String =
    when {
        state.items.isEmpty() ->
            "Complete a study review to create your first history entry."

        state.query.isNotBlank() ->
            "No review events match \"${state.query.trim()}\". Clear the search or change the rating filter."

        state.filter != ReviewHistoryFilter.ALL ->
            "No ${state.filter.label} review events are available."

        else ->
            "No review events are available."
    }

fun reviewHistoryEmptySearchPresentation(
    state: ReviewHistoryUiState
): SearchEmptyStatePresentation =
    presentSearchEmptyState(
        message = reviewHistoryEmptySearchMessage(state),
        noun = "review events",
        hasSourceItems = state.items.isNotEmpty(),
        hasQuery = state.query.isNotBlank(),
        hasNonQueryRefinement =
            state.filter != ReviewHistoryFilter.ALL ||
                state.sort != ReviewHistorySort.NEWEST
    )
