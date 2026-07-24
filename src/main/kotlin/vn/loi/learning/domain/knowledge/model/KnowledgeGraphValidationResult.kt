package vn.loi.learning.domain.knowledge.model

/**
 * Kết quả typed từ quá trình validation khi xây dựng knowledge graph.
 *
 * - [Valid]: không có vấn đề nào, kèm theo graph đã build thành công.
 * - [Invalid]: danh sách các vấn đề phát hiện được (không rỗng).
 */
sealed class KnowledgeGraphValidationResult {

    /** Validation thành công; graph sẵn sàng sử dụng. */
    data class Valid(
        val graph: KnowledgeGraph
    ) : KnowledgeGraphValidationResult()

    /** Validation thất bại; chứa danh sách các vấn đề (không rỗng, không thay đổi). */
    data class Invalid(
        val issues: List<KnowledgeGraphValidationIssue>
    ) : KnowledgeGraphValidationResult() {
        init {
            require(issues.isNotEmpty()) {
                "Invalid result must contain at least one issue."
            }
        }
    }
}
