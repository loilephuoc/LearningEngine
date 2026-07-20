package vn.loi.learning.application.learningdashboard

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Tính thống kê các MemoryState đang đến hạn học.
 *
 * Calculator này:
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thay đổi MemoryState;
 * - sử dụng quy tắc due chính thức của MemoryState;
 * - chỉ phân biệt thêm overdue bằng cách so sánh dueAt
 *   với thời điểm thống kê được truyền từ bên ngoài.
 */
class LearningDashboardDueStatisticsCalculator {

    fun calculate(
        memoryStates: List<MemoryState>,
        at: Moment
    ): LearningDashboardDueStatistics {
        if (memoryStates.isEmpty()) {
            return LearningDashboardDueStatistics.EMPTY
        }

        var dueCount = 0
        var overdueCount = 0

        memoryStates.forEach { memoryState ->
            if (!memoryState.isDue(at)) {
                return@forEach
            }

            dueCount++

            if (memoryState.dueAt < at) {
                overdueCount++
            }
        }

        return LearningDashboardDueStatistics(
            dueCount = dueCount,
            overdueCount = overdueCount
        )
    }
}