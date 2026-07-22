package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.SearchOptionGroupPresentation
import vn.loi.learning.desktop.ui.search.SearchOptionKind
import vn.loi.learning.desktop.ui.search.presentSearchOptionGroup

fun reviewHistoryFilterPresentation(state: ReviewHistoryUiState): SearchOptionGroupPresentation =
    presentSearchOptionGroup(
        noun = "review history",
        kind = SearchOptionKind.FILTER,
        options = ReviewHistoryFilter.entries.map { it.label },
        selectedLabel = state.filter.label
    )

fun reviewHistorySortPresentation(state: ReviewHistoryUiState): SearchOptionGroupPresentation =
    presentSearchOptionGroup(
        noun = "review history",
        kind = SearchOptionKind.SORT,
        options = ReviewHistorySort.entries.map { it.label },
        selectedLabel = state.sort.label
    )
