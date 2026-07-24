package vn.loi.learning.domain.knowledge.model

/**
 * Bất biến, immutable — knowledge graph mô tả cấu trúc kiến thức.
 *
 * Không chứa learner state (review history, FSRS, due date, mastery, progress...).
 * Không phụ thuộc vào infrastructure, JSON, Desktop hay Android.
 *
 * Tất cả các collection trả về là bất biến (không thể sửa đổi từ bên ngoài).
 * Thứ tự của node và edge là deterministic (sort theo id.value, sau đó theo các trường khác).
 *
 * Khởi tạo thông qua [KnowledgeGraphFactory], không qua constructor trực tiếp.
 */
class KnowledgeGraph internal constructor(
    nodes: List<KnowledgeNode>,
    edges: List<KnowledgeEdge>
) {
    /**
     * Tất cả nodes, được sắp xếp deterministically theo (id.value, kind.name).
     */
    private val nodeList: List<KnowledgeNode> =
        nodes.sortedWith(compareBy({ it.id.value }, { it.kind.name }))

    /**
     * Tất cả edges, được sắp xếp deterministically theo
     * (sourceId.value, targetId.value, relationshipType.name).
     */
    private val edgeList: List<KnowledgeEdge> =
        edges.sortedWith(
            compareBy(
                { it.sourceId.value },
                { it.targetId.value },
                { it.relationshipType.name }
            )
        )

    /**
     * Index nhanh tra cứu node theo id (không thể mutate từ bên ngoài).
     */
    private val nodeById: Map<KnowledgeNodeId, KnowledgeNode> =
        nodeList.associateBy { it.id }

    // ── Query API ──────────────────────────────────────────────────────────

    /** Tìm node theo id; trả về null nếu không tìm thấy. */
    fun findNode(id: KnowledgeNodeId): KnowledgeNode? = nodeById[id]

    /** Kiểm tra node có tồn tại trong graph không. */
    fun containsNode(id: KnowledgeNodeId): Boolean = nodeById.containsKey(id)

    /**
     * Tất cả nodes (không thể sửa đổi), theo thứ tự deterministic.
     */
    fun nodes(): List<KnowledgeNode> = nodeList

    /**
     * Tất cả edges (không thể sửa đổi), theo thứ tự deterministic.
     */
    fun edges(): List<KnowledgeEdge> = edgeList

    /**
     * Các edges xuất phát từ node `nodeId` (outgoing).
     * Trả về list rỗng nếu node không tồn tại.
     */
    fun outgoing(nodeId: KnowledgeNodeId): List<KnowledgeEdge> =
        edgeList.filter { it.sourceId == nodeId }

    /**
     * Các edges đi vào node `nodeId` (incoming).
     * Trả về list rỗng nếu node không tồn tại.
     */
    fun incoming(nodeId: KnowledgeNodeId): List<KnowledgeEdge> =
        edgeList.filter { it.targetId == nodeId }

    /**
     * Tất cả nodes lân cận (neighbor) của node `nodeId` — không trùng lặp,
     * bao gồm cả outgoing targets và incoming sources.
     * Trả về list rỗng nếu node không tồn tại.
     */
    fun neighbors(nodeId: KnowledgeNodeId): List<KnowledgeNode> {
        val neighborIds = mutableSetOf<KnowledgeNodeId>()
        outgoing(nodeId).forEach { neighborIds.add(it.targetId) }
        incoming(nodeId).forEach { neighborIds.add(it.sourceId) }
        return neighborIds
            .mapNotNull { nodeById[it] }
            .sortedWith(compareBy({ it.id.value }, { it.kind.name }))
    }

    /**
     * Các node kế tiếp (successors) của `nodeId` qua một [KnowledgeRelationshipType] cụ thể.
     * Chỉ theo outgoing edges của loại đó.
     * Trả về list rỗng nếu node không tồn tại.
     */
    fun successors(
        nodeId: KnowledgeNodeId,
        relationshipType: KnowledgeRelationshipType
    ): List<KnowledgeNode> =
        outgoing(nodeId)
            .filter { it.relationshipType == relationshipType }
            .mapNotNull { nodeById[it.targetId] }
            .sortedWith(compareBy({ it.id.value }, { it.kind.name }))

    /**
     * Các node đi trước (predecessors) của `nodeId` qua một [KnowledgeRelationshipType] cụ thể.
     * Chỉ theo incoming edges của loại đó.
     * Trả về list rỗng nếu node không tồn tại.
     */
    fun predecessors(
        nodeId: KnowledgeNodeId,
        relationshipType: KnowledgeRelationshipType
    ): List<KnowledgeNode> =
        incoming(nodeId)
            .filter { it.relationshipType == relationshipType }
            .mapNotNull { nodeById[it.sourceId] }
            .sortedWith(compareBy({ it.id.value }, { it.kind.name }))

    /**
     * Tất cả edges từ `sourceId` đến `targetId` (mọi loại quan hệ).
     * Trả về list rỗng nếu không có cạnh nào.
     */
    fun edgesBetween(
        sourceId: KnowledgeNodeId,
        targetId: KnowledgeNodeId
    ): List<KnowledgeEdge> =
        edgeList.filter { it.sourceId == sourceId && it.targetId == targetId }

    /**
     * Tất cả nodes có [KnowledgeNodeKind] được chỉ định.
     * Theo thứ tự deterministic.
     */
    fun nodesOfKind(kind: KnowledgeNodeKind): List<KnowledgeNode> =
        nodeList.filter { it.kind == kind }

    // ── Equality & identity ─────────────────────────────────────────────────

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KnowledgeGraph) return false
        return nodeList == other.nodeList && edgeList == other.edgeList
    }

    override fun hashCode(): Int {
        var result = nodeList.hashCode()
        result = 31 * result + edgeList.hashCode()
        return result
    }

    override fun toString(): String =
        "KnowledgeGraph(nodes=${nodeList.size}, edges=${edgeList.size})"
}
