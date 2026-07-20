package vn.loi.learning.desktop.ui.statistics

data class StatisticsUiState(
    val totalReviews: String = "0",
    val successfulReviews: String = "0",
    val againCount: String = "0",
    val goodCount: String = "0",
    val successRate: String = "--",
    val averageResponseTime: String = "--"
)