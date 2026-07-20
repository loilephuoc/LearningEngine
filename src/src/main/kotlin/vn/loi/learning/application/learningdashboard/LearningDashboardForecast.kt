package vn.loi.learning.application.learningdashboard

/**
 * Dữ liệu forecast của Learning Dashboard.
 *
 * Forecast được biểu diễn bằng một danh sách bucket
 * có thứ tự thời gian tăng dần.
 *
 * Các bucket:
 * - không được chồng lấn;
 * - có thể tiếp giáp tại cùng một mốc thời gian;
 * - tự giữ due count của khoảng thời gian riêng.
 *
 * Model này không lưu thêm totalDueCount dưới dạng state
 * để tránh dữ liệu dẫn xuất bị sai lệch so với các bucket.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thay đổi MemoryState;
 * - không tự phân phối memory vào bucket.
 */
data class LearningDashboardForecast(
    val buckets: List<LearningDashboardForecastBucket>
) {

    init {
        requireBucketsAreOrderedAndNonOverlapping(
            buckets = buckets
        )
    }

    /**
     * Tổng số lần memory xuất hiện trong toàn bộ forecast.
     *
     * Vì các bucket không chồng lấn nên mỗi MemoryState
     * chỉ có thể thuộc tối đa một bucket.
     */
    val totalDueCount: Int
        get() =
            buckets.sumOf { bucket ->
                bucket.dueCount
            }

    /**
     * Cho biết forecast có ít nhất một bucket hay không.
     */
    val hasBuckets: Boolean
        get() = buckets.isNotEmpty()

    /**
     * Cho biết có ít nhất một memory dự kiến đến hạn
     * trong toàn bộ forecast hay không.
     */
    val hasDueMemories: Boolean
        get() = totalDueCount > 0

    companion object {

        val EMPTY: LearningDashboardForecast =
            LearningDashboardForecast(
                buckets = emptyList()
            )

        private fun requireBucketsAreOrderedAndNonOverlapping(
            buckets: List<LearningDashboardForecastBucket>
        ) {
            buckets
                .zipWithNext()
                .forEach { (previous, current) ->
                    require(
                        current.windowStart >= previous.windowEnd
                    ) {
                        "Forecast buckets must be ordered and must not overlap."
                    }
                }
        }
    }
}