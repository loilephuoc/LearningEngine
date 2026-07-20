package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Persistence DTO của ContentLibrary.
 *
 * Chỉ dùng cho persistence layer.
 * Không chứa logic nghiệp vụ.
 */
@Serializable
data class ContentLibraryRecord(
    val id: String,
    val name: String,
    val contentIds: Set<String> = emptySet()
)