package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Persistence DTO của InstalledPackage (Library domain).
 *
 * Chỉ dùng cho persistence layer.
 * Không chứa logic nghiệp vụ.
 *
 * contentChecksum có thể null — backward compatible với record cũ chưa có field này.
 */
@Serializable
data class InstalledPackageRecord(
    val id: String,
    val libraryId: String,
    val packageId: String,
    val topicId: String,
    val name: String,
    val version: String,
    val state: String,
    val installedAt: String,
    val contentCount: Int = 0,
    val learningItemCount: Int = 0,
    val contentChecksum: String? = null
)
