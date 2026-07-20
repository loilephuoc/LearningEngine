package vn.loi.learning.domain.study.selection.model

/**
 * Kết quả của một lần lựa chọn LearningItem tiếp theo.
 */
sealed interface SessionSelectionResult {

    /**
     * Đã lựa chọn được một candidate phù hợp.
     */
    data class Selected(
        val candidate: SelectionCandidate,
        val usedSiblingFallback: Boolean = false
    ) : SessionSelectionResult

    /**
     * Không thể lựa chọn candidate nào.
     */
    data class NoSelection(
        val reason: NoSelectionReason
    ) : SessionSelectionResult
}