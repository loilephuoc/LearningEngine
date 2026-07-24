package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Persistence DTO của một KnowledgeNode.
 *
 * Chỉ dùng cho persistence layer.
 * Không chứa logic nghiệp vụ.
 */
@Serializable
data class KnowledgeNodeRecord(
    val id: String,
    val kind: String,
    val displayName: String
)
