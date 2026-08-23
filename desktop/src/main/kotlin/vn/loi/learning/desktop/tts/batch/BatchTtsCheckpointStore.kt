package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BatchTtsCheckpointRecord(
    val jobId: String,
    val textFingerprint: String,
    val status: String,
    val assetRelativePath: String? = null
)

@Serializable
data class BatchTtsCheckpoint(
    val batchId: String,
    val packageName: String,
    val overwriteExisting: Boolean,
    val records: List<BatchTtsCheckpointRecord> = emptyList()
)

class BatchTtsCheckpointStore(private val path: Path) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun load(): BatchTtsCheckpoint? = try {
        if (!Files.isRegularFile(path)) null else json.decodeFromString<BatchTtsCheckpoint>(Files.readString(path))
    } catch (_: Exception) {
        null
    }

    fun save(checkpoint: BatchTtsCheckpoint) {
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, path.fileName.toString(), ".tmp")
        try {
            Files.writeString(temporary, json.encodeToString(checkpoint))
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun clear() = Files.deleteIfExists(path)
}

internal fun BatchTtsJob.textFingerprint(): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest("${field.name}\u0000${language.code}\u0000$text".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
