package vn.loi.learning.application.learningdashboard

/**
 * Section scheduling của Learning Dashboard.
 *
 * Section này compose [LearningDashboardDueStatistics]
 * thay vì sao chép các field due trực tiếp lên root Dashboard.
 *
 * Nhờ đó:
 * - Due Statistics có contract riêng;
 * - Scheduling section có thể mở rộng độc lập;
 * - LearningDashboardSnapshot không trở thành God DTO;
 * - Application Layer chỉ tổng hợp kết quả đã được tính.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thực hiện scheduling;
 * - không thay đổi MemoryState.
 */
data class LearningDashboardSchedulingSnapshot(
    val dueStatistics: LearningDashboardDueStatistics
) {

    /**
     * Cho biết learner đang có memory cần học hoặc ôn.
     *
     * Đây chỉ là delegation tới Due Statistics.
     */
    val hasDueMemories: Boolean
        get() = dueStatistics.hasDueMemories

    /**
     * Cho biết learner đang có memory đã quá hạn.
     *
     * Đây chỉ là delegation tới Due Statistics.
     */
    val hasOverdueMemories: Boolean
        get() = dueStatistics.hasOverdueMemories

    companion object {

        val EMPTY: LearningDashboardSchedulingSnapshot =
            LearningDashboardSchedulingSnapshot(
                dueStatistics =
                    LearningDashboardDueStatistics.EMPTY
            )
    }
}