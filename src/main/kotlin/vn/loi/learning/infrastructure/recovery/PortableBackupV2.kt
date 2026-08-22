package vn.loi.learning.infrastructure.recovery

import java.nio.file.Path
import kotlinx.serialization.Serializable

@Serializable
data class PortableBackupEntryV2(
    val logicalPath: String,
    val section: String,
    val logicalType: String,
    val uncompressedSize: Long,
    val sha256: String
)

@Serializable
data class PortableBackupCountsV2(
    val packages: Long = 0,
    val contents: Long = 0,
    val learningItems: Long = 0,
    val memoryStates: Long = 0,
    val reviewEvents: Long = 0,
    val learningTrajectories: Long = 0,
    val studySessions: Long = 0,
    val studyQueues: Long = 0,
    val mediaFiles: Long = 0,
    val recordings: Long = 0
)

@Serializable
data class PortableBackupBytesV2(
    val mediaBytes: Long = 0,
    val recordingBytes: Long = 0,
    val totalExpandedBytes: Long = 0
)

@Serializable
data class PortableBackupPackageEntryV2(
    val packageId: String,
    val packageName: String,
    val version: String = "1.0.0",
    val contentCount: Int = 0,
    val learningItemCount: Int = 0,
    val mediaCount: Int = 0,
    val fingerprint: String? = null
)

@Serializable
data class PortableBackupManifestV2(
    val backupSchemaVersion: Int = 2,
    val backupId: String? = null,
    val appVersion: String,
    val versionCode: Long? = null,
    val createdAtUtc: String,
    val sourcePlatform: String,
    val learnerIds: List<String> = emptyList(),
    val selectivePackageIds: Set<String> = emptySet(),
    val includedSections: List<String>,
    val packages: List<PortableBackupPackageEntryV2> = emptyList(),
    val counts: PortableBackupCountsV2,
    val bytes: PortableBackupBytesV2,
    val entries: List<PortableBackupEntryV2>
)

data class PortableBackupV2Descriptor(
    val appVersion: String,
    val versionCode: Long? = null,
    val sourcePlatform: String,
    val learnerIds: List<String> = emptyList(),
    val specificPackageIds: Set<String>? = null,
    val includeLearningProgress: Boolean = true,
    val backupId: String? = null
)

data class PortableBackupPackagePlanV2(
    val packageId: String,
    val packageName: String,
    val version: String,
    val contentCount: Int,
    val learningItemCount: Int,
    val memoryStateCount: Int,
    val reviewEventCount: Int,
    val learningTrajectoryCount: Int,
    val studySessionCount: Int,
    val studyQueueCount: Int,
    val mediaFileCount: Int,
    val mediaBytes: Long,
    val estimatedDataBytes: Long
)

data class PortableBackupCreationPlanV2(
    val packages: List<PortableBackupPackagePlanV2>,
    val counts: PortableBackupCountsV2,
    val estimatedDataBytes: Long,
    val mediaBytes: Long,
    val estimatedTotalBytes: Long
)

enum class PortableBackupPhaseV2 {
    PREPARING,
    SCANNING_PACKAGES,
    CALCULATING_MEDIA,
    PREPARING_ARCHIVE,
    WRITING_DATA,
    WRITING_MEDIA,
    VERIFYING_BACKUP,
    FINALIZING,
    COMPLETED
}

class PortableBackupCancelledException : IllegalStateException("Backup cancelled safely.")

data class PortableBackupProgressV2(
    val phase: PortableBackupPhaseV2,
    val processedItems: Long = 0,
    val totalItems: Long = 0,
    val processedBytes: Long = 0,
    val totalBytes: Long = 0,
    val currentItem: String? = null
) {
    val fraction: Float
        get() = when {
            totalBytes > 0 -> (processedBytes.toDouble() / totalBytes).coerceIn(0.0, 1.0).toFloat()
            totalItems > 0 -> (processedItems.toDouble() / totalItems).coerceIn(0.0, 1.0).toFloat()
            phase == PortableBackupPhaseV2.COMPLETED -> 1f
            else -> 0f
        }

    val overallFraction: Float
        get() = when (phase) {
            PortableBackupPhaseV2.PREPARING -> 0.02f
            PortableBackupPhaseV2.SCANNING_PACKAGES -> 0.05f + 0.25f * fraction
            PortableBackupPhaseV2.CALCULATING_MEDIA -> 0.32f
            PortableBackupPhaseV2.PREPARING_ARCHIVE -> 0.35f
            PortableBackupPhaseV2.WRITING_DATA,
            PortableBackupPhaseV2.WRITING_MEDIA -> 0.35f + 0.50f * fraction
            PortableBackupPhaseV2.VERIFYING_BACKUP -> 0.88f
            PortableBackupPhaseV2.FINALIZING -> 0.96f
            PortableBackupPhaseV2.COMPLETED -> 1f
        }
}

