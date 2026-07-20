package vn.loi.learning.domain.study.analytics.service

import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Domain service tính thống kê từ lịch sử review.
 *
 * Calculator:
 * - không truy cập repository;
 * - không thay đổi ReviewEvent;
 * - không phụ thuộc persistence hoặc UI;
 * - không suy diễn các metric chưa được định nghĩa.
 */
class StudyStatisticsCalculator {

    /**
     * Tính thống kê trên toàn bộ tập sự kiện được cung cấp.
     */
    fun calculate(
        events: Iterable<ReviewEvent>
    ): StudyStatistics =
        calculateMatching(events) {
            true
        }

    /**
     * Tính thống kê chỉ với những review nằm trong period.
     */
    fun calculate(
        events: Iterable<ReviewEvent>,
        period: StudyPeriod
    ): StudyStatistics =
        calculateMatching(events) { event ->
            event.reviewedAt in period
        }

    private fun calculateMatching(
        events: Iterable<ReviewEvent>,
        predicate: (ReviewEvent) -> Boolean
    ): StudyStatistics {
        var totalReviews = 0
        var againCount = 0
        var hardCount = 0
        var goodCount = 0
        var easyCount = 0

        var reviewsWithResponseTime = 0
        var totalResponseTimeMillis = 0L

        events.forEach { event ->
            if (!predicate(event)) {
                return@forEach
            }

            totalReviews++

            when (event.rating) {
                ReviewRating.AGAIN ->
                    againCount++

                ReviewRating.HARD ->
                    hardCount++

                ReviewRating.GOOD ->
                    goodCount++

                ReviewRating.EASY ->
                    easyCount++
            }

            event.responseTime?.let { responseTime ->
                reviewsWithResponseTime++

                totalResponseTimeMillis =
                    Math.addExact(
                        totalResponseTimeMillis,
                        responseTime.millis
                    )
            }
        }

        if (totalReviews == 0) {
            return StudyStatistics.EMPTY
        }

        val averageResponseTimeMillis =
            if (reviewsWithResponseTime == 0) {
                null
            } else {
                totalResponseTimeMillis.toDouble() /
                        reviewsWithResponseTime
            }

        return StudyStatistics(
            totalReviews = totalReviews,
            againCount = againCount,
            hardCount = hardCount,
            goodCount = goodCount,
            easyCount = easyCount,
            reviewsWithResponseTime =
                reviewsWithResponseTime,
            totalResponseTime =
                TimeSpan(totalResponseTimeMillis),
            averageResponseTimeMillis =
                averageResponseTimeMillis
        )
    }
}