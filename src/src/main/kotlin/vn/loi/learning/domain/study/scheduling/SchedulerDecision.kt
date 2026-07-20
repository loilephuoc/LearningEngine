package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Kết quả thuần Domain sau khi Scheduler xử lý một rating.
 */
data class SchedulerDecision(
    val previousState: MemoryState,
    val nextState: MemoryState,
    val scheduledInterval: TimeSpan
)