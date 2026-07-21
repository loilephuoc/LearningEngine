package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.state.DesktopLoadState

data class ReviewHistoryUiState(
    val loadState: DesktopLoadState = DesktopLoadState.Loading,
    val items: List<ReviewHistoryItemUi> = emptyList(),
    val query: String = "",
    val filter: ReviewHistoryFilter = ReviewHistoryFilter.ALL,
    val sort: ReviewHistorySort = ReviewHistorySort.NEWEST
) {
    val visibleItems: List<ReviewHistoryItemUi>
        get() = projectReviewHistory(items, query, filter, sort)
}

data class ReviewHistoryItemUi(
    val reviewedAt: String,
    val rating: String,
    val responseTime: String,
    val stability: String,
    val difficulty: String
)