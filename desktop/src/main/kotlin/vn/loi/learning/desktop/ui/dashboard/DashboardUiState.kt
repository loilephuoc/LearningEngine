package vn.loi.learning.desktop.ui.dashboard

import vn.loi.learning.desktop.ui.state.DesktopLoadState

/**
 * Immutable state của Dashboard UI.
 *
 * Chỉ chứa dữ liệu hiển thị, không chứa business logic.
 */
data class DashboardUiState(
    val loadState: DesktopLoadState = DesktopLoadState.Loading,
    val totalLearningItems: String = "--",
    val activeMemories: String = "--",
    val dueToday: String = "--",
    val dueNow: String = "--",
    val overdue: String = "--",
    val schedulingPressure: List<DashboardChartValue> = emptyList(),
    val newItems: String = "--",
    val learningItems: String = "--",
    val reviewItems: String = "--",
    val relearningItems: String = "--",
    val masteredItems: String = "--",
    val suspendedItems: String = "--",
    val memoryStageDistribution: List<DashboardChartValue> = emptyList(),
    val retention: String = "--",
    val retentionValue: Float? = null,
    val retentionEvaluated: String = "--",
    val forecastNextDay: String = "--",
    val forecastDaysTwoToThree: String = "--",
    val forecastDaysFourToSeven: String = "--",
    val forecastTotal: String = "--",
    val forecastBuckets: List<DashboardChartValue> = emptyList(),
    val totalReviews: String = "--",
    val activeDays: String = "--",
    val averageReviewsPerActiveDay: String = "--",
    val accuracy: String = "--",
    val ratingDistribution: List<DashboardChartValue> = emptyList(),
    val reviewHeatmapDays: List<DashboardHeatmapDay> = emptyList(),
    val studyStreak: String = "--",
    val lastStudy: String = "--"
)