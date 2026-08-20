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
    val studySessions: Long = 0,
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
data class PortableBackupManifestV2(
    val backupSchemaVersion: Int = 2,
    val appVersion: String,
    val versionCode: Long? = null,
    val createdAtUtc: String,
    val sourcePlatform: String,
    val learnerIds: List<String> = emptyList(),
    val includedSections: List<String>,
    val counts: PortableBackupCountsV2,
    val bytes: PortableBackupBytesV2,
    val entries: List<PortableBackupEntryV2>
)

data class PortableBackupV2Descriptor(
    val appVersion: String,
    val versionCode: Long? = null,
    val sourcePlatform: String,
    val learnerIds: List<String> = emptyList()
)

data class PortableBackupV2Limits(
    val maxArchiveEntryCount: Int = 20_000,
    val maxUncompressedBytesPerEntry: Long = 2L * 1024 * 1024 * 1024,
    val maxTotalExpandedBytes: Long = 8L * 1024 * 1024 * 1024,
    val maxCompressionRatio: Double = 1_000.0,
    val ioBufferBytes: Int = 64 * 1024
) {
    init {
        require(maxArchiveEntryCount > 0)
        require(maxUncompressedBytesPerEntry > 0 && maxTotalExpandedBytes > 0)
        require(maxCompressionRatio >= 1.0 && ioBufferBytes > 0)
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
