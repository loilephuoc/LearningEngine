package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.UUID
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

@Serializable
data class BatchTtsLeaseInfo(
    val planId: String,
    val ownerPid: Long,
    val ownerStartTimestamp: Long? = null,
    val leaseCreationTimestamp: Long = System.currentTimeMillis(),
    val lastHeartbeatTimestamp: Long = System.currentTimeMillis(),
    val ownerToken: String = UUID.randomUUID().toString()
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
    private val staleLeaseAge: Duration = Duration.ofHours(24),
    private val eventLogger: BatchTtsEventLogger = BatchTtsEventLogger.NoOp
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun storeFor(plan: BatchTtsGenerationPlan): BatchTtsCheckpointStore =
        BatchTtsCheckpointStore(planDirectory(plan).resolve("${plan.id}.json"))

    fun checkpointPath(plan: BatchTtsGenerationPlan): Path = planDirectory(plan).resolve("${plan.id}.json")

    fun discard(plan: BatchTtsGenerationPlan): Boolean = Files.deleteIfExists(checkpointPath(plan))

    fun acquire(plan: BatchTtsGenerationPlan): AutoCloseable {
        val directory = planDirectory(plan)
        Files.createDirectories(directory)
        val lock = directory.resolve("${plan.id}.lock")

        val currentPid = ProcessHandle.current().pid()
        val currentStartTime = runCatching {
            ProcessHandle.current().info().startInstant().map { it.toEpochMilli() }.orElse(null)
        }.getOrNull()
        val ownerToken = UUID.randomUUID().toString()

        val leaseInfo = BatchTtsLeaseInfo(
            planId = plan.id,
            ownerPid = currentPid,
            ownerStartTimestamp = currentStartTime,
            leaseCreationTimestamp = System.currentTimeMillis(),
            lastHeartbeatTimestamp = System.currentTimeMillis(),
            ownerToken = ownerToken
        )

        // Try direct acquire
        tryAcquire(lock, leaseInfo)?.let {
            eventLogger.logLeaseAcquireAttempt(
                planId = plan.id,
                ownerPid = currentPid,
                ownerToken = ownerToken,
                existingOwnerPid = null,
                existingOwnerAlive = null,
                decision = "ACQUIRED_NEW"
            )
            return it
        }

        // Lock file exists: inspect owner liveness
        val existingLease = readLeaseInfo(lock)
        val isAlive = isProcessAlive(existingLease?.ownerPid, existingLease?.ownerStartTimestamp)

        if (!isAlive) {
            val leaseAgeMillis = existingLease?.leaseCreationTimestamp?.let { System.currentTimeMillis() - it }
                ?: runCatching { Duration.between(Files.getLastModifiedTime(lock).toInstant(), Instant.now()).toMillis() }.getOrDefault(0L)

            eventLogger.logLeaseReclaimed(
                planId = plan.id,
                deadOwnerPid = existingLease?.ownerPid ?: -1L,
                leaseAgeMillis = leaseAgeMillis
            )
            eventLogger.logLeaseAcquireAttempt(
                planId = plan.id,
                ownerPid = currentPid,
                ownerToken = ownerToken,
                existingOwnerPid = existingLease?.ownerPid,
                existingOwnerAlive = false,
                decision = "RECLAIMED_DEAD_OWNER"
            )

            Files.deleteIfExists(lock)
            tryAcquire(lock, leaseInfo)?.let { return it }
        } else {
            // Check fallback stale lease age threshold
            val modified = runCatching { Files.getLastModifiedTime(lock).toInstant() }.getOrNull()
            if (modified != null && Duration.between(modified, Instant.now()) > staleLeaseAge) {
                eventLogger.logLeaseReclaimed(
                    planId = plan.id,
                    deadOwnerPid = existingLease?.ownerPid ?: -1L,
                    leaseAgeMillis = Duration.between(modified, Instant.now()).toMillis()
                )
                Files.deleteIfExists(lock)
                tryAcquire(lock, leaseInfo)?.let { return it }
            }

            eventLogger.logLeaseAcquireAttempt(
                planId = plan.id,
                ownerPid = currentPid,
                ownerToken = ownerToken,
                existingOwnerPid = existingLease?.ownerPid,
                existingOwnerAlive = true,
                decision = "REJECTED_LIVE_OWNER"
            )
        }

        throw IllegalStateException("This exact TTS generation plan is already running in process ${existingLease?.ownerPid ?: "unknown"}")
    }

    private fun readLeaseInfo(lock: Path): BatchTtsLeaseInfo? {
        if (!Files.exists(lock)) return null
        return try {
            val content = Files.readString(lock).trim()
            if (content.startsWith("{")) {
                json.decodeFromString<BatchTtsLeaseInfo>(content)
            } else {
                // Legacy plain PID format
                val pid = content.lineSequence().firstOrNull()?.trim()?.toLongOrNull() ?: return null
                BatchTtsLeaseInfo(
                    planId = "",
                    ownerPid = pid,
                    ownerStartTimestamp = null,
                    leaseCreationTimestamp = Files.getLastModifiedTime(lock).toMillis(),
                    lastHeartbeatTimestamp = Files.getLastModifiedTime(lock).toMillis()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isProcessAlive(pid: Long?, recordedStartTime: Long?): Boolean {
        if (pid == null || pid <= 0L) return false
        val handle = ProcessHandle.of(pid).orElse(null) ?: return false
        if (!handle.isAlive) return false

        if (recordedStartTime != null) {
            val actualStart = handle.info().startInstant().map { it.toEpochMilli() }.orElse(null)
            // If the start time is recorded and differs significantly from live process start time, PID was reused
            if (actualStart != null && kotlin.math.abs(actualStart - recordedStartTime) > 3000L) {
                return false
            }
        }
        return true
    }

    private fun tryAcquire(lock: Path, leaseInfo: BatchTtsLeaseInfo): AutoCloseable? = try {
        Files.createFile(lock)
        Files.writeString(lock, json.encodeToString(leaseInfo))
        AutoCloseable {
            Files.deleteIfExists(lock)
        }
    } catch (_: java.nio.file.FileAlreadyExistsException) {
        null
    } catch (_: Exception) {
        null
    }

    private fun planDirectory(plan: BatchTtsGenerationPlan): Path =
        root.resolve(plan.packageName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
}

internal fun BatchTtsJob.textFingerprint(): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest("${field.name}\u0000${language.code}\u0000$text".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
