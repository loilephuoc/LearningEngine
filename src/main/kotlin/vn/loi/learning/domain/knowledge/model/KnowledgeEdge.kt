package vn.loi.learning.domain.knowledge.model

/**
 * Cạnh có hướng (directed typed edge) giữa hai nút trong knowledge graph.
 *
 * - `sourceId` → `targetId` (directed).
 * - `relationshipType` là loại quan hệ được định kiểu rõ ràng.
 * - Self-edge (sourceId == targetId) không hợp lệ và sẽ bị từ chối khi validate graph.
 * - Equality dựa trên (sourceId, targetId, relationshipType).
 */
data class KnowledgeEdge(
    val sourceId: KnowledgeNodeId,
    val targetId: KnowledgeNodeId,
    val relationshipType: KnowledgeRelationshipType
)
