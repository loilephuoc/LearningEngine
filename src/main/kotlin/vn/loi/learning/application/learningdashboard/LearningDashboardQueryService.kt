package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.progress.LearningProgressQuery
import vn.loi.learning.domain.study.analytics.model.StudyPeriod

/**
 * Application service điều phối toàn bộ
 * Learning Dashboard.
 *
 * Service này tạo năm section:
 * - Activity;
 * - Memory;
 * - Scheduling;
 * - Retention;
 * - Forecast.
 *
 * Mọi phép tính nghiệp vụ được giao cho:
 * - các section query service;
 * - các calculator chuyên biệt phía sau chúng.
 *
 * Service này không:
 * - truy cập repository trực tiếp;
 * - tự đọc MemoryState hoặc ReviewEvent;
 * - tự tính due, overdue hoặc retention;
 * - tự chia forecast bucket;
 * - tự đọc đồng hồ hệ thống;
 * - tự xác định ngày hoặc múi giờ.
 */
class LearningDashboardQueryService(
    private val activityQueryService:
    LearningDashboardActivityQueryService,
    private val memoryQueryService:
    LearningDashboardMemoryQueryService,
    private val schedulingQueryService:
    LearningDashboardSchedulingQueryService,
    private val retentionQueryService:
    LearningDashboardRetentionQueryService,
    private val forecastQueryService:
    LearningDashboardForecastQueryService
) {

    fun query(
        query: LearningDashboardQuery
    ): LearningDashboardSnapshot {
        val activity =
            activityQueryService.query(
                LearningProgressQuery(
                    learnerId = query.learnerId,
                    activityPeriod =
                        StudyPeriod(
                            startInclusive =
                                query.activityFrom,
                            endExclusive =
                                query.activityUntil
                        ),
                    evaluatedAt =
                        query.at,
                    dailyPeriods =
                        emptyList()
                )
            )

        val memory =
            memoryQueryService.query(
                learnerId = query.learnerId
            )

        val scheduling =
            schedulingQueryService.query(
                learnerId = query.learnerId,
                at = query.at
            )

        val retention =
            retentionQueryService.query(
                learnerId = query.learnerId,
                at = query.at
            )

        val forecast =
            forecastQueryService.query(
                learnerId = query.learnerId,
                forecastStart = query.at,
                windowEnds =
                    query.forecastWindowEnds
            )

        return LearningDashboardSnapshot(
            activity = activity,
            memory = memory,
            scheduling = scheduling,
            retention = retention,
            forecast = forecast
        )
    }
}