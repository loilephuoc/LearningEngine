package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Persistence DTO của một KnowledgeGraph.
 *
 * Chỉ dùng cho persistence layer.
 * Không chứa logic nghiệp vụ.
 *
 * Lưu toàn bộ nodes và edges trong một document JSON duy nhất.
 */
@Serializable
data class KnowledgeGraphRecord(
    val nodes: List<KnowledgeNodeRecord> = emptyList(),
    val edges: List<KnowledgeEdgeRecord> = emptyList()
)