data class PortableBackupV2Limits(
    val maxArchiveEntryCount: Int = 20_000,
    val maxUncompressedBytesPerEntry: Long = 2L * 1024 * 1024 * 1024,
    val maxTotalExpandedBytes: Long = 8L * 1024 * 1024 * 1024,
    val maxCompressionRatio: Double = 1_000.0,
    val ioBufferBytes: Int = 64 * 1024,
    val requireFreeDiskSpaceMarginBytes: Long = 50L * 1024 * 1024
) {
    init {
        require(maxArchiveEntryCount > 0)
        require(maxUncompressedBytesPerEntry > 0 && maxTotalExpandedBytes > 0)
        require(maxCompressionRatio >= 1.0 && ioBufferBytes > 0)
        require(requireFreeDiskSpaceMarginBytes >= 0)
    }
}

data class PortableBackupSupplementV2(
    val logicalPath: String,
    val section: String,
    val logicalType: String,
    val source: Path
)

fun interface PortableBackupV2SnapshotContributor {
    /** Called while the recovery gate owns the exclusive backup snapshot. */
    fun snapshot(stagingDirectory: Path): List<PortableBackupSupplementV2>
}

@Serializable
data class PortableBackupV2Preview(
    val backupSchemaVersion: Int,
    val backupId: String? = null,
    val appVersion: String,
    val versionCode: Long? = null,
    val createdAtUtc: String,
    val sourcePlatform: String,
    val learnerIds: List<String> = emptyList(),
    val includedSections: List<String> = emptyList(),
    val packages: List<PortableBackupPackageEntryV2> = emptyList(),
    val packagePreviews: List<vn.loi.learning.domain.sync.model.PackageRestorePreviewItem> = emptyList(),
    val counts: PortableBackupCountsV2 = PortableBackupCountsV2(),
    val bytes: PortableBackupBytesV2 = PortableBackupBytesV2(),
    val totalEntries: Int = 0
)

@Serializable
sealed interface PortableBackupV2RestoreResult {
    val message: String

    @Serializable
    data class Success(
        override val message: String = "Restore completed successfully.",
        val safetyBackupPath: String,
        val restoredEntriesCount: Int,
        val appVersion: String,
        val restoredCounts: PortableBackupCountsV2 = PortableBackupCountsV2()
    ) : PortableBackupV2RestoreResult

    @Serializable
    data class ValidationFailed(
        override val message: String,
        val detail: String? = null
    ) : PortableBackupV2RestoreResult

    @Serializable
    data class SafetyBackupFailed(
        override val message: String,
        val causeMessage: String? = null
    ) : PortableBackupV2RestoreResult

    @Serializable
    data class RestoreFailedRolledBack(
        override val message: String,
        val safetyBackupPath: String,
        val failureReason: String
    ) : PortableBackupV2RestoreResult

    @Serializable
    data class RollbackFailed(
        override val message: String,
        val safetyBackupPath: String,
        val restoreFailure: String,
        val rollbackFailure: String
    ) : PortableBackupV2RestoreResult

    @Serializable
    data class UnsupportedSchema(
        override val message: String,
        val foundVersion: Int,
        val supportedVersion: Int = 2
    ) : PortableBackupV2RestoreResult

    @Serializable
    data class InsufficientSpace(
        override val message: String,
        val requiredBytes: Long,
        val availableBytes: Long
    ) : PortableBackupV2RestoreResult

    @Serializable
    data class Busy(
        override val message: String = "Restore cannot proceed while another operation is active."
    ) : PortableBackupV2RestoreResult
}

interface PortableBackupV2RestoreConsumer {
    fun preflight(stagingDirectory: Path)
    fun captureCurrentState(): Any?
    fun applyRestored(stagingDirectory: Path)
    fun rollback(capturedState: Any?)
    fun validateLive()
}
