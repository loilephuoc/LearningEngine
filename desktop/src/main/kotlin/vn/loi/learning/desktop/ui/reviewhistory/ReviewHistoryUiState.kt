package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.state.DesktopLoadState

data class ReviewHistoryUiState(
    val loadState: DesktopLoadState = DesktopLoadState.Loading,
    val items: List<ReviewHistoryItemUi> = emptyList()
)

data class ReviewHistoryItemUi(
    val reviewedAt: String,
    val rating: String,
    val responseTime: String,
    val stability: String,
    val difficulty: String
)