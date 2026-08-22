package vn.loi.learning.infrastructure.recovery

import java.nio.file.Path
import kotlinx.serialization.Serializable

data class SafetyBackupCandidate(
    val path: Path,
    val fileName: String,
    val fileSizeBytes: Long,
    val preview: PortableBackupV2Preview
)

data class LegacySafetyBackup(
    val path: Path,
    val fileName: String,
    val fileSizeBytes: Long
)

data class SafetyBackupInventory(
    val validV2: List<SafetyBackupCandidate> = emptyList(),
    val legacy: List<LegacySafetyBackup> = emptyList(),
    val invalidV2Count: Int = 0
) {
    val totalValidV2Bytes: Long get() = validV2.sumOf(SafetyBackupCandidate::fileSizeBytes)
}

@Serializable
data class SafetyBackupCleanupResult(
    val retainedValidV2Count: Int = 0,
    val deletedValidV2Count: Int = 0,
    val failureMessage: String? = null
)
