package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.MemoryState

/**
 * Application service tạo Scheduling section
 * của Learning Dashboard.
 *
 * Service này:
 * - đọc toàn bộ MemoryState của learner
 *   thông qua MemoryStateQuery;
 * - chuyển MemoryState và thời điểm đánh giá
 *   cho LearningDashboardDueStatisticsCalculator;
 * - bọc kết quả thành LearningDashboardSchedulingSnapshot.
 *
 * Service không:
 * - truy cập persistence implementation trực tiếp;
 * - tự xác định MemoryState nào đến hạn;
 * - tự tính số lượng due hoặc overdue;
 * - thay đổi MemoryState;
 * - tự đọc đồng hồ hệ thống.
 */
class LearningDashboardSchedulingQueryService(
    private val memoryStateQuery: MemoryStateQuery,
    private val dueStatisticsCalculator:
    LearningDashboardDueStatisticsCalculator
) {

    fun query(
        learnerId: LearnerId,
        at: Moment
    ): LearningDashboardSchedulingSnapshot =
        query(memoryStateQuery.findAll(learnerId), at)

    internal fun query(
        memoryStates: List<MemoryState>,
        at: Moment
    ): LearningDashboardSchedulingSnapshot {
        val dueStatistics =
            dueStatisticsCalculator.calculate(
                memoryStates = memoryStates,
                at = at
            )

        return LearningDashboardSchedulingSnapshot(
            dueStatistics = dueStatistics
        )
    }
}
