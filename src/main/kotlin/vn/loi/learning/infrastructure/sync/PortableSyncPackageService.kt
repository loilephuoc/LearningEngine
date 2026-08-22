package vn.loi.learning.infrastructure.sync

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.sync.model.ContentDeltaRecord
import vn.loi.learning.domain.sync.model.MediaSyncItem
import vn.loi.learning.domain.sync.model.MediaSyncManifest
import vn.loi.learning.domain.sync.model.SyncPackageEntry
import vn.loi.learning.domain.sync.model.SyncPackageManifest
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord

data class StagedSyncPayload(
    val manifest: SyncPackageManifest,
    val contentDeltas: List<ContentDeltaRecord>,
    val reviewEvents: List<ReviewEventRecord>,
    val mediaManifest: MediaSyncManifest,
    val mediaFiles: Map<String, Path>
)

open class SyncPackageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

object PortableSyncPackageService {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private const val BUFFER_SIZE = 64 * 1024
    const val SYNC_MANIFEST_ENTRY = "sync-manifest.json"
    const val CONTENT_DELTAS_ENTRY = "content/deltas.json"
    const val REVIEW_EVENTS_ENTRY = "learning/review-events.json"
    const val MEDIA_MANIFEST_ENTRY = "media/manifest.json"

    fun writeSyncPackage(
        target: Path,
        sourcePlatform: String,
        sourceDeviceId: String? = null,
        packageIds: List<String> = emptyList(),
        contentDeltas: List<ContentDeltaRecord> = emptyList(),
        reviewEvents: List<ReviewEventRecord> = emptyList(),
        mediaManifest: MediaSyncManifest = MediaSyncManifest(),
        mediaFiles: Map<String, Path> = emptyMap()
    ): Path {
        val normalized = target.toAbsolutePath().normalize()
        if (Files.exists(normalized)) throw SyncPackageException("Target sync file already exists: $target")
        Files.createDirectories(requireNotNull(normalized.parent))

        val tempArchive = Files.createTempFile(normalized.parent, ".sync-pkg-", ".tmp")
        try {
            val contentJsonBytes = json.encodeToString(contentDeltas).toByteArray(StandardCharsets.UTF_8)
            val reviewJsonBytes = json.encodeToString(reviewEvents).toByteArray(StandardCharsets.UTF_8)
            val mediaJsonBytes = json.encodeToString(mediaManifest).toByteArray(StandardCharsets.UTF_8)

            val entries = mutableListOf<SyncPackageEntry>()
            entries += SyncPackageEntry(
                logicalPath = CONTENT_DELTAS_ENTRY,
                entryType = "content-deltas",
                uncompressedSize = contentJsonBytes.size.toLong(),
                sha256 = sha256(contentJsonBytes)
            )
            entries += SyncPackageEntry(
                logicalPath = REVIEW_EVENTS_ENTRY,
                entryType = "review-events",
                uncompressedSize = reviewJsonBytes.size.toLong(),
                sha256 = sha256(reviewJsonBytes)
            )
            entries += SyncPackageEntry(
                logicalPath = MEDIA_MANIFEST_ENTRY,
                entryType = "media-manifest",
                uncompressedSize = mediaJsonBytes.size.toLong(),
                sha256 = sha256(mediaJsonBytes)
            )

            mediaFiles.forEach { (relPath, filePath) ->
                val size = Files.size(filePath)
                val sha = sha256(filePath)
                entries += SyncPackageEntry(
                    logicalPath = "media/files/$relPath",
                    entryType = "media",
                    uncompressedSize = size,
                    sha256 = sha
                )
            }

            val manifest = SyncPackageManifest(
                formatVersion = 1,
                syncPackageId = "sync-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().take(8),
                createdAtUtc = java.time.Instant.now().toString(),
                sourcePlatform = sourcePlatform,
                sourceDeviceId = sourceDeviceId,
                packageIds = packageIds.distinct().sorted(),
                contentDeltasCount = contentDeltas.size,
                reviewEventsCount = reviewEvents.size,
                mediaAssetsCount = mediaFiles.size,
                totalExpandedBytes = entries.sumOf { it.uncompressedSize },
                entries = entries.sortedBy { it.logicalPath }
            )

            val manifestBytes = json.encodeToString(manifest).toByteArray(StandardCharsets.UTF_8)

            ZipOutputStream(Files.newOutputStream(tempArchive)).use { zip ->
                zip.putNextEntry(ZipEntry(SYNC_MANIFEST_ENTRY).apply { time = 0L })
                zip.write(manifestBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(CONTENT_DELTAS_ENTRY).apply { time = 0L })
                zip.write(contentJsonBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(REVIEW_EVENTS_ENTRY).apply { time = 0L })
                zip.write(reviewJsonBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(MEDIA_MANIFEST_ENTRY).apply { time = 0L })
                zip.write(mediaJsonBytes)
                zip.closeEntry()

                mediaFiles.toSortedMap().forEach { (relPath, filePath) ->
                    zip.putNextEntry(ZipEntry("media/files/$relPath").apply { time = 0L })
                    Files.newInputStream(filePath).use { it.copyTo(zip, BUFFER_SIZE) }
                    zip.closeEntry()
                }
            }

            Files.move(tempArchive, normalized, StandardCopyOption.REPLACE_EXISTING)
            return normalized
        } catch (e: Exception) {
            Files.deleteIfExists(tempArchive)
            throw SyncPackageException("Failed to write sync package: ${e.message}", e)
        }
    }

