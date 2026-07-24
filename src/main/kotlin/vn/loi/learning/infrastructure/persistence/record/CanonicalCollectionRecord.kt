package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Record lưu trữ của Aggregate Root Collection (Canonical Library Domain).
 * Bảo tồn toàn bộ trạng thái domain kể cả DELETED state và assigned packages.
 */
@Serializable
data class CanonicalCollectionRecord(
    val id: String,
    val libraryId: String,
    val name: String,
    val description: String = "",
    val assignedPackageIds: List<String> = emptyList(),
    val state: String,
    val createdAt: String
)
