package vn.loi.learning.desktop.runtime

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import vn.loi.learning.application.port.RecoveryOperationGate

data class DesktopRecoveryFile(val path: String, val size: Long, val sha256: String)

data class DesktopRecoveryManifest(
    val formatVersion: Int,
    val createdAt: Instant,
    val files: List<DesktopRecoveryFile>
)

open class DesktopRecoveryException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
class DesktopCatastrophicRecoveryException(val safetyBackup: Path, cause: Throwable) :
    DesktopRecoveryException(
        "Restore failed and rollback could not restore the exact previous state. Safety backup retained at $safetyBackup",
        cause
    )

class DesktopRecoveryManager(
    private val dataDirectory: Path,
    private val configDirectory: Path,
    private val clock: Clock = Clock.systemUTC(),
    private val gate: RecoveryOperationGate = RecoveryOperationGate(),
    private val stagedDomainValidator: (Path, Path) -> Unit = { _, _ -> },
    private val beforeRestoreWrite: (String) -> Unit = {}
) {
    val safetyBackupDirectory: Path = configDirectory.resolve("backups")

    fun createBackup(target: Path): Path = gate.backup { createBackupLocked(target) }

    private fun createBackupLocked(target: Path): Path {
        val normalized = target.toAbsolutePath().normalize()
        if (Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw DesktopRecoveryException("Backup target already exists.")
        }
        val parent = normalized.parent
            ?: throw DesktopRecoveryException("Backup target requires a parent directory.")
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw DesktopRecoveryException("Backup directory does not exist.")
        }

        val temporary = Files.createTempFile(parent, ".learning-engine-backup-", ".tmp")
        val staging = Files.createTempDirectory(parent, ".learning-engine-snapshot-")
        try {
            val sources = inventory().map { (name, source) ->
                val staged = staging.resolve(name).normalize()
                require(staged.startsWith(staging))
                Files.createDirectories(requireNotNull(staged.parent))
                Files.copy(source, staged)
                name to staged
            }
            val manifest = DesktopRecoveryManifest(FORMAT_VERSION, clock.instant(), sources.map {
                DesktopRecoveryFile(it.first, Files.size(it.second), sha256(it.second))
            })
            ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY).apply { time = 0L })
                zip.write(encode(manifest))
                zip.closeEntry()
                sources.forEach { (name, source) ->
                    zip.putNextEntry(ZipEntry(name).apply { time = 0L })
                    Files.copy(source, zip)
                    zip.closeEntry()
                }
            }
            validate(temporary)
            Files.move(temporary, normalized)
        } catch (failure: Exception) {
            throw DesktopRecoveryException("Could not create backup.", failure)
        } finally {
            Files.deleteIfExists(temporary)
            deleteTree(staging)
        }
        return normalized
    }

    fun validate(source: Path): DesktopRecoveryManifest {
        val normalized = source.toAbsolutePath().normalize()
        try {
            ZipFile(normalized.toFile()).use { zip ->
                val entries = zip.entries().asSequence().toList()
                val names = entries.map { it.name }
                if (names.size != names.toSet().size || names.any(::unsafeName)) {
                    throw DesktopRecoveryException("Backup archive structure is invalid.")
                }
                val manifestEntry = zip.getEntry(MANIFEST_ENTRY)
                    ?: throw DesktopRecoveryException("Backup manifest is missing.")
                val payloadNames = names.filter { it != MANIFEST_ENTRY }.sorted()
                val manifest = decode(
                    zip.getInputStream(manifestEntry).readAllBytes(),
                    expectedFileCount = payloadNames.size
                )
                if (manifest.formatVersion != FORMAT_VERSION) {
                    throw DesktopRecoveryException("Backup format is not supported.")
                }
                if (payloadNames != manifest.files.map { it.path }.sorted()) {
                    throw DesktopRecoveryException("Backup file inventory does not match the archive.")
                }
                manifest.files.forEach { file ->
                    val entry = zip.getEntry(file.path)
                        ?: throw DesktopRecoveryException("Backup file is missing: ${file.path}")
                    val bytes = zip.getInputStream(entry).readAllBytes()
                    if (bytes.size.toLong() != file.size || sha256(bytes) != file.sha256) {
                        throw DesktopRecoveryException("Backup checksum validation failed: ${file.path}")
                    }
                }
                return manifest
            }
        } catch (failure: DesktopRecoveryException) {
            throw failure
        } catch (failure: Exception) {
            throw DesktopRecoveryException("Could not validate backup.", failure)
        }
    }

    fun restore(source: Path, operationActive: Boolean): Path {
        if (operationActive) {
            throw DesktopRecoveryException("Restore is unavailable during an active study or persistence operation.")
        }
        return gate.restore { restoreLocked(source) }
    }

    private fun restoreLocked(source: Path): Path {
        val manifest = validate(source)
        val staging = Files.createTempDirectory(configDirectory.parent ?: configDirectory, ".learning-engine-restore-")
        try {
            ZipFile(source.toFile()).use { zip -> manifest.files.forEach { file ->
                val target = staging.resolve(file.path).normalize()
                require(target.startsWith(staging))
                Files.createDirectories(requireNotNull(target.parent))
                zip.getInputStream(zip.getEntry(file.path)).use { Files.copy(it, target) }
            } }
            stagedDomainValidator(staging.resolve("data"), staging.resolve("config"))
            Files.createDirectories(safetyBackupDirectory)
            val safety = safetyBackupDirectory.resolve("safety-${clock.instant().toEpochMilli()}.lebak")
            createBackupLocked(safety)
            validate(safety)
            val before = inventory().associate { it.first to Files.readAllBytes(it.second) }
            try {
                clearManagedFiles()
                manifest.files.forEach { file ->
                    beforeRestoreWrite(file.path)
                    val target = resolveManaged(file.path)
                    Files.createDirectories(target.parent)
                    Files.copy(staging.resolve(file.path), target)
                }
                verifyExact(manifest.files.associate { it.path to Files.readAllBytes(staging.resolve(it.path)) })
                stagedDomainValidator(dataDirectory, configDirectory)
            } catch (failure: Exception) {
                val rollback = runCatching {
                    clearManagedFiles()
                    before.forEach { (name, bytes) ->
                        val target = resolveManaged(name)
                        Files.createDirectories(target.parent)
                        Files.write(target, bytes)
                    }
                    verifyExact(before)
                }.exceptionOrNull()
                if (rollback != null) {
                    failure.addSuppressed(rollback)
                    throw DesktopCatastrophicRecoveryException(safety, failure)
                }
                throw DesktopRecoveryException("Restore failed and the previous snapshot was restored.", failure)
            }
            return safety
        } finally { deleteTree(staging) }
    }

    private fun verifyExact(expected: Map<String, ByteArray>) {
        if (inventory().map { it.first } != expected.keys.sorted()) error("Canonical inventory verification failed.")
        expected.forEach { (name, bytes) ->
            if (!Files.readAllBytes(resolveManaged(name)).contentEquals(bytes)) error("Canonical checksum verification failed: $name")
        }
    }

    private fun deleteTree(root: Path) {
        if (Files.notExists(root)) return
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }

    private fun inventory(): List<Pair<String, Path>> =
        listOf("data" to dataDirectory, "config" to configDirectory)
            .flatMap { (prefix, root) ->
                if (Files.notExists(root)) emptyList() else Files.walk(root).use { paths ->
                    paths.filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
                        .filter { !it.startsWith(safetyBackupDirectory) }
                        .filter { !it.fileName.toString().endsWith(".tmp") }
                        .map { "$prefix/${root.relativize(it).toString().replace('\\', '/')}" to it }
                        .toList()
                }
            }.sortedBy { it.first }

    private fun clearManagedFiles() {
        listOf(dataDirectory, configDirectory).forEach { root ->
            if (Files.exists(root)) Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder())
                    .filter { it != root && !it.startsWith(safetyBackupDirectory) }
                    .forEach(Files::deleteIfExists)
            }
        }
    }

    private fun resolveManaged(name: String): Path = when {
        name.startsWith("data/") -> dataDirectory.resolve(name.removePrefix("data/"))
        name.startsWith("config/") -> configDirectory.resolve(name.removePrefix("config/"))
        else -> throw DesktopRecoveryException("Backup contains an unknown durable root.")
    }.normalize()

    private fun unsafeName(name: String): Boolean =
        name.isBlank() || name.startsWith('/') || name.contains('\\') ||
            name.split('/').any { it.isBlank() || it == "." || it == ".." } ||
            (name != MANIFEST_ENTRY && !name.startsWith("data/") && !name.startsWith("config/"))

    companion object {
        const val FORMAT_VERSION = 1
        const val MANIFEST_ENTRY = "manifest.txt"
        const val FILE_EXTENSION = "lebak"

        private fun sha256(path: Path): String = sha256(Files.readAllBytes(path))
        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        private fun encode(manifest: DesktopRecoveryManifest): ByteArray = buildString {
            appendLine("format=${manifest.formatVersion}")
            appendLine("created=${manifest.createdAt}")
            appendLine("files=${manifest.files.size}")
            manifest.files.forEachIndexed { index, file ->
                appendLine("file.$index=${file.path}\t${file.size}\t${file.sha256}")
            }
        }.toByteArray(StandardCharsets.UTF_8)

        private fun decode(
            bytes: ByteArray,
            expectedFileCount: Int
        ): DesktopRecoveryManifest {
            val lines = bytes.toString(StandardCharsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
            fun value(prefix: String) = lines.singleOrNull { it.startsWith(prefix) }?.substringAfter('=')
                ?: throw DesktopRecoveryException("Backup manifest is invalid.")
            val count = value("files=").toIntOrNull()
                ?: throw DesktopRecoveryException("Backup manifest file count is invalid.")
            if (count < 0 || count != expectedFileCount) {
                throw DesktopRecoveryException("Backup manifest file count is invalid.")
            }
            val files = (0 until count).map { index ->
                val parts = value("file.$index=").split('\t')
                if (parts.size != 3) throw DesktopRecoveryException("Backup manifest inventory is invalid.")
                DesktopRecoveryFile(parts[0], parts[1].toLong(), parts[2])
            }
            return DesktopRecoveryManifest(
                value("format=").toInt(), Instant.parse(value("created=")), files
            )
        }
    }
}
