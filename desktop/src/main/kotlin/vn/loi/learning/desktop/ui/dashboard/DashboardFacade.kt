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
 * Facade kiông giữ UI state và không truy cập repository trực tiếp.
 */
class DashboardFacade(
    private val applicationContext: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    fun load(): DashboardUiState {
        val at = Moment(currentTimeMillis())
        val activityFrom = Moment(
            (at.epochMillis - ACTIVITY_WINDOW_MILLIS)
                .coerceAtLeast(0L)
        )

        val snapshot =
            applicationContext.dashboard.query(
                LearningDashboardQuery(
                    learnerId = learnerId,
                    activityFrom = activityFrom,
                    activityUntil =
                        at + TimeSpan.seconds(1),
                    at = at,
                    forecastWindowEnds =
                        listOf(
                            at + TimeSpan.seconds(SECONDS_PER_DAY),
                            at + TimeSpan.seconds(SECONDS_PER_DAY * 3),
                            at + TimeSpan.seconds(SECONDS_PER_DAY * 7)
                        )
                )
            )

        val retention =
            snapshot.retention.statistics
                .averageRetrievability
                ?.let { retrievability ->
                    String.format(
                        Locale.US,
                        "%.1f%%",
                        retrievability.value * 100.0
                    )
                }
                ?: "--"

        return DashboardUiState(
            totalLearningItems =
                snapshot.memory.totalMemories.toString(),
            dueToday =
                snapshot.scheduling.dueStatistics
                    .dueCount
                    .toString(),
            newItems =
                snapshot.memory.stageCounts
                    .newCount
                    .toString(),
            retention = retention,
            studyStreak = "--",
            lastStudy =
                if (snapshot.activity.progress.totalReviews > 0) {
                    "${snapshot.activity.progress.totalReviews} reviews"
                } else {
                    "--"
                }
        )
    }

    private companion object {

        const val SECONDS_PER_DAY = 86_400L
        const val ACTIVITY_WINDOW_MILLIS =
            SECONDS_PER_DAY * 30L * 1_000L
    }
}

