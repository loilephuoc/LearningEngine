package vn.loi.learning.desktop.ui.statistics

import java.util.Locale
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import vn.loi.learning.application.learningdashboard.LearningDashboardQuery
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.desktop.ui.dashboard.DashboardChartValue
import vn.loi.learning.desktop.ui.dashboard.DashboardHeatmapDay
import vn.loi.learning.desktop.ui.dashboard.DashboardUiState
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.content.model.ContentId

class StatisticsFacade(
    private val applicationContext: LearningApplicationContext,
    private val learnerId: LearnerId =
        LearnerId("default-learner"),
    private val currentTimeMillis: () -> Long =
        System::currentTimeMillis
) {

    fun loadUiState(selectedPackageId: InstalledPackageId? = null): StatisticsUiState {
        val now = Moment(currentTimeMillis())
        val installedPackageOptions = applicationContext.defaultLibraryId?.let { libraryId ->
            applicationContext.libraryQuery?.getInstalledPackages(libraryId)
                ?.filter { it.isActive }
                ?.map { StatisticsScopeOption(it.id, it.name) }
        } ?: applicationContext.installedPackages.query()
            .map { StatisticsScopeOption(InstalledPackageId(it.id), it.name) }
        val scopeOptions = listOf(StatisticsScopeOption(null, "Tất cả nội dung đã học")) + installedPackageOptions
        val scopedItemIds = selectedPackageId?.let { packageId ->
            val packageContentQuery = requireNotNull(applicationContext.packageContentQuery) {
                "Package content query is unavailable."
            }
            val contentIds = packageContentQuery.getContentDescriptorsForPackage(packageId)
                .mapTo(linkedSetOf()) { ContentId(it.id) }
            applicationContext.engine.getLearningItemsByContentIds(contentIds).mapTo(linkedSetOf()) { it.id }
        }

        val periodStart =
            Moment(
                (now.epochMillis - STATISTICS_WINDOW_MILLIS)
                    .coerceAtLeast(0L)
            )

        val statistics =
            applicationContext.statistics.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    learningItemIds = scopedItemIds,
                    period =
                        StudyPeriod(
                            startInclusive = periodStart,
                            endExclusive =
                                Moment(now.epochMillis + 1L)
                        )
                )
            )

        val dashboardQuery = LearningDashboardQuery(
                learnerId = learnerId,
                activityFrom = periodStart,
                activityUntil = Moment(now.epochMillis + 1L),
                at = now,
                forecastWindowEnds = listOf(
                    now + TimeSpan.seconds(86_400),
                    now + TimeSpan.seconds(86_400 * 3),
                    now + TimeSpan.seconds(86_400 * 7)
                )
            )
        val dashboard = scopedItemIds?.let { applicationContext.dashboard.query(dashboardQuery, it) }
            ?: applicationContext.dashboard.query(dashboardQuery)
        val heatmap = loadHeatmap(now.epochMillis, scopedItemIds)
        val activeDays30 = heatmap.takeLast(30).count { it.reviewCount > 0 }
        val activity = dashboard.activity.progress
        val memory = dashboard.memory.stageCounts
        val due = dashboard.scheduling.dueStatistics
        val retention = dashboard.retention.statistics
        val buckets = dashboard.forecast.forecast.buckets

        val successRate =
            statistics.successfulReviewProportion
                ?.let { proportion ->
                    String.format(
                        Locale.US,
                        "%.1f%%",
                        proportion * 100.0
                    )
                }
                ?: "--"

        val averageResponseTime =
            statistics.averageResponseTimeMillis
                ?.let { milliseconds ->
                    String.format(
                        Locale.US,
                        "%.1f s",
                        milliseconds / 1_000.0
                    )
                }
                ?: "--"

        return StatisticsUiState(
            totalReviews =
                statistics.totalReviews.toString(),
            successfulReviews =
                statistics.successfulReviewCount.toString(),
            againCount =
                statistics.againCount.toString(),
            hardCount = statistics.hardCount.toString(),
            goodCount =
                statistics.goodCount.toString(),
            easyCount = statistics.easyCount.toString(),
            successRate =
                successRate,
            averageResponseTime = averageResponseTime,
            analytics = DashboardUiState(
                activeMemories = dashboard.memory.activeMemories.toString(),
                dueToday = due.dueCount.toString(),
                dueNow = due.dueNowCount.toString(),
                overdue = due.overdueCount.toString(),
                schedulingPressure = listOf(
                    DashboardChartValue("Tổng đến hạn", due.dueCount),
                    DashboardChartValue("Có thể ôn ngay", due.dueNowCount),
                    DashboardChartValue("Quá hạn", due.overdueCount)
                ),
                newItems = memory.newCount.toString(),
                learningItems = memory.learningCount.toString(),
                reviewItems = memory.reviewCount.toString(),
                relearningItems = memory.relearningCount.toString(),
                masteredItems = memory.masteredCount.toString(),
                suspendedItems = memory.suspendedCount.toString(),
                memoryStageDistribution = listOf(
                    DashboardChartValue("Mới", memory.newCount),
                    DashboardChartValue("Đang học", memory.learningCount),
                    DashboardChartValue("Ôn tập", memory.reviewCount),
                    DashboardChartValue("Học lại", memory.relearningCount),
                    DashboardChartValue("Thành thạo", memory.masteredCount),
                    DashboardChartValue("Tạm dừng", memory.suspendedCount)
                ),
                retention = retention.averageRetrievability?.value?.let(::formatPercent) ?: "--",
                retentionValue = retention.averageRetrievability?.value?.toFloat(),
                retentionEvaluated = retention.evaluatedMemoryCount.toString(),
                forecastNextDay = (buckets.getOrNull(0)?.dueCount ?: 0).toString(),
                forecastDaysTwoToThree = (buckets.getOrNull(1)?.dueCount ?: 0).toString(),
                forecastDaysFourToSeven = (buckets.getOrNull(2)?.dueCount ?: 0).toString(),
                forecastTotal = dashboard.forecast.forecast.totalDueCount.toString(),
                forecastBuckets = listOf(
                    DashboardChartValue("Ngày tiếp theo", buckets.getOrNull(0)?.dueCount ?: 0),
                    DashboardChartValue("Ngày 2–3", buckets.getOrNull(1)?.dueCount ?: 0),
                    DashboardChartValue("Ngày 4–7", buckets.getOrNull(2)?.dueCount ?: 0)
                ),
                totalReviews = activity.totalReviews.toString(),
                activeDays = activeDays30.toString(),
                averageReviewsPerActiveDay = if (activeDays30 == 0) "--" else String.format(Locale.US, "%.1f", activity.totalReviews.toDouble() / activeDays30),
                accuracy = activity.accuracy?.let(::formatPercent) ?: "--",
                ratingDistribution = listOf(
                    DashboardChartValue("Again", activity.againCount),
                    DashboardChartValue("Hard", activity.hardCount),
                    DashboardChartValue("Good", activity.goodCount),
                    DashboardChartValue("Easy", activity.easyCount)
                ),
                reviewHeatmapDays = heatmap,
                studyStreak = calculateStreak(heatmap).toString(),
                lastStudy = lastReview(heatmap)
            ),
            scopeOptions = scopeOptions,
            selectedPackageId = selectedPackageId
        )
    }

    private fun loadHeatmap(nowMillis: Long, learningItemIds: Set<vn.loi.learning.domain.study.learning.model.LearningItemId>?): List<DashboardHeatmapDay> {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val first = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11)
        val events = applicationContext.reviewHistory.query(
            ReviewHistoryQuery(learnerId = learnerId, learningItemIds = learningItemIds, period = StudyPeriod(
                Moment(first.atStartOfDay(zone).toInstant().toEpochMilli()),
                Moment(today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
            ))
        )
        val counts = events.groupingBy { Instant.ofEpochMilli(it.reviewedAt.epochMillis).atZone(zone).toLocalDate() }.eachCount()
        return List(84) { index ->
            val date = first.plusDays(index.toLong())
            DashboardHeatmapDay(date.toEpochDay(), counts[date] ?: 0, date > today)
        }
    }

    private fun calculateStreak(days: List<DashboardHeatmapDay>): Int {
        val today = java.time.LocalDate.now()
        val counts = days.associate { it.epochDay to it.reviewCount }
        var cursor = if ((counts[today.toEpochDay()] ?: 0) > 0) today else today.minusDays(1)
        var streak = 0
        while ((counts[cursor.toEpochDay()] ?: 0) > 0) { streak++; cursor = cursor.minusDays(1) }
        return streak
    }

    private fun lastReview(days: List<DashboardHeatmapDay>): String =
        days.lastOrNull { !it.isFuture && it.reviewCount > 0 }
            ?.let { java.time.LocalDate.ofEpochDay(it.epochDay).toString() } ?: "--"

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value * 100.0)

    private companion object {

        const val STATISTICS_WINDOW_MILLIS =
            30L * 24L * 60L * 60L * 1_000L
    }
}
