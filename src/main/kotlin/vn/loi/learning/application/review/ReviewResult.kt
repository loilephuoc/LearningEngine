package vn.loi.learning.application.review

import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.TimeSpan

data class ReviewResult(
    val memoryState: MemoryState,
    val reviewEvent: ReviewEvent,
    val scheduledInterval: TimeSpan,
    val memoryStateExistedBefore: Boolean = true
)
