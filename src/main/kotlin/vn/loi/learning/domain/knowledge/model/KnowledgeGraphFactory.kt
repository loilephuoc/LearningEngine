package vn.loi.learning.domain.knowledge.model

/**
 * Factory xây dựng [KnowledgeGraph] từ danh sách nodes và edges đầu vào.
 *
 * Thực hiện validation và trả về [KnowledgeGraphValidationResult] typed:
 * - [KnowledgeGraphValidationResult.Valid] nếu không có vấn đề nào.
 * - [KnowledgeGraphValidationResult.Invalid] nếu có ít nhất một vấn đề.
 *
 * Không ném exception cho lỗi nghiệp vụ thông thường.
 */
object KnowledgeGraphFactory {

    /**
     * Xây dựng graph từ danh sách nodes và edges.
     *
     * Thứ tự của input collection không ảnh hưởng đến kết quả
     * (equality và ordering của graph là deterministic).
     *
     * @param nodes Danh sách các node; có thể rỗng.
     * @param edges Danh sách các cạnh; có thể rỗng.
     */
    fun build(
        nodes: List<KnowledgeNode>,
        edges: List<KnowledgeEdge>
    ): KnowledgeGraphValidationResult {
        val issues = mutableListOf<KnowledgeGraphValidationIssue>()

        // Kiểm tra duplicate node id
        val seenNodeIds = mutableSetOf<KnowledgeNodeId>()
        for (node in nodes) {
            if (!seenNodeIds.add(node.id)) {
                issues.add(
                    KnowledgeGraphValidationIssue.DuplicateNodeId(node.id)
                )
            }
        }

        // Build node set (chỉ các node unique, để validate edges)
        val nodeIdSet = nodes.map { it.id }.toSet()

        // Kiểm tra edges
        val seenEdges = mutableSetOf<KnowledgeEdge>()
        for (edge in edges) {
            // Self-edge
            if (edge.sourceId == edge.targetId) {
                issues.add(
                    KnowledgeGraphValidationIssue.SelfEdge(
                        nodeId = edge.sourceId,
                        relationshipType = edge.relationshipType
                    )
                )
                continue
            }

            // Duplicate edge
            if (!seenEdges.add(edge)) {
                issues.add(
                    KnowledgeGraphValidationIssue.DuplicateEdge(edge)
                )
                continue
            }

            // Missing source
            if (edge.sourceId !in nodeIdSet) {
                issues.add(
                    KnowledgeGraphValidationIssue.MissingSourceNode(edge)
                )
            }

            // Missing target
            if (edge.targetId !in nodeIdSet) {
                issues.add(
                    KnowledgeGraphValidationIssue.MissingTargetNode(edge)
                )
            }
        }

        return if (issues.isEmpty()) {
            KnowledgeGraphValidationResult.Valid(
                KnowledgeGraph(
                    nodes = nodes,
                    edges = edges
                )
            )
        } else {
            KnowledgeGraphValidationResult.Invalid(issues)
        }
    }

    /**
     * Xây dựng graph rỗng (không có nodes, không có edges).
     *
     * Luôn trả về [KnowledgeGraphValidationResult.Valid].
     */
    fun empty(): KnowledgeGraphValidationResult.Valid =
        KnowledgeGraphValidationResult.Valid(
            KnowledgeGraph(
                nodes = emptyList(),
                edges = emptyList()
            )
        )
}
