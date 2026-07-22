package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.SearchRefinementPresentation
import vn.loi.learning.desktop.ui.search.SearchRefinementState
import vn.loi.learning.desktop.ui.search.presentSearchRefinements

fun ReviewHistoryUiState.refinementState(): SearchRefinementState =
    SearchRefinementState(
        hasQuery = query.isNotBlank(),
        hasFilter = filter != ReviewHistoryFilter.ALL,
        hasSort = sort != ReviewHistorySort.NEWEST
    )

fun reviewHistoryRefinementPresentation(
    state: ReviewHistoryUiState
): SearchRefinementPresentation =
    presentSearchRefinements(state.refinementState(), "review history")
