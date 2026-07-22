package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.SearchResultStatusPresentation
import vn.loi.learning.desktop.ui.search.presentSearchResultStatus

fun reviewHistoryResultStatus(
    state: ReviewHistoryUiState
): SearchResultStatusPresentation =
    presentSearchResultStatus(
        summary = reviewHistorySearchSummary(state),
        noun = "review events",
        hasNonQueryRefinement =
            state.filter != ReviewHistoryFilter.ALL ||
                state.sort != ReviewHistorySort.NEWEST
    )
