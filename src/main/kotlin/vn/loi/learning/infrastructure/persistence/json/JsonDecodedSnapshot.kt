package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

/** Instance-owned decoded snapshot that is invalidated by any persisted file replacement. */
internal class JsonDecodedSnapshot<T>(
    private val filePath: Path,
    private val storeName: String = filePath.fileName.toString()
) {
    private data class Fingerprint(val exists: Boolean, val size: Long, val modifiedMillis: Long, val fileKey: Any?)

    private var fingerprint: Fingerprint? = null
    private var value: List<T>? = null

    @Synchronized
    fun load(decode: () -> List<T>): List<T> {
        val totalStarted = System.nanoTime()
        val statStarted = System.nanoTime()
        val current = fingerprint()
        val statMs = elapsedMillis(statStarted)
        value?.takeIf { fingerprint == current }?.let {
            JsonPersistenceTrace.write(storeName, filePath, "snapshot_hit", statMs, it.size)
            return it
        }
        val decodeStarted = System.nanoTime()
        val decoded = decode()
        val decodeMs = elapsedMillis(decodeStarted)
        val copyStarted = System.nanoTime()
        return decoded.toList().also {
            val copyMs = elapsedMillis(copyStarted)
            value = it
            val publishStatStarted = System.nanoTime()
            fingerprint = fingerprint()
            JsonPersistenceTrace.write(
                storeName, filePath, "snapshot_cold", elapsedMillis(totalStarted), it.size,
                "statMs=$statMs decodePipelineMs=$decodeMs immutableCopyMs=$copyMs " +
                    "publishStatMs=${elapsedMillis(publishStatStarted)}"
            )
        }
    }

    @Synchronized
    fun written(records: List<T>) {
        value = records.toList()
        fingerprint = fingerprint()
    }

    private fun fingerprint(): Fingerprint {
        if (!Files.exists(filePath)) return Fingerprint(false, 0L, 0L, null)
        val attributes = Files.readAttributes(filePath, BasicFileAttributes::class.java)
        return Fingerprint(true, attributes.size(), attributes.lastModifiedTime().toMillis(), attributes.fileKey())
    }

    private fun elapsedMillis(started: Long): Long = (System.nanoTime() - started) / 1_000_000
}

object JsonPersistenceTrace {
    @Volatile
    var enabled: Boolean = false

    fun write(
        storeName: String,
        filePath: Path,
        event: String,
        elapsedMs: Long,
        recordCount: Int? = null,
        detail: String = ""
    ) {
        if (!enabled) return
        val bytes = runCatching { if (Files.exists(filePath)) Files.size(filePath) else 0L }.getOrDefault(-1L)
        println(
            "LearningEnginePersistence store=$storeName file=${filePath.fileName} bytes=$bytes " +
                "event=$event elapsedMs=$elapsedMs" +
                (recordCount?.let { " recordCount=$it" } ?: "") +
                (if (detail.isBlank()) "" else " $detail")
        )
    }
}
