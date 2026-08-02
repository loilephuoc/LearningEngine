package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.study.memory.model.Moment

fun interface EvidenceClock {
    fun now(): Moment
}
