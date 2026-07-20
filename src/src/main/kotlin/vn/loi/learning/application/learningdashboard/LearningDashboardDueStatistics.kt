package vn.loi.learning.application.learningdashboard

/**
 * Thống kê các MemoryState đang đến hạn học.
 *
 * [dueCount] là tổng số memory đến hạn tại thời điểm
 * Dashboard được tạo. Giá trị này bao gồm:
 * - memory đến hạn chính xác tại thời điểm thống kê;
 * - memory đã quá hạn.
 *
 * [overdueCount] là tập con của [dueCount], chỉ bao gồm
 * memory có thời điểm đến hạn nằm trước thời điểm thống kê.
 *
 * MemoryState đang suspended không được đưa vào các số liệu này.
 * Việc xác định một MemoryState có due hoặc overdue hay không
 * thuộc về calculator riêng, không thuộc result model này.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thay đổi MemoryState;
 * - không tự thực hiện phép tính scheduling.
 */
data class LearningDashboardDueStatistics(
    val dueCount: Int,
    val overdueCount: Int
) {

    init {
        require(dueCount >= 0) {
            "Due count must not be negative."
        }

        require(overdueCount >= 0) {
            "Overdue count must not be negative."
        }

        require(overdueCount <= dueCount) {
            "Overdue count must not exceed due count."
        }
    }

    /**
     * Số memory đến hạn chính xác tại thời điểm thống kê,
     * không bao gồm memory đã quá hạn.
     */
    val dueNowCount: Int
        get() = dueCount - overdueCount

    /**
     * Cho biết learner đang có ít nhất một memory
     * cần được học hoặc ôn lại.
     */
    val hasDueMemories: Boolean
        get() = dueCount > 0

    /**
     * Cho biết learner đang có ít nhất một memory
     * đã vượt quá thời điểm cần ôn.
     */
    val hasOverdueMemories: Boolean
        get() = overdueCount > 0

    companion object {

        val EMPTY: LearningDashboardDueStatistics =
            LearningDashboardDueStatistics(
                dueCount = 0,
                overdueCount = 0
            )
    }
}