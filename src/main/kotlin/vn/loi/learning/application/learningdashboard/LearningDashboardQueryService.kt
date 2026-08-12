package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.progress.LearningProgressQuery
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.learning.model.LearningItemId

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
    private val memoryStateQuery: MemoryStateQuery,
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

    fun queryHome(
        query: LearningDashboardQuery
    ): LearningHomeSnapshot {
        val memoryStates = memoryStateQuery.findAll(query.learnerId)
        return LearningHomeSnapshot(
            activity = queryActivity(query),
            memory = memoryQueryService.query(memoryStates),
            scheduling = schedulingQueryService.query(memoryStates, query.at),
            retention = retentionQueryService.query(memoryStates, query.at)
        )
    }

    fun query(
        query: LearningDashboardQuery
    ): LearningDashboardSnapshot {
        val activity = queryActivity(query)

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

    fun query(
        query: LearningDashboardQuery,
        learningItemIds: Set<LearningItemId>
    ): LearningDashboardSnapshot {
        val memoryStates = memoryStateQuery.findAll(query.learnerId)
            .filter { it.learningItemId in learningItemIds }
        val activity = activityQueryService.query(
            LearningProgressQuery(
                learnerId = query.learnerId,
                activityPeriod = StudyPeriod(query.activityFrom, query.activityUntil),
                evaluatedAt = query.at,
                dailyPeriods = emptyList(),
                learningItemIds = learningItemIds
            )
        )
        return LearningDashboardSnapshot(
            activity = activity,
            memory = memoryQueryService.query(memoryStates),
            scheduling = schedulingQueryService.query(memoryStates, query.at),
            retention = retentionQueryService.query(memoryStates, query.at),
            forecast = forecastQueryService.query(memoryStates, query.at, query.forecastWindowEnds)
        )
    }

    private fun queryActivity(query: LearningDashboardQuery) =
        activityQueryService.query(
            LearningProgressQuery(
                learnerId = query.learnerId,
                activityPeriod = StudyPeriod(query.activityFrom, query.activityUntil),
                evaluatedAt = query.at,
                dailyPeriods = emptyList()
            )
        )
}
