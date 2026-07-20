package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Persistence DTO của ContentPackage.
 *
 * Chỉ dùng cho persistence layer.
 * Không chứa logic nghiệp vụ.
 */
@Serializable
data class PackageRecord(
    val id: String,
    val name: String,
    val version: String,
    val format: String,
    val libraryIds: Set<String> = emptySet()
)
