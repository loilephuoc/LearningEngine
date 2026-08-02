package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.study.memory.model.TimeSpan

/** Authoritative result produced by an automatic evaluative recall interaction. */
data class AutomaticRecallEvidenceInput(
    val result: RecallResult,
    val wasRevealUsed: Boolean = false,
    val typingLatency: TimeSpan? = null
)
