package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.progress.LearningStageCounts

/**
 * Section trạng thái memory của Learning Dashboard.
 *
 * Section này compose [LearningStageCounts] thay vì
 * sao chép từng learning stage thành các field riêng.
 *
 * Nhờ đó:
 * - invariant của phân bố LearningStage chỉ tồn tại
 *   tại một nơi;
 * - Dashboard không phụ thuộc vào cách tính counts;
 * - Application Layer giữ vai trò orchestration;
 * - việc bổ sung metric memory sau này không làm
 *   LearningDashboardSnapshot trở thành God DTO.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không thay đổi MemoryState;
 * - không tự tính lại phân bố LearningStage.
 */
data class LearningDashboardMemorySnapshot(
    val stageCounts: LearningStageCounts
) {

    /**
     * Tổng số memory trong hệ thống, bao gồm cả
     * memory đang suspended.
     */
    val totalMemories: Int
        get() = stageCounts.totalMemories

    /**
     * Tổng số memory đang hoạt động.
     *
     * Memory suspended không được tính là active.
     */
    val activeMemories: Int
        get() = stageCounts.activeMemories

    /**
     * Cho biết learner đã có memory hay chưa.
     */
    val hasMemories: Boolean
        get() = stageCounts.hasMemories

    companion object {

        val EMPTY: LearningDashboardMemorySnapshot =
            LearningDashboardMemorySnapshot(
                stageCounts = LearningStageCounts.EMPTY
            )
    }
}