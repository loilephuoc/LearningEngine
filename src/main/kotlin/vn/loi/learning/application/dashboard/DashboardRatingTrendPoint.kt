package vn.loi.learning.application.dashboard

import vn.loi.learning.domain.study.analytics.model.StudyPeriod

/**
 * Một điểm dữ liệu trong biểu đồ xu hướng rating của Dashboard.
 *
 * Point đại diện cho thống kê rating của đúng một daily period.
 *
 * Dữ liệu được lấy từ DashboardDailyStatistics đã tồn tại,
 * không truy vấn repository và không tính lại từ ReviewEvent.
 */
data class DashboardRatingTrendPoint(
    val period: StudyPeriod,
    val totalReviews: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int
) {

    init {
        require(totalReviews >= 0) {
            "Total reviews must not be negative."
        }

        require(againCount >= 0) {
            "Again count must not be negative."
        }

        require(hardCount >= 0) {
            "Hard count must not be negative."
        }

        require(goodCount >= 0) {
            "Good count must not be negative."
        }

        require(easyCount >= 0) {
            "Easy count must not be negative."
        }

        require(
            againCount +
                    hardCount +
                    goodCount +
                    easyCount ==
                    totalReviews
        ) {
            "Rating counts must equal total reviews."
        }
    }

    val hasReviewActivity: Boolean
        get() = totalReviews > 0

    val againProportion: Double?
        get() =
            proportionOf(
                count = againCount
            )

    val hardProportion: Double?
        get() =
            proportionOf(
                count = hardCount
            )

    val goodProportion: Double?
        get() =
            proportionOf(
                count = goodCount
            )

    val easyProportion: Double?
        get() =
            proportionOf(
                count = easyCount
            )

    private fun proportionOf(
        count: Int
    ): Double? =
        if (totalReviews == 0) {
            null
        } else {
            count.toDouble() / totalReviews.toDouble()
        }

    companion object {

        fun from(
            dailyStatistics: DashboardDailyStatistics
        ): DashboardRatingTrendPoint =
            DashboardRatingTrendPoint(
                period = dailyStatistics.period,
                totalReviews =
                    dailyStatistics.statistics.totalReviews,
                againCount =
                    dailyStatistics.statistics.againCount,
                hardCount =
                    dailyStatistics.statistics.hardCount,
                goodCount =
                    dailyStatistics.statistics.goodCount,
                easyCount =
                    dailyStatistics.statistics.easyCount
            )
    }
}