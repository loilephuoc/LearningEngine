package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Record lưu trữ của LibraryEntry trong Canonical Library Domain.
 */
@Serializable
data class LibraryEntryRecord(
    val installedPackageId: String,
    val packageId: String,
    val registeredAt: String
)

/**
 * Record lưu trữ của Aggregate Root Library (Canonical Library Domain).
 * Đóng vai trò DTO cho persistence layer, không chứa logic nghiệp vụ.
 */
@Serializable
data class CanonicalLibraryRecord(
    val id: String,
    val name: String,
    val entries: List<LibraryEntryRecord> = emptyList(),
    val createdAt: String
)
