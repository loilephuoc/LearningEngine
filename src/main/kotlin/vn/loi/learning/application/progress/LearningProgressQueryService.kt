package vn.loi.learning.application.progress

import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.domain.study.analytics.model.StudyStatistics

/**
 * Application service truy vấn tiến độ học tập.
 *
 * Service này điều phối StudyStatisticsQueryService để:
 * - lấy thống kê toàn bộ activityPeriod;
 * - lấy thống kê của từng dailyPeriod;
 * - xác định số ngày có hoạt động;
 * - tạo LearningProgressSnapshot.
 *
 * Service không:
 * - truy cập repository trực tiếp;
 * - tự lọc ReviewEvent;
 * - tự tính rating statistics;
 * - tự chia ngày;
 * - tự đọc đồng hồ hệ thống hoặc xác định múi giờ.
 *
 * evaluatedAt hiện chưa được dùng cho nhóm metric hoạt động.
 * Thời điểm này sẽ được sử dụng ở các metric trạng thái
 * như due, overdue, maturity, stability và difficulty.
 */
class LearningProgressQueryService(
    private val studyStatisticsQueryService:
    StudyStatisticsQueryService
) {

    fun query(
        query: LearningProgressQuery
    ): LearningProgressSnapshot {
        val overallStatistics =
            queryStatistics(
                query = query
            )

        val activeDays =
            query.dailyPeriods.count { dailyPeriod ->
                studyStatisticsQueryService
                    .query(
                        ReviewHistoryQuery(
                            learnerId = query.learnerId,
                            learningItemIds = query.learningItemIds,
                            period = dailyPeriod
                        )
                    )
                    .hasReviews
            }

        return overallStatistics.toProgressSnapshot(
            activeDays = activeDays
        )
    }

    private fun queryStatistics(
        query: LearningProgressQuery
    ): StudyStatistics =
        studyStatisticsQueryService.query(
            ReviewHistoryQuery(
                learnerId = query.learnerId,
                learningItemIds = query.learningItemIds,
                period = query.activityPeriod
            )
        )

    private fun StudyStatistics.toProgressSnapshot(
        activeDays: Int
    ): LearningProgressSnapshot =
        LearningProgressSnapshot(
            totalReviews = totalReviews,
            activeDays = activeDays,
            againCount = againCount,
            hardCount = hardCount,
            goodCount = goodCount,
            easyCount = easyCount
        )
}
