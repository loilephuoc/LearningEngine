package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
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

data class BatchTtsGenerationPlan(val packageName: String, val id: String)

object BatchTtsPlanIdentity {
    fun create(packageName: String, jobs: List<BatchTtsJob>, overwriteExisting: Boolean): BatchTtsGenerationPlan {
        require(packageName.isNotBlank())
        val canonical = buildString {
            token(packageName)
            token(overwriteExisting.toString())
            jobs.forEach { job ->
                token(job.id)
                token(job.contentId)
                token(job.field.name)
                token(job.language.code)
                token(job.textFingerprint())
                token(job.rate.toString())
                token(job.pitch.orEmpty())
                token(job.volume.orEmpty())
                job.candidateVoices.forEach { token(it.id) }
                token("END_VOICES")
            }
        }
        val id = java.security.MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return BatchTtsGenerationPlan(packageName, id)
    }

    private fun StringBuilder.token(value: String) {
        append(value.length).append(':').append(value).append('|')
    }
}

class BatchTtsCheckpointRepository(
    private val root: Path,
    private val staleLeaseAge: Duration = Duration.ofHours(24)
) {
    fun storeFor(plan: BatchTtsGenerationPlan): BatchTtsCheckpointStore =
        BatchTtsCheckpointStore(planDirectory(plan).resolve("${plan.id}.json"))

    fun checkpointPath(plan: BatchTtsGenerationPlan): Path = planDirectory(plan).resolve("${plan.id}.json")

    fun discard(plan: BatchTtsGenerationPlan): Boolean = Files.deleteIfExists(checkpointPath(plan))

    fun acquire(plan: BatchTtsGenerationPlan): AutoCloseable {
        val directory = planDirectory(plan)
        Files.createDirectories(directory)
        val lock = directory.resolve("${plan.id}.lock")
        tryAcquire(lock)?.let { return it }
        val modified = runCatching { Files.getLastModifiedTime(lock).toInstant() }.getOrNull()
        if (modified != null && Duration.between(modified, Instant.now()) > staleLeaseAge) {
            Files.deleteIfExists(lock)
            tryAcquire(lock)?.let { return it }
        }
        throw IllegalStateException("This exact TTS generation plan is already running")
    }

    private fun tryAcquire(lock: Path): AutoCloseable? = try {
        val channel = java.nio.channels.FileChannel.open(lock, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        channel.write(java.nio.ByteBuffer.wrap("${ProcessHandle.current().pid()}\n".toByteArray()))
        AutoCloseable {
            channel.close()
            Files.deleteIfExists(lock)
        }
    } catch (_: java.nio.file.FileAlreadyExistsException) {
        null
    }

    private fun planDirectory(plan: BatchTtsGenerationPlan): Path =
        root.resolve(plan.packageName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
}

internal fun BatchTtsJob.textFingerprint(): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest("${field.name}\u0000${language.code}\u0000$text".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
