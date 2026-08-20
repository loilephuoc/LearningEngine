package vn.loi.learning.android.dashboard

import java.time.LocalDate

/**
 * Scope selector for FSRS Forecast & Retention Insights.
 */
sealed interface AndroidInsightsScope {
    data object AllPackages : AndroidInsightsScope
    data class SpecificPackage(
        val packageId: String,
        val packageName: String
    ) : AndroidInsightsScope
}

/**
 * Dropdown item option for scope selection.
 */
data class AndroidInsightsScopeOption(
    val scope: AndroidInsightsScope,
    val label: String,
    val isActivePackage: Boolean = false
)

/**
 * Single day bucket in the 7-day review forecast.
 *
 * @param dayIndex 1 for Tomorrow, 2 for +2d, up to 7 for +7d.
 * @param date Local calendar date corresponding to this bucket.
 * @param label Human-readable label (e.g., "Tomorrow", "Thu", "Fri", "+2d").
 * @param count Number of distinct memory states scheduled for review on this local day.
 */
data class ForecastDayBucket(
    val dayIndex: Int,
    val date: LocalDate,
    val label: String,
    val count: Int
)

/**
 * Presentation-level memory retention distribution.
 *
 * NOTE: The 21-day threshold is an application presentation metric for dividing
 * young review items from mature/retained items, and is NOT a Core FSRS scheduler state.
 */
data class MemoryDistributionInsights(
    val newCount: Int,
    val learningCount: Int,
    val youngCount: Int,
    val retainedCount: Int,
    val totalCount: Int
) {
    val newPercent: Float get() = if (totalCount > 0) newCount.toFloat() / totalCount else 0f
    val learningPercent: Float get() = if (totalCount > 0) learningCount.toFloat() / totalCount else 0f
    val youngPercent: Float get() = if (totalCount > 0) youngCount.toFloat() / totalCount else 0f
    val retainedPercent: Float get() = if (totalCount > 0) retainedCount.toFloat() / totalCount else 0f
}

/**
 * Summary of today's ratings across all valid review sources (STANDARD_REVIEW, MANUAL_USER, etc.).
 */
data class TodayRatingsInsights(
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val totalCount: Int
)

/**
 * Aggregate immutable UI model for the FSRS Forecast & Retention Insights dashboard.
 */
data class AndroidForecastInsightsUiModel(
    val scope: AndroidInsightsScope,
    val availableScopes: List<AndroidInsightsScopeOption>,
    val forecast7Days: List<ForecastDayBucket>,
    val memoryDistribution: MemoryDistributionInsights,
    val todayRatings: TodayRatingsInsights
)
