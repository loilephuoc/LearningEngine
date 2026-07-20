package vn.loi.learning.desktop.ui.reviewhistory

data class ReviewHistoryUiState(
    val items: List<ReviewHistoryItemUi> = emptyList()
)

data class ReviewHistoryItemUi(
    val reviewedAt: String,
    val rating: String,
    val responseTime: String,
    val stability: String,
    val difficulty: String
)