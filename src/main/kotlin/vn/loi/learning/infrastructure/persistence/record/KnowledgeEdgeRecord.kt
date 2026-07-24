package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Persistence DTO của một KnowledgeEdge.
 *
 * Chỉ dùng cho persistence layer.
 * Không chứa logic nghiệp vụ.
 */
@Serializable
data class KnowledgeEdgeRecord(
    val sourceId: String,
    val targetId: String,
    val relationshipType: String
)
