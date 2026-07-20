package vn.loi.learning.application.progress

/**
 * Snapshot tiến độ học tập của một learner
 * trong khoảng thời gian đã được truy vấn.
 *
 * Snapshot chứa các số liệu hoạt động nền tảng:
 * - tổng số review;
 * - số ngày có hoạt động;
 * - phân bố rating.
 *
 * Các metric dẫn xuất như averageReviewsPerActiveDay
 * và accuracy được tính thuần từ dữ liệu đã có,
 * không truy cập repository và không sửa đổi domain.
 */
data class LearningProgressSnapshot(
    val totalReviews: Int,
    val activeDays: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int
) {

    init {
        require(totalReviews >= 0) {
            "Total reviews must not be negative."
        }

        require(activeDays >= 0) {
            "Active days must not be negative."
        }

        require(activeDays <= totalReviews) {
            "Active days must not exceed total reviews."
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

        require(
            totalReviews > 0 || activeDays == 0
        ) {
            "Active days must be zero when there are no reviews."
        }
    }

    /**
     * Số review trung bình trên mỗi ngày thực sự có học.
     *
     * Trả về null khi chưa có ngày hoạt động.
     */
    val averageReviewsPerActiveDay: Double?
        get() =
            if (activeDays == 0) {
                null
            } else {
                totalReviews.toDouble() /
                        activeDays.toDouble()
            }

    /**
     * Tỷ lệ review không nhận AGAIN.
     *
     * HARD, GOOD và EASY được xem là các lần
     * learner đã nhớ được nội dung ở một mức độ nào đó.
     *
     * Trả về null khi chưa có review.
     */
    val accuracy: Double?
        get() =
            if (totalReviews == 0) {
                null
            } else {
                successfulReviewCount.toDouble() /
                        totalReviews.toDouble()
            }

    val hasReviewActivity: Boolean
        get() = totalReviews > 0

    private val successfulReviewCount: Int
        get() =
            hardCount +
                    goodCount +
                    easyCount

    companion object {

        val EMPTY: LearningProgressSnapshot =
            LearningProgressSnapshot(
                totalReviews = 0,
                activeDays = 0,
                againCount = 0,
                hardCount = 0,
                goodCount = 0,
                easyCount = 0
            )
    }
}