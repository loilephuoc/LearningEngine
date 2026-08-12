package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.state.DesktopLoadState

data class ReviewHistoryUiState(
    val loadState: DesktopLoadState = DesktopLoadState.Loading,
    val selectedTab: ReviewCenterTab = ReviewCenterTab.QUICK_REVIEW,
    val historyLoaded: Boolean = false,
    val items: List<ReviewHistoryItemUi> = emptyList(),
    val query: String = "",
    val filter: ReviewHistoryFilter = ReviewHistoryFilter.ALL,
    val sort: ReviewHistorySort = ReviewHistorySort.NEWEST
) {
    val visibleItems: List<ReviewHistoryItemUi>
        get() = projectReviewHistory(items, query, filter, sort)
}

enum class ReviewCenterTab { QUICK_REVIEW, HISTORY }

data class ReviewHistoryItemUi(
    val reviewedAt: String,
    val rating: String,
    val responseTime: String,
    val stability: String,
    val difficulty: String
)
