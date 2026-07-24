package vn.loi.learning.domain.knowledge.model

/**
 * Bất biến, immutable — đại diện cho một nút trong knowledge graph.
 *
 * - `id` là định danh canonical duy nhất; display name không phải identity.
 * - `displayName` chỉ dành cho hiển thị, không ảnh hưởng đến equals/hashCode.
 * - Không chứa learner state (review history, FSRS, due date, mastery...).
 */
data class KnowledgeNode(
    val id: KnowledgeNodeId,
    val kind: KnowledgeNodeKind,
    val displayName: String
) {
    init {
        require(displayName.isNotBlank()) {
            "KnowledgeNode displayName must not be blank (nodeId=${id.value})."
        }
    }

    /**
     * Equality và ordering dựa trên `id` và `kind`, KHÔNG dựa vào `displayName`.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KnowledgeNode) return false
        return id == other.id && kind == other.kind
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }

    override fun toString(): String =
        "KnowledgeNode(id=${id.value}, kind=$kind, displayName='$displayName')"
}
