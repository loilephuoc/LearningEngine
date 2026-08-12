package vn.loi.learning.desktop.ui.dashboard

import java.util.Locale
import vn.loi.learning.application.learningdashboard.LearningDashboardQuery
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Desktop adapter tải Learning Dashboard và chuyển Application snapshot
 * thành immutable DashboardUiState dành cho Compose.
 *
 * Facade không giữ UI state và không truy cập repository trực tiếp.
 */
class DashboardFacade(
    private val applicationContext: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    fun load(): DashboardUiState {
        val currentMillis = currentTimeMillis()
        val at = Moment(currentMillis)

        val activityFrom =
            Moment(
                (at.epochMillis - ACTIVITY_WINDOW_MILLIS)
                    .coerceAtLeast(0L)
            )

        val snapshot =
            applicationContext.dashboard.queryHome(
                LearningDashboardQuery(
                    learnerId = learnerId,
                    activityFrom = activityFrom,
                    activityUntil = at + TimeSpan.seconds(1),
                    at = at,
                    forecastWindowEnds =
                        listOf(
                            at + TimeSpan.seconds(SECONDS_PER_DAY),
                            at + TimeSpan.seconds(SECONDS_PER_DAY * 3),
                            at + TimeSpan.seconds(SECONDS_PER_DAY * 7)
                        )
                )
            )

        val activity = snapshot.activity.progress
        val memory = snapshot.memory.stageCounts
        val dueStatistics = snapshot.scheduling.dueStatistics
        val retentionStatistics = snapshot.retention.statistics
        return DashboardUiState(
            totalLearningItems =
                snapshot.memory.totalMemories.toString(),
            activeMemories =
                snapshot.memory.activeMemories.toString(),
            dueToday =
                dueStatistics.dueCount.toString(),
            dueNow =
                dueStatistics.dueNowCount.toString(),
            overdue =
                dueStatistics.overdueCount.toString(),
            schedulingPressure =
                listOf(
                    DashboardChartValue(
                        label = "Due total",
                        value = dueStatistics.dueCount
                    ),
                    DashboardChartValue(
                        label = "Due now",
                        value = dueStatistics.dueNowCount
                    ),
                    DashboardChartValue(
                        label = "Overdue",
                        value = dueStatistics.overdueCount
                    )
                ),
            newItems =
                memory.newCount.toString(),
            learningItems =
                memory.learningCount.toString(),
            reviewItems =
                memory.reviewCount.toString(),
            relearningItems =
                memory.relearningCount.toString(),
            masteredItems =
                memory.masteredCount.toString(),
            suspendedItems =
                memory.suspendedCount.toString(),
            memoryStageDistribution =
                listOf(
                    DashboardChartValue(
                        label = "New",
                        value = memory.newCount
                    ),
                    DashboardChartValue(
                        label = "Learning",
                        value = memory.learningCount
                    ),
                    DashboardChartValue(
                        label = "Review",
                        value = memory.reviewCount
                    ),
                    DashboardChartValue(
                        label = "Relearning",
                        value = memory.relearningCount
                    ),
                    DashboardChartValue(
                        label = "Mastered",
                        value = memory.masteredCount
                    ),
                    DashboardChartValue(
                        label = "Suspended",
                        value = memory.suspendedCount
                    )
                ),
            retention =
                retentionStatistics.averageRetrievability
                    ?.let { retrievability ->
                        formatPercentage(
                            value = retrievability.value
                        )
                    }
                    ?: "--",
            retentionValue =
                retentionStatistics.averageRetrievability
                    ?.value
                    ?.toFloat(),
            retentionEvaluated =
                retentionStatistics.evaluatedMemoryCount
                    .toString(),
            totalReviews =
                activity.totalReviews.toString(),
            activeDays =
                activity.activeDays.toString(),
            averageReviewsPerActiveDay =
                activity.averageReviewsPerActiveDay
                    ?.let(::formatDecimal)
                    ?: "--",
            accuracy =
                activity.accuracy
                    ?.let(::formatPercentage)
                    ?: "--",
            ratingDistribution =
                listOf(
                    DashboardChartValue(
                        label = "Again",
                        value = activity.againCount
                    ),
                    DashboardChartValue(
                        label = "Hard",
                        value = activity.hardCount
                    ),
                    DashboardChartValue(
                        label = "Good",
                        value = activity.goodCount
                    ),
                    DashboardChartValue(
                        label = "Easy",
                        value = activity.easyCount
                    )
                ),
            // Home deliberately avoids the additional history scan required for a streak.
            studyStreak = "--"
        )
    }

    private fun formatPercentage(
        value: Double
    ): String =
        String.format(
            Locale.US,
            "%.1f%%",
            value * 100.0
        )

    private fun formatDecimal(
        value: Double
    ): String =
        String.format(
            Locale.US,
            "%.1f",
            value
        )

    private companion object {

        const val SECONDS_PER_DAY = 86_400L

        const val ACTIVITY_WINDOW_MILLIS =
            SECONDS_PER_DAY * 30L * 1_000L

    }
}
