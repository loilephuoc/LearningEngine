package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * QueueBalancer giữ nguyên ordering.
 */
class NoOpQueueBalancer :
    QueueBalancer {

    override fun balance(
        orderedCandidates:
        List<SelectionCandidate>
    ): List<SelectionCandidate> =
        orderedCandidates.toList()
}