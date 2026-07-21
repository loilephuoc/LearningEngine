package vn.loi.learning.desktop.ui.dashboard

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import vn.loi.learning.application.learningdashboard.LearningDashboardQuery
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
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
            applicationContext.dashboard.query(
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
        val forecast = snapshot.forecast.forecast
        val forecastBuckets = forecast.buckets

        val nextDayForecast =
            forecastBuckets
                .getOrNull(0)
                ?.dueCount
                ?: 0

        val daysTwoToThreeForecast =
            forecastBuckets
                .getOrNull(1)
                ?.dueCount
                ?: 0

        val daysFourToSevenForecast =
            forecastBuckets
                .getOrNull(2)
                ?.dueCount
                ?: 0

        val reviewHeatmapDays =
            loadReviewHeatmap(
                currentMillis = currentMillis
            )

        val today =
            Instant
                .ofEpochMilli(currentMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

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
            forecastNextDay =
                nextDayForecast.toString(),
            forecastDaysTwoToThree =
                daysTwoToThreeForecast.toString(),
            forecastDaysFourToSeven =
                daysFourToSevenForecast.toString(),
            forecastTotal =
                forecast.totalDueCount.toString(),
            forecastBuckets =
                listOf(
                    DashboardChartValue(
                        label = "Next day",
                        value = nextDayForecast
                    ),
                    DashboardChartValue(
                        label = "Days 2–3",
                        value = daysTwoToThreeForecast
                    ),
                    DashboardChartValue(
                        label = "Days 4–7",
                        value = daysFourToSevenForecast
                    )
                ),
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
            reviewHeatmapDays = reviewHeatmapDays,
            studyStreak =
                calculateCurrentStreak(
                    days = reviewHeatmapDays,
                    today = today
                ).toString(),
            lastStudy =
                formatLastReview(
                    days = reviewHeatmapDays,
                    today = today
                )
        )
    }

    private fun loadReviewHeatmap(
        currentMillis: Long
    ): List<DashboardHeatmapDay> {
        val zoneId = ZoneId.systemDefault()

        val today =
            Instant
                .ofEpochMilli(currentMillis)
                .atZone(zoneId)
                .toLocalDate()

        val currentWeekMonday =
            today.with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.MONDAY
                )
            )

        val firstDate =
            currentWeekMonday.minusWeeks(
                HEATMAP_WEEK_COUNT - 1L
            )

        val queryEndDate =
            today.plusDays(1)

        val events =
            applicationContext.reviewHistory.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    period =
                        StudyPeriod(
                            startInclusive =
                                Moment(
                                    firstDate
                                        .atStartOfDay(zoneId)
                                        .toInstant()
                                        .toEpochMilli()
                                ),
                            endExclusive =
                                Moment(
                                    queryEndDate
                                        .atStartOfDay(zoneId)
                                        .toInstant()
                                        .toEpochMilli()
                                )
                        )
                )
            )

        val reviewCountByDate =
            events
                .groupingBy { event ->
                    Instant
                        .ofEpochMilli(
                            event.reviewedAt.epochMillis
                        )
                        .atZone(zoneId)
                        .toLocalDate()
                }
                .eachCount()

        return List(
            HEATMAP_WEEK_COUNT * DAYS_PER_WEEK
        ) { index ->
            val date =
                firstDate.plusDays(index.toLong())

            DashboardHeatmapDay(
                epochDay = date.toEpochDay(),
                reviewCount =
                    reviewCountByDate[date] ?: 0,
                isFuture = date > today
            )
        }
    }

    /**
     * Chuỗi hiện tại được tính đến hôm nay.
     *
     * Nếu hôm nay chưa có review nhưng hôm qua có,
     * chuỗi vẫn được giữ vì ngày hôm nay chưa kết thúc.
     */
    private fun calculateCurrentStreak(
        days: List<DashboardHeatmapDay>,
        today: LocalDate
    ): Int {
        val reviewCountByEpochDay =
            days.associate { day ->
                day.epochDay to day.reviewCount
            }

        val todayCount =
            reviewCountByEpochDay[today.toEpochDay()]
                ?: 0

        var cursor =
            if (todayCount > 0) {
                today
            } else {
                today.minusDays(1)
            }

        var streak = 0

        while (
            (reviewCountByEpochDay[cursor.toEpochDay()] ?: 0) > 0
        ) {
            streak += 1
            cursor = cursor.minusDays(1)
        }

        return streak
    }

    private fun formatLastReview(
        days: List<DashboardHeatmapDay>,
        today: LocalDate
    ): String {
        val lastReviewDate =
            days
                .asSequence()
                .filter { day ->
                    !day.isFuture &&
                            day.reviewCount > 0
                }
                .maxByOrNull { day ->
                    day.epochDay
                }
                ?.let { day ->
                    LocalDate.ofEpochDay(day.epochDay)
                }
                ?: return "--"

        return when (lastReviewDate) {
            today ->
                "Today"

            today.minusDays(1) ->
                "Yesterday"

            else ->
                lastReviewDate.format(
                    LAST_REVIEW_DATE_FORMATTER
                )
        }
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

        const val HEATMAP_WEEK_COUNT = 12

        const val DAYS_PER_WEEK = 7

        val LAST_REVIEW_DATE_FORMATTER:
                DateTimeFormatter =
            DateTimeFormatter.ofPattern(
                "MMM d",
                Locale.US
            )
    }
}