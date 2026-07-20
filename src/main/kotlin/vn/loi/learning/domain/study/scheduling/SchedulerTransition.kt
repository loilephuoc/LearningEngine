package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Stability
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
 *
 * Difficulty và Stability được giữ dưới dạng Value Object.
 * Các alias primitive được giữ tạm thời để bảo toàn tương thích
 * trong quá trình migration.
 */
internal data class SchedulerTransition(
    val stage: LearningStage,
    val difficultyValue: Difficulty,
    val stability: Stability,
    val interval: TimeSpan
) {

    /**
     * Alias primitive tạm thời cho các call site cũ.
     */
    val difficulty: Double
        get() = difficultyValue.value

    /**
     * Alias primitive tạm thời cho các call site cũ.
     */
    val stabilityDays: Double
        get() = stability.days
}