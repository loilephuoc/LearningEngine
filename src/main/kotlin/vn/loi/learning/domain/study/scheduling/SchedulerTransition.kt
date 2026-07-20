package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Kết quả nội bộ sau khi một scheduling rule
 * tính toán transition từ MemoryState hiện tại.
 *
 * SchedulerTransition chưa phải là SchedulerDecision:
 * - chưa chứa previousState;
 * - chưa tạo nextState hoàn chỉnh;
 * - chưa cập nhật thời điểm review;
 * - chưa cập nhật reviewCount hoặc lapseCount.
 */
internal data class SchedulerTransition(
    val stage: LearningStage,
    val difficulty: Double,
    val stabilityDays: Double,
    val interval: TimeSpan
)