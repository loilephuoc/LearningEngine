package vn.loi.learning.domain.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncPackageEntry(
    val logicalPath: String,
    val entryType: String,
    val uncompressedSize: Long,
    val sha256: String
)

@Serializable
data class SyncPackageManifest(
    val formatVersion: Int = 1,
    val syncPackageId: String,
    val createdAtUtc: String,
    val sourcePlatform: String,
    val sourceDeviceId: String? = null,
    val packageIds: List<String> = emptyList(),
    val contentDeltasCount: Int = 0,
    val reviewEventsCount: Int = 0,
    val mediaAssetsCount: Int = 0,
    val totalExpandedBytes: Long = 0,
    val entries: List<SyncPackageEntry> = emptyList()
)

@Serializable
enum class ContentDeltaOperation {
    UPSERT,
    DELETE
}

@Serializable
data class ContentDeltaRecord(
    val contentId: String,
    val operation: ContentDeltaOperation = ContentDeltaOperation.UPSERT,
    val packageId: String? = null,
    val type: String = "WORD",
    val primaryText: String = "",
    val translatedText: String? = null,
    val pronunciation: String? = null,
    val exampleText: String? = null,
    val exampleTranslation: String? = null,
    val primaryAudio: String? = null,
    val translatedAudio: String? = null,
    val image: String? = null,
    val exampleAudio: String? = null,
    val exampleTranslatedAudio: String? = null,
    val customFields: Map<String, String> = emptyMap(),
    val title: String? = null,
    val tags: Set<String> = emptySet(),
    val updatedAtEpochMillis: Long = 0L
)

@Serializable
data class MediaSyncItem(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String,
    val isContainedInSyncPackage: Boolean
)

@Serializable
data class MediaSyncManifest(
    val items: List<MediaSyncItem> = emptyList()
)

@Serializable
enum class SyncConflictType {
    CONTENT_FIELD_COLLISION,
    MEDIA_ASSET_COLLISION,
    REVIEW_TIMELINE_COLLISION
}

@Serializable
enum class ConflictResolutionStrategy {
    PRESERVE_LOCAL,
    APPLY_INCOMING,
    MERGE_FIELD_LEVEL
}

@Serializable
data class SyncConflict(
    val conflictType: SyncConflictType,
    val entityId: String,
    val fieldName: String? = null,
    val localValueSummary: String? = null,
    val incomingValueSummary: String? = null,
    val resolutionApplied: ConflictResolutionStrategy,
    val detail: String? = null
)

@Serializable
data class SyncResultSummary(
    val syncPackageId: String,
    val sourcePlatform: String,
    val contentDeltasApplied: Int = 0,
    val mediaAssetsAdded: Int = 0,
    val reviewEventsMerged: Int = 0,
    val reviewEventsDeduplicated: Int = 0,
    val conflicts: List<SyncConflict> = emptyList(),
    val success: Boolean = true,
    val message: String = "Sync completed successfully."
)

@Serializable
data class SyncPreviewReport(
    val syncPackageId: String,
    val sourcePlatform: String,
    val createdAtUtc: String,
    val packagesAffected: List<String> = emptyList(),
    val contentChangesCount: Int = 0,
    val newMediaCount: Int = 0,
    val newMediaBytes: Long = 0L,
    val existingMediaReusedCount: Int = 0,
    val reviewEventsCount: Int = 0,
    val reviewEventsDeduplicatedCount: Int = 0,
    val conflicts: List<SyncConflict> = emptyList(),
    val missingBaselinePackageIds: List<String> = emptyList(),
    val requiresFullBackup: Boolean = false,
    val warnings: List<String> = emptyList()
)
