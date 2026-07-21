package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * QueueDiversifier không thay đổi ordering.
 *
 * Implementation này được dùng khi SessionPolicy tắt diversity.
 */
class NoOpQueueDiversifier :
    QueueDiversifier {

    override fun diversify(
        orderedCandidates:
        List<SelectionCandidate>
    ): List<SelectionCandidate> =
        orderedCandidates.toList()
}