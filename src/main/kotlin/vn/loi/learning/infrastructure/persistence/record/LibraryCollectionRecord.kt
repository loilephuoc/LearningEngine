package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Persistence DTO của LibraryCollection.
 *
 * Chỉ dùng trong persistence layer và không chứa logic nghiệp vụ.
 */
@Serializable
data class LibraryCollectionRecord(
    val id: String,
    val libraryId: String,
    val name: String,
    val packageIds: Set<String> = emptySet()
)