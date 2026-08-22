package vn.loi.learning.infrastructure.recovery

import java.nio.file.Path
import kotlinx.serialization.Serializable

@Serializable
enum class SafetyBackupValidationStatus { VALIDATED, VALIDATING, UNKNOWN, INVALID, MISSING }

@Serializable
data class SafetyBackupIndexEntry(
    val path: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModifiedMillis: Long,
    val preview: PortableBackupV2Preview? = null,
    val validationStatus: SafetyBackupValidationStatus = SafetyBackupValidationStatus.UNKNOWN,
    val lastValidatedAtUtc: String? = null
)

@Serializable
data class SafetyBackupIndex(
    val schemaVersion: Int = 1,
    val entries: List<SafetyBackupIndexEntry> = emptyList()
)

data class SafetyBackupListEntry(
    val path: Path,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModifiedMillis: Long,
    val preview: PortableBackupV2Preview?,
    val validationStatus: SafetyBackupValidationStatus,
    val lastValidatedAtUtc: String?
)

data class FastSafetyBackupInventory(
    val entries: List<SafetyBackupListEntry> = emptyList(),
    val legacy: List<LegacySafetyBackup> = emptyList()
)
