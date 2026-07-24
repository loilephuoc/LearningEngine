package vn.loi.learning.domain.knowledge.model

/**
 * Định danh duy nhất cho một nút trong knowledge graph.
 *
 * Identity không phụ thuộc vào display name.
 * Giá trị phải không rỗng và không phải whitespace.
 */
@JvmInline
value class KnowledgeNodeId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "KnowledgeNodeId value must not be blank."
        }
    }

    override fun toString(): String = value
}