    fun readAndValidateSyncPackage(source: Path, stagingDir: Path): StagedSyncPayload {
        val normalized = source.toAbsolutePath().normalize()
        if (!Files.isRegularFile(normalized)) throw SyncPackageException("Sync file not found: $source")

        Files.createDirectories(stagingDir)
        try {
            ZipFile(normalized.toFile()).use { zip ->
                val manifestEntry = zip.getEntry(SYNC_MANIFEST_ENTRY)
                    ?: throw SyncPackageException("Sync manifest missing ($SYNC_MANIFEST_ENTRY)")
                val manifestJson = zip.getInputStream(manifestEntry).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val manifest = json.decodeFromString<SyncPackageManifest>(manifestJson)

                if (manifest.formatVersion != 1) {
                    throw SyncPackageException("Unsupported sync package formatVersion: ${manifest.formatVersion}")
                }

                // Verify and extract all entries
                val mediaFileMap = mutableMapOf<String, Path>()
                manifest.entries.forEach { entry ->
                    if (isUnsafeLogicalPath(entry.logicalPath)) {
                        throw SyncPackageException("Unsafe path in sync archive: ${entry.logicalPath}")
                    }
                    val zipEntry = zip.getEntry(entry.logicalPath)
                        ?: throw SyncPackageException("Declared entry missing from archive: ${entry.logicalPath}")

                    val targetFile = stagingDir.resolve(entry.logicalPath).normalize()
                    if (!targetFile.startsWith(stagingDir)) {
                        throw SyncPackageException("Entry path escaped staging directory: ${entry.logicalPath}")
                    }
                    Files.createDirectories(requireNotNull(targetFile.parent))

                    zip.getInputStream(zipEntry).use { input ->
                        Files.newOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            val digest = MessageDigest.getInstance("SHA-256")
                            var count: Int
                            while (input.read(buffer).also { count = it } > 0) {
                                output.write(buffer, 0, count)
                                digest.update(buffer, 0, count)
                            }
                            val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                            if (actualSha != entry.sha256) {
                                throw SyncPackageException("Checksum mismatch for entry: ${entry.logicalPath}")
                            }
                        }
                    }

                    if (entry.logicalPath.startsWith("media/files/")) {
                        val relMedia = entry.logicalPath.removePrefix("media/files/")
                        mediaFileMap[relMedia] = targetFile
                    }
                }

                val deltasFile = stagingDir.resolve(CONTENT_DELTAS_ENTRY)
                val contentDeltas: List<ContentDeltaRecord> = if (Files.isRegularFile(deltasFile)) {
                    val text = Files.newBufferedReader(deltasFile, StandardCharsets.UTF_8).use { it.readText() }
                    json.decodeFromString(text)
                } else emptyList()

                val reviewFile = stagingDir.resolve(REVIEW_EVENTS_ENTRY)
                val reviewEvents: List<ReviewEventRecord> = if (Files.isRegularFile(reviewFile)) {
                    val text = Files.newBufferedReader(reviewFile, StandardCharsets.UTF_8).use { it.readText() }
                    json.decodeFromString(text)
                } else emptyList()

                val mediaManifestFile = stagingDir.resolve(MEDIA_MANIFEST_ENTRY)
                val mediaManifest: MediaSyncManifest = if (Files.isRegularFile(mediaManifestFile)) {
                    val text = Files.newBufferedReader(mediaManifestFile, StandardCharsets.UTF_8).use { it.readText() }
                    json.decodeFromString(text)
                } else MediaSyncManifest()

                return StagedSyncPayload(
                    manifest = manifest,
                    contentDeltas = contentDeltas,
                    reviewEvents = reviewEvents,
                    mediaManifest = mediaManifest,
                    mediaFiles = mediaFileMap
                )
            }
        } catch (e: Exception) {
            throw SyncPackageException("Sync package validation failed: ${e.message}", e)
        }
    }

    private fun isUnsafeLogicalPath(path: String): Boolean {
        return path.isBlank() || path.startsWith('/') || path.contains('\\') ||
            path.split('/').any { it.isBlank() || it == "." || it == ".." }
    }

    fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).buffered(BUFFER_SIZE).use { stream ->
            val buffer = ByteArray(BUFFER_SIZE)
            var count: Int
            while (stream.read(buffer).also { count = it } > 0) {
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
