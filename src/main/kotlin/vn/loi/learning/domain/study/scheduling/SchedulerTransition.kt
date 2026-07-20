package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Kết quả chuyển trạng thái trung gian của Scheduler.
 *
 * SchedulerTransition chỉ chứa dữ liệu được xác định bởi
 * transition rule:
 *
 * - stage tiếp theo;
 * - difficulty tiếp theo;
 * - stability tiếp theo;
 * - interval được lập lịch.
 *
 * Nó chưa:
 *
 * - tạo nextState hoàn chỉnh;
 * - cập nhật thời điểm review;
 * - cập nhật reviewCount;
 * - cập nhật lapseCount.
 *
 * Difficulty và Stability được giữ hoàn toàn dưới dạng
 * Value Object trong scheduling domain.
 */
internal data class SchedulerTransition(
    val stage: LearningStage,
    val difficultyValue: Difficulty,
    val stability: Stability,
    val interval: TimeSpan
)