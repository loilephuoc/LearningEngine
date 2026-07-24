package vn.loi.learning.domain.knowledge.model

/**
 * Một vấn đề validation phát hiện khi xây dựng knowledge graph.
 *
 * Không ném exception; được trả về dưới dạng kết quả typed.
 */
sealed class KnowledgeGraphValidationIssue {

    /** Hai nút có cùng id. */
    data class DuplicateNodeId(
        val nodeId: KnowledgeNodeId
    ) : KnowledgeGraphValidationIssue()

    /** Hai cạnh giống hệt nhau (cùng source, target, relationshipType). */
    data class DuplicateEdge(
        val edge: KnowledgeEdge
    ) : KnowledgeGraphValidationIssue()

    /** Cạnh tự vòng (source == target). */
    data class SelfEdge(
        val nodeId: KnowledgeNodeId,
        val relationshipType: KnowledgeRelationshipType
    ) : KnowledgeGraphValidationIssue()

    /** Source của cạnh không tồn tại trong node set. */
    data class MissingSourceNode(
        val edge: KnowledgeEdge
    ) : KnowledgeGraphValidationIssue()

    /** Target của cạnh không tồn tại trong node set. */
    data class MissingTargetNode(
        val edge: KnowledgeEdge
    ) : KnowledgeGraphValidationIssue()
}
