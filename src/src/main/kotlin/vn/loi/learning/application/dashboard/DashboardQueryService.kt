package vn.loi.learning.application.dashboard

import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.memory.model.LearnerId

/**
 * Application service truy vấn dữ liệu thống kê dành cho Dashboard.
 *
 * Service này:
 * - nhận DashboardQuery;
 * - tạo các ReviewHistoryQuery tương ứng;
 * - tái sử dụng StudyStatisticsQueryService;
 * - trả về DashboardStatisticsSnapshot.
 *
 * Service không:
 * - truy cập ReviewEventRepository trực tiếp;
 * - tự lọc ReviewEvent;
 * - tự tính toán metric;
 * - tự xác định ngày, tuần, tháng hoặc múi giờ.
 */
class DashboardQueryService(
    private val studyStatisticsQueryService:
    StudyStatisticsQueryService
) : DashboardStatisticsQuery {

    override fun query(
        query: DashboardQuery
    ): DashboardStatisticsSnapshot =
        DashboardStatisticsSnapshot(
            today =
                queryStatistics(
                    learnerId = query.learnerId,
                    period = query.todayPeriod
                ),
            currentWeek =
                queryStatistics(
                    learnerId = query.learnerId,
                    period = query.currentWeekPeriod
                ),
            currentMonth =
                queryStatistics(
                    learnerId = query.learnerId,
                    period = query.currentMonthPeriod
                ),
            dailyStatistics =
                query.dailyPeriods.map { period ->
                    DashboardDailyStatistics(
                        period = period,
                        statistics =
                            queryStatistics(
                                learnerId = query.learnerId,
                                period = period
                            )
                    )
                }
        )

    private fun queryStatistics(
        learnerId: LearnerId,
        period: StudyPeriod
    ): StudyStatistics =
        studyStatisticsQueryService.query(
            ReviewHistoryQuery(
                learnerId = learnerId,
                period = period
            )
        )
}