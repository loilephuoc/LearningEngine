package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.MemoryState

/**
 * Application service tạo Retention section
 * của Learning Dashboard.
 *
 * Service này:
 * - đọc toàn bộ MemoryState của learner
 *   thông qua MemoryStateQuery;
 * - chuyển MemoryState và thời điểm đánh giá
 *   cho LearningDashboardRetentionStatisticsCalculator;
 * - bọc kết quả thành LearningDashboardRetentionSnapshot.
 *
 * Service không:
 * - truy cập persistence implementation trực tiếp;
 * - tự tính retrievability;
 * - tự loại bỏ suspended hoặc unreviewed memories;
 * - tự tính average retention;
 * - thay đổi MemoryState;
 * - tự đọc đồng hồ hệ thống.
 */
class LearningDashboardRetentionQueryService(
    private val memoryStateQuery: MemoryStateQuery,
    private val retentionStatisticsCalculator:
    LearningDashboardRetentionStatisticsCalculator
) {

    fun query(
        learnerId: LearnerId,
        at: Moment
    ): LearningDashboardRetentionSnapshot =
        query(memoryStateQuery.findAll(learnerId), at)

    internal fun query(
        memoryStates: List<MemoryState>,
        at: Moment
    ): LearningDashboardRetentionSnapshot {
        val retentionStatistics =
            retentionStatisticsCalculator.calculate(
                memoryStates = memoryStates,
                at = at
            )

        return LearningDashboardRetentionSnapshot(
            statistics = retentionStatistics
        )
    }
}
