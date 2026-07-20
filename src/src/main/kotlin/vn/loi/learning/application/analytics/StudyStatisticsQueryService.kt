package vn.loi.learning.application.analytics

import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.analytics.service.StudyStatisticsCalculator

/**
 * Application service truy vấn thống kê học tập.
 *
 * Service này điều phối:
 * - ReviewHistoryQueryService để lấy lịch sử review;
 * - StudyStatisticsCalculator để tính thống kê.
 *
 * Nó không tự thực hiện:
 * - truy cập persistence;
 * - lọc ReviewEvent;
 * - tính toán metric.
 */
class StudyStatisticsQueryService(
    private val reviewHistoryQueryService:
    ReviewHistoryQueryService,
    private val studyStatisticsCalculator:
    StudyStatisticsCalculator
) {

    fun query(
        query: ReviewHistoryQuery
    ): StudyStatistics {
        val events =
            reviewHistoryQueryService.query(query)

        return studyStatisticsCalculator.calculate(events)
    }
}