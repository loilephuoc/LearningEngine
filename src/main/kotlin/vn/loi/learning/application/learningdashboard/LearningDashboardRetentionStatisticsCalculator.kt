package vn.loi.learning.application.learningdashboard

import vn.loi.learning.domain.study.memory.ForgettingCurve
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.Retrievability
import vn.loi.learning.domain.study.memory.model.Stability

/**
 * Tính retention statistics cho Learning Dashboard.
 *
 * Một MemoryState chỉ được đánh giá khi:
 * - đã từng được review;
 * - có lastReviewedAt;
 * - có stability dương;
 * - không ở trạng thái SUSPENDED;
 * - lastReviewedAt không nằm sau thời điểm thống kê.
 *
 * Calculator này:
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thay đổi MemoryState;
 * - không tự cài đặt forgetting curve;
 * - chỉ orchestration việc chuẩn hóa dữ liệu và tổng hợp kết quả.
 */
class LearningDashboardRetentionStatisticsCalculator(
    private val forgettingCurve: ForgettingCurve
) {

    fun calculate(
        memoryStates: List<MemoryState>,
        at: Moment
    ): LearningDashboardRetentionStatistics {
        if (memoryStates.isEmpty()) {
            return LearningDashboardRetentionStatistics.EMPTY
        }

        var retrievabilityTotal = 0.0
        var evaluatedMemoryCount = 0

        memoryStates.forEach { memoryState ->
            val lastReviewedAt =
                memoryState.lastReviewedAt
                    ?: return@forEach

            if (memoryState.stage == LearningStage.SUSPENDED) {
                return@forEach
            }

            val stability =
                memoryState.stability

            if (stability.isZero()) {
                return@forEach
            }

            if (lastReviewedAt > at) {
                return@forEach
            }

            val retrievability =
                forgettingCurve.calculate(
                    stability = stability,
                    elapsedTime = at - lastReviewedAt
                )

            retrievabilityTotal += retrievability.value
            evaluatedMemoryCount++
        }

        if (evaluatedMemoryCount == 0) {
            return LearningDashboardRetentionStatistics.EMPTY
        }

        return LearningDashboardRetentionStatistics(
            averageRetrievability =
                Retrievability(
                    retrievabilityTotal /
                            evaluatedMemoryCount
                ),
            evaluatedMemoryCount =
                evaluatedMemoryCount
        )
    }
}