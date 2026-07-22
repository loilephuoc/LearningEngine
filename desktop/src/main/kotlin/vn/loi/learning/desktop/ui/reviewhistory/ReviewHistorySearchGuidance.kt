package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.SearchQueryGuidancePresentation
import vn.loi.learning.desktop.ui.search.presentSearchQueryGuidance

fun reviewHistorySearchGuidance(state: ReviewHistoryUiState): SearchQueryGuidancePresentation =
    presentSearchQueryGuidance(
        noun = "review history",
        examples = listOf("rating", "review date", "response time"),
        query = state.query
    )
