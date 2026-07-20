package vn.loi.learning.infrastructure

import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.learningdashboard.LearningDashboardActivityQueryService
import vn.loi.learning.application.learningdashboard.LearningDashboardDueStatisticsCalculator
import vn.loi.learning.application.learningdashboard.LearningDashboardForecastCalculator
import vn.loi.learning.application.learningdashboard.LearningDashboardForecastQueryService
import vn.loi.learning.application.learningdashboard.LearningDashboardMemoryQueryService
import vn.loi.learning.application.learningdashboard.LearningDashboardQueryService
import vn.loi.learning.application.learningdashboard.LearningDashboardRetentionQueryService
import vn.loi.learning.application.learningdashboard.LearningDashboardRetentionStatisticsCalculator
import vn.loi.learning.application.learningdashboard.LearningDashboardSchedulingQueryService
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.progress.LearningProgressQueryService
import vn.loi.learning.application.progress.LearningStageCountsCalculator
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.domain.study.analytics.service.StudyStatisticsCalculator
import vn.loi.learning.domain.study.memory.ForgettingCurve
import vn.loi.learning.domain.study.memory.FsrsForgettingCurve

/**
 * Composition root cho toàn bộ Learning Dashboard query stack.
 *
 * Factory này:
 * - lắp ráp các application query service và calculator hiện có;
 * - sử dụng chung một MemoryStateQuery cho các section
 *   Memory, Scheduling, Retention và Forecast;
 * - sử dụng chung ReviewEventRepository cho Activity stack;
 * - chọn FsrsForgettingCurve làm production default;
 * - không truy cập persistence trực tiếp;
 * - không chứa business calculation;
 * - không đọc đồng hồ hệ thống.
 *
 * Infrastructure adapter cụ thể được tạo bên ngoài và truyền vào
 * thông qua các port.
 */
object LearningDashboardQueryServiceFactory {

    fun create(
        memoryStateQuery: MemoryStateQuery,
        reviewEventRepository: ReviewEventRepository,
        forgettingCurve: ForgettingCurve =
            FsrsForgettingCurve()
    ): LearningDashboardQueryService {
        val reviewHistoryQueryService =
            ReviewHistoryQueryService(
                reviewEventRepository =
                    reviewEventRepository
            )

        val studyStatisticsQueryService =
            StudyStatisticsQueryService(
                reviewHistoryQueryService =
                    reviewHistoryQueryService,
                studyStatisticsCalculator =
                    StudyStatisticsCalculator()
            )

        val learningProgressQueryService =
            LearningProgressQueryService(
                studyStatisticsQueryService =
                    studyStatisticsQueryService
            )

        val activityQueryService =
            LearningDashboardActivityQueryService(
                learningProgressQueryService =
                    learningProgressQueryService
            )

        val memoryQueryService =
            LearningDashboardMemoryQueryService(
                memoryStateQuery =
                    memoryStateQuery,
                learningStageCountsCalculator =
                    LearningStageCountsCalculator()
            )

        val schedulingQueryService =
            LearningDashboardSchedulingQueryService(
                memoryStateQuery =
                    memoryStateQuery,
                dueStatisticsCalculator =
                    LearningDashboardDueStatisticsCalculator()
            )

        val retentionQueryService =
            LearningDashboardRetentionQueryService(
                memoryStateQuery =
                    memoryStateQuery,
                retentionStatisticsCalculator =
                    LearningDashboardRetentionStatisticsCalculator(
                        forgettingCurve =
                            forgettingCurve
                    )
            )

        val forecastQueryService =
            LearningDashboardForecastQueryService(
                memoryStateQuery =
                    memoryStateQuery,
                forecastCalculator =
                    LearningDashboardForecastCalculator()
            )

        return LearningDashboardQueryService(
            activityQueryService =
                activityQueryService,
            memoryQueryService =
                memoryQueryService,
            schedulingQueryService =
                schedulingQueryService,
            retentionQueryService =
                retentionQueryService,
            forecastQueryService =
                forecastQueryService
        )
    }
}