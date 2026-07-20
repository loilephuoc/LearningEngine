package vn.loi.learning.domain.study.analytics.model

import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Thống kê tổng hợp từ một tập ReviewEvent.
 *
 * Model này chỉ chứa những số liệu có thể tính trực tiếp,
 * không suy diễn về khả năng ghi nhớ hoặc chất lượng học.
 */
data class StudyStatistics(
    val totalReviews: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val reviewsWithResponseTime: Int,
    val totalResponseTime: TimeSpan,
    val averageResponseTimeMillis: Double?
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

        require(reviewsWithResponseTime >= 0) {
            "Reviews with response time must not be negative."
        }

        require(
            againCount + hardCount + goodCount + easyCount ==
                    totalReviews
        ) {
            "Rating counts must equal total reviews."
        }

        require(reviewsWithResponseTime <= totalReviews) {
            "Reviews with response time must not exceed total reviews."
        }

        require(
            averageResponseTimeMillis == null ||
                    (
                            averageResponseTimeMillis.isFinite() &&
                                    averageResponseTimeMillis >= 0.0
                            )
        ) {
            "Average response time must be null or a finite non-negative value."
        }

        require(
            (reviewsWithResponseTime == 0) ==
                    (averageResponseTimeMillis == null)
        ) {
            "Average response time must be null exactly when no response times exist."
        }

        require(
            reviewsWithResponseTime > 0 ||
                    totalResponseTime == TimeSpan.ZERO
        ) {
            "Total response time must be zero when no response times exist."
        }
    }

    val hasReviews: Boolean
        get() = totalReviews > 0

    val hasResponseTimeData: Boolean
        get() = reviewsWithResponseTime > 0


    /**
     * Số review mà người học không nhớ được tại thời điểm trả lời.
     *
     * Hiện tại ReviewRating.AGAIN được xem là unsuccessful.
     */
    val unsuccessfulReviewCount: Int
        get() = againCount

    /**
     * Số review mà người học nhớ được ở một mức độ nào đó.
     *
     * Bao gồm HARD, GOOD và EASY.
     */
    val successfulReviewCount: Int
        get() = hardCount + goodCount + easyCount

    /**
     * Tỷ lệ review thành công trong toàn bộ review.
     *
     * Đây là observed success rate tại thời điểm review,
     * không phải retention probability của FSRS.
     *
     * Trả về null nếu chưa có review.
     */
    val successfulReviewProportion: Double?
        get() =
            if (totalReviews == 0) {
                null
            } else {
                successfulReviewCount.toDouble() /
                        totalReviews
            }

    /**
     * Tỷ lệ review không thành công trong toàn bộ review.
     *
     * Trả về null nếu chưa có review.
     */
    val unsuccessfulReviewProportion: Double?
        get() =
            if (totalReviews == 0) {
                null
            } else {
                unsuccessfulReviewCount.toDouble() /
                        totalReviews
            }

    /**
     * Trả về số lần xuất hiện của một rating.
     */
    fun countFor(
        rating: ReviewRating
    ): Int =
        when (rating) {
            ReviewRating.AGAIN -> againCount
            ReviewRating.HARD -> hardCount
            ReviewRating.GOOD -> goodCount
            ReviewRating.EASY -> easyCount
        }

    /**
     * Tỷ lệ của một rating trong toàn bộ review.
     *
     * Kết quả nằm trong khoảng 0.0..1.0.
     * Trả về null nếu chưa có review.
     */
    fun proportionFor(
        rating: ReviewRating
    ): Double? {
        if (totalReviews == 0) {
            return null
        }

        return countFor(rating).toDouble() /
                totalReviews
    }

    companion object {

        val EMPTY: StudyStatistics =
            StudyStatistics(
                totalReviews = 0,
                againCount = 0,
                hardCount = 0,
                goodCount = 0,
                easyCount = 0,
                reviewsWithResponseTime = 0,
                totalResponseTime = TimeSpan.ZERO,
                averageResponseTimeMillis = null
            )
    }
}