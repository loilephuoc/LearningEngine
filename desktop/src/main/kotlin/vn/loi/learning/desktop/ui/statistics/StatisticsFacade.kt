package vn.loi.learning.desktop.ui.statistics

import java.util.Locale
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationContext

class StatisticsFacade(
    private val applicationContext: LearningApplicationContext,
    private val learnerId: LearnerId =
        LearnerId("default-learner"),
    private val currentTimeMillis: () -> Long =
        System::currentTimeMillis
) {

    fun loadUiState(): StatisticsUiState {
        val now = Moment(currentTimeMillis())

        val periodStart =
            Moment(
                (now.epochMillis - STATISTICS_WINDOW_MILLIS)
                    .coerceAtLeast(0L)
            )

        val statistics =
            applicationContext.statistics.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    period =
                        StudyPeriod(
                            startInclusive = periodStart,
                            endExclusive =
                                Moment(now.epochMillis + 1L)
                        )
                )
            )

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
                        "%.2f s",
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
            goodCount =
                statistics.goodCount.toString(),
            successRate =
                successRate,
            averageResponseTime =
                averageResponseTime
        )
    }

    private companion object {

        const val STATISTICS_WINDOW_MILLIS =
            30L * 24L * 60L * 60L * 1_000L
    }
}