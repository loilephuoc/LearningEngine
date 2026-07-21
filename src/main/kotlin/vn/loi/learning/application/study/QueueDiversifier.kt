package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Tinh chỉnh thứ tự candidate sau StudyQueueStrategy.
 *
 * Diversifier không:
 * - thêm hoặc loại candidate;
 * - thay đổi candidate;
 * - áp dụng SessionPolicy limit;
 * - đọc hoặc ghi repository.
 */
fun interface QueueDiversifier {

    fun diversify(
        orderedCandidates:
        List<SelectionCandidate>
    ): List<SelectionCandidate>
}