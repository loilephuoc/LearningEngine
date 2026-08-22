package vn.loi.learning.infrastructure.contentmedia

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import vn.loi.learning.application.port.IncrementalMediaBlobStore

class JvmIncrementalMediaBlobStore(rootDirectory: Path) : IncrementalMediaBlobStore {
    private val root = rootDirectory.toAbsolutePath().normalize()
    private val staging = root.resolve(".sync-staging")
    private val managed = root.resolve("sync")

    init {
        Files.createDirectories(staging)
        Files.createDirectories(managed)
    }

    override fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    override fun stage(sha256: String, bytes: ByteArray) {
        requireHash(sha256)
        require(sha256(bytes) == sha256) { "Staged media checksum mismatch." }
        val target = stagedPath(sha256)
        if (Files.isRegularFile(target) && digest(target) == sha256) return
        val temporary = Files.createTempFile(staging, ".incoming-", ".tmp")
        try {
            Files.write(temporary, bytes)
            require(digest(temporary) == sha256) { "Staged media checksum mismatch after write." }
            runCatching {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }.getOrElse {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    override fun hasStaged(sha256: String, sizeBytes: Long): Boolean {
        requireHash(sha256)
        val path = stagedPath(sha256)
        return Files.isRegularFile(path) && Files.size(path) == sizeBytes && digest(path) == sha256
    }

    override fun materialize(sha256: String, sizeBytes: Long, mimeType: String): String {
        require(hasStaged(sha256, sizeBytes)) { "Validated staged media blob is unavailable." }
        val extension = extensionFor(mimeType)
        val target = managed.resolve("$sha256.$extension").normalize()
        require(target.parent == managed) { "Managed media target escaped its root." }
        if (!Files.isRegularFile(target) || Files.size(target) != sizeBytes || digest(target) != sha256) {
            val temporary = Files.createTempFile(managed, ".materialize-", ".tmp")
            try {
                Files.copy(stagedPath(sha256), temporary, StandardCopyOption.REPLACE_EXISTING)
                require(Files.size(temporary) == sizeBytes && digest(temporary) == sha256) {
                    "Materialized media validation failed."
                }
                runCatching {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                }.getOrElse {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
        return "sync/$sha256.$extension"
    }

    override fun digestReference(reference: String): String? {
        val clean = safeRelative(reference) ?: return null
        val path = root.resolve(clean).normalize()
        return path.takeIf { it.startsWith(root) && Files.isRegularFile(it) }?.let(::digest)
    }

    override fun discardStaged(sha256: String) {
        requireHash(sha256)
        Files.deleteIfExists(stagedPath(sha256))
    }

    override fun deleteManaged(reference: String): Boolean {
        val clean = safeRelative(reference) ?: return false
        val target = root.resolve(clean).normalize()
        if (target.parent != managed || !MANAGED_FILE.matches(target.fileName.toString())) return false
        Files.deleteIfExists(target)
        return !Files.exists(target)
    }

    override fun isManaged(reference: String): Boolean {
        val clean = safeRelative(reference) ?: return false
        val target = root.resolve(clean).normalize()
        return target.parent == managed && MANAGED_FILE.matches(target.fileName.toString())
    }

    private fun stagedPath(sha256: String): Path = staging.resolve("$sha256.blob")
    private fun requireHash(value: String) = require(HASH.matches(value)) { "Invalid media SHA-256 identity." }
    private fun digest(path: Path): String = Files.newInputStream(path).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun safeRelative(value: String): Path? = runCatching {
        if (value.isBlank() || value.contains('\\') || value.startsWith('/') || DRIVE.matches(value)) return null
        val path = Path.of(value)
        if (path.isAbsolute || path.any { it.toString() == ".." }) return null
        path.normalize()
    }.getOrNull()

    private fun extensionFor(mimeType: String): String = EXTENSIONS[mimeType]
        ?: throw IllegalArgumentException("Unsupported incremental media MIME type.")

    companion object {
        private val HASH = Regex("[0-9a-f]{64}")
        private val DRIVE = Regex("^[A-Za-z]:.*")
        private val MANAGED_FILE = Regex("[0-9a-f]{64}\\.(mp3|wav|jpg|png|webp)")
        val EXTENSIONS = mapOf(
            "audio/mpeg" to "mp3", "audio/wav" to "wav",
            "image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp"
        )
    }
}
