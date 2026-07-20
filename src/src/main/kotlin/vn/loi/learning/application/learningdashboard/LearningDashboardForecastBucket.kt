package vn.loi.learning.application.learningdashboard

import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Một khoảng thời gian trong dự báo scheduling
 * của Learning Dashboard.
 *
 * Bucket sử dụng khoảng:
 *
 *     (windowStart, windowEnd]
 *
 * Nghĩa là:
 * - không bao gồm windowStart;
 * - bao gồm windowEnd.
 *
 * Quy ước này cho phép nhiều bucket liên tiếp
 * không bị chồng lấn hoặc đếm trùng MemoryState.
 *
 * [dueCount] là số memory có dueAt nằm trong khoảng
 * thời gian của bucket và đủ điều kiện tham gia forecast.
 *
 * Việc lựa chọn memory, loại bỏ suspended memory
 * và phân phối chúng vào bucket thuộc trách nhiệm
 * của calculator riêng.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thay đổi MemoryState;
 * - không tự thực hiện forecast.
 */
data class LearningDashboardForecastBucket(
    val windowStart: Moment,
    val windowEnd: Moment,
    val dueCount: Int
) {

    init {
        require(windowEnd > windowStart) {
            "Forecast bucket window end must be after window start."
        }

        require(dueCount >= 0) {
            "Forecast bucket due count must not be negative."
        }
    }

    /**
     * Cho biết bucket có ít nhất một memory
     * dự kiến đến hạn hay không.
     */
    val hasDueMemories: Boolean
        get() = dueCount > 0

    /**
     * Kiểm tra một thời điểm có thuộc bucket hay không.
     *
     * Bucket sử dụng khoảng (windowStart, windowEnd].
     */
    fun contains(
        moment: Moment
    ): Boolean =
        moment > windowStart &&
                moment <= windowEnd
}