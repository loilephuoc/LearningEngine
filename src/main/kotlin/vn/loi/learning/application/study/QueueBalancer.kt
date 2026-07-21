package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Tinh chỉnh ordering của candidate theo một tiêu chí cân bằng.
 *
 * QueueBalancer không:
 * - thêm hoặc loại candidate;
 * - thay đổi candidate;
 * - áp dụng quota;
 * - đọc hoặc ghi repository.
 */
fun interface QueueBalancer {

    fun balance(
        orderedCandidates:
        List<SelectionCandidate>
    ): List<SelectionCandidate>
}