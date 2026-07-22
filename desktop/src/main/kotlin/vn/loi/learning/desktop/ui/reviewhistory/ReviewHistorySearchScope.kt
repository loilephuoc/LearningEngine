package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.SearchScopePresentation
import vn.loi.learning.desktop.ui.search.presentSearchScope

fun reviewHistorySearchScope(state: ReviewHistoryUiState): SearchScopePresentation =
    presentSearchScope(
        noun = "review history",
        fields = listOf("Rating", "Reviewed time", "Response time", "Stability", "Difficulty"),
        query = state.query,
        filterLabel = state.filter.label,
        sortLabel = state.sort.label
    )
