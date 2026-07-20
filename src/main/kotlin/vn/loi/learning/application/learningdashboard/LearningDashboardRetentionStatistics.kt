package vn.loi.learning.application.learningdashboard

import vn.loi.learning.domain.study.memory.model.Retrievability

/**
 * Thống kê khả năng truy hồi hiện tại của các memory
 * đã đủ điều kiện để đánh giá retention.
 *
 * [averageRetrievability] là xác suất truy hồi trung bình
 * của các memory được đánh giá tại cùng một thời điểm.
 *
 * [evaluatedMemoryCount] là số memory thực sự tham gia
 * vào phép tính trung bình.
 *
 * Khi không có memory đủ điều kiện:
 * - evaluatedMemoryCount bằng 0;
 * - averageRetrievability bằng null.
 *
 * null ở đây biểu diễn sự vắng mặt hợp lệ của dữ liệu,
 * không phải placeholder hay giá trị chưa hoàn thành.
 *
 * Việc lựa chọn memory đủ điều kiện và tính retrievability
 * thuộc trách nhiệm của calculator riêng.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không gọi ForgettingCurve;
 * - không thay đổi MemoryState.
 */
data class LearningDashboardRetentionStatistics(
    val averageRetrievability: Retrievability?,
    val evaluatedMemoryCount: Int
) {

    init {
        require(evaluatedMemoryCount >= 0) {
            "Evaluated memory count must not be negative."
        }

        if (evaluatedMemoryCount == 0) {
            require(averageRetrievability == null) {
                "Average retrievability must be absent when no memories were evaluated."
            }
        }

        if (evaluatedMemoryCount > 0) {
            require(averageRetrievability != null) {
                "Average retrievability must be present when memories were evaluated."
            }
        }
    }

    /**
     * Cho biết retention statistics có dữ liệu
     * để hiển thị hoặc phân tích hay không.
     */
    val hasData: Boolean
        get() = evaluatedMemoryCount > 0

    companion object {

        val EMPTY: LearningDashboardRetentionStatistics =
            LearningDashboardRetentionStatistics(
                averageRetrievability = null,
                evaluatedMemoryCount = 0
            )
    }
}