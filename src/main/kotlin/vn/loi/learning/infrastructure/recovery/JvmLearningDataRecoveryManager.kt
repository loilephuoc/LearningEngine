package vn.loi.learning.infrastructure.recovery

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Clock
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.nio.file.StandardCopyOption
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardOpenOption
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import vn.loi.learning.application.port.RecoveryOperationGate
import vn.loi.learning.application.port.RecoveryOperationBusyException

internal fun groupLogicalEntriesByPhysicalTarget(
    roots: Map<String, Path>,
    names: List<String>
): List<Pair<Path, List<String>>> = names.sorted().groupBy { name ->
    val rootName = name.substringBefore('/')
    val root = roots[rootName]?.toAbsolutePath()?.normalize() ?: error("Unknown durable root.")
    root.resolve(name.substringAfter('/')).normalize().also { require(it.startsWith(root)) }
}.entries.sortedBy { it.key.toString() }.map { it.key to it.value }

internal fun filesEqual(first: Path, second: Path): Boolean {
    if (Files.size(first) != Files.size(second)) return false
    Files.newInputStream(first).buffered(FILE_COMPARISON_BUFFER_SIZE).use { firstInput ->
        Files.newInputStream(second).buffered(FILE_COMPARISON_BUFFER_SIZE).use { secondInput ->
            val firstBuffer = ByteArray(FILE_COMPARISON_BUFFER_SIZE)
            val secondBuffer = ByteArray(FILE_COMPARISON_BUFFER_SIZE)
            while (true) {
                val firstCount = firstInput.read(firstBuffer)
                val secondCount = secondInput.read(secondBuffer)
                if (firstCount != secondCount) return false
                if (firstCount < 0) return true
                for (index in 0 until firstCount) {
                    if (firstBuffer[index] != secondBuffer[index]) return false
                }
            }
        }
    }
}

private const val FILE_COMPARISON_BUFFER_SIZE = 64 * 1024

open class LearningDataRecoveryException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
class CatastrophicLearningDataRecoveryException(
    val safetyBackup: Path,
    cause: Throwable
) : LearningDataRecoveryException("Restore failed and rollback could not restore the exact previous state. Safety backup retained at $safetyBackup", cause)

class ConflictingRestoreAliasException(
    val logicalEntries: List<String>,
    val physicalTarget: Path
) : LearningDataRecoveryException(
    "CONFLICTING_ALIAS logical entries=${logicalEntries.joinToString(",")} physical target=${physicalTarget.toAbsolutePath().normalize()}"
)

/** JVM adapter for the established v1 .lebak inventory/checksum contract. */
class JvmLearningDataRecoveryManager(
    roots: Map<String, Path>,
    private val safetyDirectory: Path,
    private val clock: Clock = Clock.systemUTC(),
    private val gate: RecoveryOperationGate = RecoveryOperationGate(),
    private val stagedDomainValidator: (Map<String, Path>) -> Unit = {},
    private val failureHook: (String, String?) -> Unit = { _, _ -> }
) {
    private val roots = roots.toSortedMap().mapValues { it.value.toAbsolutePath().normalize() }

    init {
        require(this.roots.isNotEmpty() && this.roots.keys.all { safeSegment(it) })
    }

    fun createBackup(target: Path): Path = try {
        gate.backup {
            failureHook("backup-gate-acquired", null)
            createBackupLocked(target, "backup")
        }
    } catch (failure: RecoveryOperationBusyException) {
        throw failure
    } catch (failure: LearningDataRecoveryException) {
        throw failure
    } catch (failure: Exception) {
        throw LearningDataRecoveryException("Could not create backup.", failure)
    }

    fun createPortableBackupV2(
        target: Path,
        descriptor: PortableBackupV2Descriptor,
        contributor: PortableBackupV2SnapshotContributor? = null,
        limits: PortableBackupV2Limits = PortableBackupV2Limits()
    ): Path = try {
        gate.backup {
            failureHook("backup-v2-gate-acquired", null)
            createPortableBackupV2Locked(target, descriptor, contributor, limits)
        }
    } catch (failure: RecoveryOperationBusyException) {
        throw failure
    } catch (failure: LearningDataRecoveryException) {
        throw failure
    } catch (failure: Exception) {
        throw LearningDataRecoveryException("Could not create portable backup v2.", failure)
    }

    fun validatePortableBackupV2(
        source: Path,
        limits: PortableBackupV2Limits = PortableBackupV2Limits()
    ): PortableBackupManifestV2 = try {
        validatePortableBackupV2Internal(source, limits)
    } catch (failure: Exception) {
        throw LearningDataRecoveryException("Could not validate portable backup v2.", failure)
    }

    private fun createPortableBackupV2Locked(
        target: Path,
        descriptor: PortableBackupV2Descriptor,
        contributor: PortableBackupV2SnapshotContributor?,
        limits: PortableBackupV2Limits
    ): Path {
        val normalized = target.toAbsolutePath().normalize()
        if (Files.exists(normalized)) throw LearningDataRecoveryException("Backup target already exists.")
        Files.createDirectories(requireNotNull(normalized.parent))
        var temporary: Path? = null
        var staging: Path? = null
        try {
            temporary = Files.createTempFile(normalized.parent, ".learning-engine-backup-v2-", ".tmp")
            staging = Files.createTempDirectory(normalized.parent, ".learning-engine-snapshot-v2-")
            val canonical = portableInventory().map { (logical, source) ->
                val staged = staging.resolve(logical).normalize().also { require(it.startsWith(staging)) }
                Files.createDirectories(requireNotNull(staged.parent))
                Files.copy(source, staged)
                PortableBackupSupplementV2(logical, logical.substringBefore('/'), logicalType(logical), staged)
            }
            val supplements = contributor?.snapshot(staging) ?: emptyList()
            val payloads = (canonical + supplements).sortedBy { it.logicalPath }
            validatePayloadPlan(payloads, staging, limits)
            val records = payloads.map { payload ->
                PortableBackupEntryV2(
                    logicalPath = payload.logicalPath,
                    section = payload.section,
                    logicalType = payload.logicalType,
                    uncompressedSize = Files.size(payload.source),
                    sha256 = sha256(payload.source, limits.ioBufferBytes)
                )
            }
            val manifest = PortableBackupManifestV2(
                appVersion = descriptor.appVersion,
                versionCode = descriptor.versionCode,
                createdAtUtc = clock.instant().toString(),
                sourcePlatform = descriptor.sourcePlatform,
                learnerIds = descriptor.learnerIds.distinct().sorted(),
                includedSections = records.map { it.section }.distinct().sorted(),
                counts = inferCounts(records, payloads),
                bytes = PortableBackupBytesV2(
                    mediaBytes = records.filter { it.section == "portable" && it.logicalType == "media" }.sumOf { it.uncompressedSize },
                    recordingBytes = records.filter { it.logicalType == "recording" }.sumOf { it.uncompressedSize },
                    totalExpandedBytes = records.sumOf { it.uncompressedSize }
                ),
                entries = records
            )
            val manifestBytes = V2_JSON.encodeToString(manifest).toByteArray(StandardCharsets.UTF_8)
            if (manifestBytes.size.toLong() > limits.maxUncompressedBytesPerEntry) error("Backup manifest is oversized.")
            ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
                zip.putNextEntry(ZipEntry(V2_MANIFEST_ENTRY).apply { time = 0L })
                zip.write(manifestBytes)
                zip.closeEntry()
                payloads.forEach { payload ->
                    zip.putNextEntry(ZipEntry(payload.logicalPath).apply { time = 0L })
                    Files.newInputStream(payload.source).buffered(limits.ioBufferBytes).use { it.copyTo(zip, limits.ioBufferBytes) }
                    zip.closeEntry()
                }
            }
            forceFile(temporary)
            validatePortableBackupV2Internal(temporary, limits)
            publishAtomically(temporary, normalized)
            temporary = null
            return normalized
        } finally {
            temporary?.let(Files::deleteIfExists)
            staging?.let(::deleteTree)
        }
    }

    private fun createBackupLocked(target: Path, scope: String): Path {
        val normalized = target.toAbsolutePath().normalize()
        if (Files.exists(normalized)) throw LearningDataRecoveryException("Backup target already exists.")
        Files.createDirectories(requireNotNull(normalized.parent))
        var temporary: Path? = null
        var staging: Path? = null
        try {
            failureHook("$scope-temporary-archive-create", null)
            temporary = Files.createTempFile(normalized.parent, ".learning-engine-backup-", ".tmp")
            failureHook("$scope-staging-create", null)
            staging = Files.createTempDirectory(normalized.parent, ".learning-engine-snapshot-")
            val inventory = inventory()
            val files = inventory.mapIndexed { index, (name, source) ->
                failureHook("$scope-snapshot-copy", "$index:${inventory.size}:$name")
                val staged = staging.resolve(name).normalize()
                require(staged.startsWith(staging))
                Files.createDirectories(requireNotNull(staged.parent))
                Files.copy(source, staged)
                name to staged
            }
            failureHook("$scope-manifest-create", null)
            ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
                val manifest = buildString {
                    appendLine("format=1")
                    appendLine("created=${clock.instant()}")
                    appendLine("files=${files.size}")
                    files.forEachIndexed { index, (name, path) ->
                        failureHook("$scope-hash", "$index:${files.size}:$name")
                        appendLine("file.$index=$name\t${Files.size(path)}\t${sha256(Files.readAllBytes(path))}")
                    }
                }
                failureHook("$scope-archive-entry-write", MANIFEST_ENTRY)
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY).apply { time = 0L })
                zip.write(manifest.toByteArray(StandardCharsets.UTF_8)); zip.closeEntry()
                files.forEach { (name, path) ->
                    failureHook("$scope-archive-entry-write", name)
                    zip.putNextEntry(ZipEntry(name).apply { time = 0L })
                    Files.copy(path, zip); zip.closeEntry()
                }
            }
            failureHook("$scope-archive-verify", null)
            validate(temporary)
            failureHook("$scope-publish", null)
            Files.move(temporary, normalized)
            return normalized
        } catch (failure: Exception) {
            throw LearningDataRecoveryException("Could not create backup.", failure)
        } finally {
            temporary?.let(Files::deleteIfExists)
            staging?.let(::deleteTree)
        }
    }

    fun validate(source: Path): List<String> = try {
        ZipFile(source.toFile()).use { zip ->
            val entries = zip.entries().asSequence().toList()
            val names = entries.map { it.name }
            if (names.size != names.toSet().size || names.any(::unsafeName)) error("Unsafe backup archive.")
            val manifest = zip.getEntry(MANIFEST_ENTRY) ?: error("Backup manifest is missing.")
            val lines = zip.getInputStream(manifest).bufferedReader(StandardCharsets.UTF_8).readLines()
            fun value(prefix: String) = lines.single { it.startsWith(prefix) }.substringAfter('=')
            if (value("format=") != "1") error("Unsupported backup version.")
            val count = value("files=").toInt()
            val records = (0 until count).map { index -> value("file.$index=").split('\t') }
            if (records.any { it.size != 3 } || records.map { it[0] }.sorted() != names.filter { it != MANIFEST_ENTRY }.sorted()) {
                error("Backup inventory does not match.")
            }
            records.forEach { record ->
                val bytes = zip.getInputStream(zip.getEntry(record[0])).readAllBytes()
                if (bytes.size.toLong() != record[1].toLong() || sha256(bytes) != record[2]) error("Backup checksum failed.")
                if ("/media/" in record[0] && bytes.isEmpty()) error("Backup contains empty managed media.")
            }
            records.map { it[0] }
        }
    } catch (failure: Exception) { throw LearningDataRecoveryException("Could not validate backup.", failure) }

    fun restore(source: Path, operationActive: Boolean): Path {
        if (operationActive) throw LearningDataRecoveryException("Restore is unavailable during an active operation.")
        return try {
            gate.restore {
                failureHook("restore-gate-acquired", null)
                restoreLocked(source)
            }
        } catch (failure: RecoveryOperationBusyException) {
            throw failure
        } catch (failure: CatastrophicLearningDataRecoveryException) {
            throw failure
        } catch (failure: LearningDataRecoveryException) {
            throw failure
        } catch (failure: Exception) {
            throw LearningDataRecoveryException("Could not restore backup before canonical replacement.", failure)
        }
    }

    private fun restoreLocked(source: Path): Path {
        val names = validate(source)
        Files.createDirectories(safetyDirectory)
        val staging = Files.createTempDirectory(safetyDirectory, ".learning-engine-restore-")
        try {
            ZipFile(source.toFile()).use { zip -> names.forEachIndexed { index, name ->
                failureHook("restore-stage-extract", "$index:${names.size}:$name")
                val target = staging.resolve(name).normalize()
                require(target.startsWith(staging))
                Files.createDirectories(requireNotNull(target.parent))
                zip.getInputStream(zip.getEntry(name)).use { Files.copy(it, target) }
            } }
            failureHook("restore-staged-domain-validate", null)
            stagedDomainValidator(roots.keys.associateWith { staging.resolve(it) })
            val restoreTargets = physicalTargets(names) { staging.resolve(it) }
            Files.createDirectories(safetyDirectory)
            val safety = safetyDirectory.resolve("safety-${clock.instant().toEpochMilli()}.lebak")
            createBackupLocked(safety, "safety")
            failureHook("safety-verified", null)
            validate(safety)
            val before = inventory().associate { it.first to Files.readAllBytes(it.second) }
            try {
                clear()
                restoreTargets.forEachIndexed { index, targetPlan ->
                    failureHook("restore-write", "$index:${restoreTargets.size}:${targetPlan.logicalEntries.first()}")
                    Files.createDirectories(requireNotNull(targetPlan.physicalTarget.parent))
                    Files.copy(targetPlan.source, targetPlan.physicalTarget)
                }
                failureHook("restore-post-copy-verify", null)
                assertExactRestore(names, restoreTargets)
                failureHook("restore-live-domain-validate", null)
                stagedDomainValidator(roots)
                failureHook("restore-success-publish", null)
            } catch (failure: Exception) {
                val rollback = runCatching {
                    clear()
                    val rollbackTargets = physicalByteTargets(before)
                    rollbackTargets.forEachIndexed { index, targetPlan ->
                        failureHook("rollback-write", "$index:${rollbackTargets.size}:${targetPlan.logicalEntries.first()}")
                        Files.createDirectories(requireNotNull(targetPlan.physicalTarget.parent))
                        Files.write(targetPlan.physicalTarget, targetPlan.bytes)
                    }
                    assertExactBytes(before)
                }.exceptionOrNull()
                if (rollback != null) {
                    failure.addSuppressed(rollback)
                    throw CatastrophicLearningDataRecoveryException(safety, failure)
                }
                throw LearningDataRecoveryException("Restore failed and the previous snapshot was restored.", failure)
            }
            return safety
        } finally { deleteTree(staging) }
    }

    private fun assertExactRestore(expectedNames: List<String>, targets: List<PhysicalTarget>) {
        val actualNames = inventory().map { it.first }
        if (actualNames != expectedNames.sorted()) error("Canonical logical inventory verification failed.")
        targets.forEach { target ->
            if (!Files.exists(target.physicalTarget) || !filesEqual(target.source, target.physicalTarget)) {
                error("Canonical physical target verification failed: ${target.logicalEntries.first()}")
            }
        }
    }

    private fun assertExactBytes(expected: Map<String, ByteArray>) {
        val actualNames = inventory().map { it.first }
        if (actualNames != expected.keys.sorted()) error("Canonical inventory verification failed.")
        expected.forEach { (name, bytes) ->
            if (!Files.readAllBytes(resolve(name)).contentEquals(bytes)) error("Canonical checksum verification failed: $name")
        }
    }

    private fun portableInventory(): List<Pair<String, Path>> {
        val claimed = linkedSetOf<Path>()
        val output = mutableListOf<Pair<String, Path>>()
        roots.entries.sortedByDescending { it.value.nameCount }.forEach { (rootName, root) ->
            if (Files.notExists(root)) return@forEach
            Files.walk(root).use { paths -> paths
                .filter { Files.isRegularFile(it) && !it.startsWith(safetyDirectory) && !it.fileName.toString().endsWith(".tmp") }
                .forEach { source ->
                    val physical = source.toAbsolutePath().normalize()
                    if (claimed.add(physical)) {
                        val sectionPath = when (rootName) {
                            "data" -> "portable/data"
                            "media" -> "portable/media"
                            else -> "portable/$rootName"
                        }
                        output += "$sectionPath/${root.relativize(source).toString().replace('\\', '/')}" to source
                    }
                }
            }
        }
        return output.sortedBy { it.first }
    }

    private fun validatePayloadPlan(
        payloads: List<PortableBackupSupplementV2>,
        staging: Path,
        limits: PortableBackupV2Limits
    ) {
        if (payloads.size + 1 > limits.maxArchiveEntryCount) error("Backup has too many entries.")
        val names = payloads.map { it.logicalPath }
        if (names.size != names.toSet().size || names.map(String::lowercase).size != names.map(String::lowercase).toSet().size) {
            error("Backup contains duplicate or case-colliding paths.")
        }
        if (names.any(::unsafeV2Name)) error("Backup contains an unsafe logical path.")
        var total = 0L
        payloads.forEach {
            val source = it.source.toAbsolutePath().normalize()
            require(source.startsWith(staging.toAbsolutePath().normalize())) { "Contributor source escaped staging." }
            val size = Files.size(source)
            if (size > limits.maxUncompressedBytesPerEntry) error("Backup entry is oversized.")
            total = Math.addExact(total, size)
            if (total > limits.maxTotalExpandedBytes) error("Backup expanded size is oversized.")
        }
    }

    private fun validatePortableBackupV2Internal(source: Path, limits: PortableBackupV2Limits): PortableBackupManifestV2 =
        ZipFile(source.toFile()).use { zip ->
            val zipEntries = zip.entries().asSequence().toList()
            if (zipEntries.size > limits.maxArchiveEntryCount) error("Backup has too many entries.")
            val names = zipEntries.map { it.name }
            if (names.size != names.toSet().size || names.map(String::lowercase).size != names.map(String::lowercase).toSet().size) {
                error("Backup contains duplicate or case-colliding paths.")
            }
            if (names.any { it != V2_MANIFEST_ENTRY && unsafeV2Name(it) }) error("Unsafe backup archive.")
            val manifestEntry = zip.getEntry(V2_MANIFEST_ENTRY) ?: error("Backup v2 manifest is missing.")
            if (manifestEntry.size < 0 || manifestEntry.size > limits.maxUncompressedBytesPerEntry) error("Invalid manifest size.")
            val manifest = zip.getInputStream(manifestEntry).bufferedReader(StandardCharsets.UTF_8).use {
                V2_JSON.decodeFromString<PortableBackupManifestV2>(it.readText())
            }
            if (manifest.backupSchemaVersion != 2) error("Unsupported backup schema version.")
            val declared = manifest.entries
            if (declared.map { it.logicalPath }.sorted() != names.filter { it != V2_MANIFEST_ENTRY }.sorted()) {
                error("Backup inventory does not match manifest.")
            }
            if (declared.map { it.logicalPath }.distinct().size != declared.size) error("Duplicate manifest entry.")
            var total = 0L
            declared.forEach { record ->
                if (unsafeV2Name(record.logicalPath)) error("Unsafe manifest path.")
                val entry = zip.getEntry(record.logicalPath) ?: error("Declared entry is missing.")
                if (entry.size != record.uncompressedSize || entry.size > limits.maxUncompressedBytesPerEntry) error("Backup size failed.")
                total = Math.addExact(total, entry.size)
                if (total > limits.maxTotalExpandedBytes) error("Backup expanded size is oversized.")
                val compressed = entry.compressedSize
                if ((entry.size > 0 && compressed == 0L) || (compressed > 0 && entry.size.toDouble() / compressed > limits.maxCompressionRatio)) {
                    error("Backup compression ratio is unsafe.")
                }
                zip.getInputStream(entry).use { input ->
                    if (sha256(input, limits.ioBufferBytes) != record.sha256) error("Backup checksum failed.")
                }
            }
            if (total != manifest.bytes.totalExpandedBytes) error("Backup total expanded size failed.")
            if (declared.none { it.logicalPath.startsWith("portable/data/") }) error("Canonical data section is missing.")
            validateRecordingReferences(zip, declared)
            manifest
        }

    private fun validateRecordingReferences(zip: ZipFile, records: List<PortableBackupEntryV2>) {
        val index = records.singleOrNull { it.logicalPath == "android/recordings/index.json" } ?: return
        val text = zip.getInputStream(zip.getEntry(index.logicalPath)).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val paths = Regex("\\\"recordingFile\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").findAll(text).map { it.groupValues[1] }.toList()
        val names = records.map { it.logicalPath }.toSet()
        if (paths.any { unsafeRelativeRecordingPath(it) || "android/recordings/$it" !in names }) {
            error("Recording metadata does not resolve to an archive recording.")
        }
    }

    private fun inferCounts(
        records: List<PortableBackupEntryV2>,
        payloads: List<PortableBackupSupplementV2>
    ): PortableBackupCountsV2 {
        fun countItems(vararg fileNames: String): Long {
            for (fileName in fileNames) {
                val payload = payloads.firstOrNull { it.logicalPath == "portable/data/$fileName" } ?: continue
                try {
                    val text = Files.newBufferedReader(payload.source, StandardCharsets.UTF_8).use { it.readText() }
                    val element = V2_JSON.parseToJsonElement(text)
                    if (element is kotlinx.serialization.json.JsonArray) return element.size.toLong()
                    if (element is kotlinx.serialization.json.JsonObject) {
                        val inner = element["data"] ?: element["items"] ?: element["records"]
                        if (inner is kotlinx.serialization.json.JsonArray) return inner.size.toLong()
                    }
                } catch (_: Exception) {}
            }
            return 0L
        }

        return PortableBackupCountsV2(
            packages = countItems("installed-packages.json", "content-packages.json"),
            contents = countItems("contents.json"),
            learningItems = countItems("learning-items.json"),
            memoryStates = countItems("memory-states.json"),
            reviewEvents = countItems("review-events.json"),
            studySessions = countItems("study-sessions.json"),
            mediaFiles = records.count { it.logicalType == "media" }.toLong(),
            recordings = records.count { it.logicalType == "recording" }.toLong()
        )
    }

    private fun logicalType(path: String): String = when {
        path.startsWith("portable/media/") -> "media"
        path.endsWith(".json") -> "canonical-json"
        else -> "canonical-file"
    }

    private fun unsafeV2Name(name: String): Boolean = name.isBlank() || name.startsWith('/') || name.contains('\\') ||
        DRIVE_PATH.matches(name) || name.substringBefore('/') !in V2_ROOTS ||
        name.split('/').any { it.isBlank() || it == "." || it == ".." }

    private fun unsafeRelativeRecordingPath(name: String): Boolean = name.isBlank() || name.startsWith('/') ||
        name.contains('\\') || DRIVE_PATH.matches(name) || name.split('/').any { it.isBlank() || it == "." || it == ".." }

    private fun publishAtomically(temporary: Path, target: Path) {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, target)
        }
    }

    private fun forceFile(path: Path) = FileChannel.open(path, StandardOpenOption.WRITE).use { it.force(true) }

    private data class PhysicalTarget(val physicalTarget: Path, val logicalEntries: List<String>, val source: Path)
    private data class PhysicalByteTarget(val physicalTarget: Path, val logicalEntries: List<String>, val bytes: ByteArray)

    private fun physicalTargets(names: List<String>, source: (String) -> Path): List<PhysicalTarget> =
        groupLogicalEntriesByPhysicalTarget(roots, names).map { (target, aliases) ->
            val sources = aliases.map(source)
            val reference = sources.first()
            val identical = sources.drop(1).all { candidate ->
                Files.size(candidate) == Files.size(reference) &&
                    sha256(candidate) == sha256(reference) &&
                    filesEqual(reference, candidate)
            }
            if (!identical) throw ConflictingRestoreAliasException(aliases, target)
            if (aliases.size > 1) failureHook(
                "restore-alias-classified",
                "IDENTICAL_ALIAS:${aliases.joinToString(",")}:$target"
            )
            PhysicalTarget(target, aliases, reference)
        }

    private fun physicalByteTargets(expected: Map<String, ByteArray>): List<PhysicalByteTarget> =
        groupLogicalEntriesByPhysicalTarget(roots, expected.keys.toList()).map { (target, aliases) ->
            val reference = expected.getValue(aliases.first())
            if (aliases.drop(1).any { !expected.getValue(it).contentEquals(reference) }) {
                throw ConflictingRestoreAliasException(aliases, target)
            }
            PhysicalByteTarget(target, aliases, reference)
        }

    private fun inventory(): List<Pair<String, Path>> = roots.flatMap { (name, root) ->
        if (Files.notExists(root)) emptyList() else Files.walk(root).use { paths -> paths
            .filter { Files.isRegularFile(it) && !it.startsWith(safetyDirectory) && !it.fileName.toString().endsWith(".tmp") }
            .map { "$name/${root.relativize(it).toString().replace('\\', '/')}" to it }.toList() }
    }.sortedBy { it.first }

    private fun clear() = roots.values.forEach { root -> if (Files.exists(root)) Files.walk(root).use { paths -> paths
        .sorted(Comparator.reverseOrder()).filter { it != root && !it.startsWith(safetyDirectory) }.forEach(Files::deleteIfExists) } }

    private fun resolve(name: String): Path {
        val rootName = name.substringBefore('/'); val root = roots[rootName] ?: error("Unknown durable root.")
        return root.resolve(name.substringAfter('/')).normalize().also { require(it.startsWith(root)) }
    }
    private fun unsafeName(name: String) = name.isBlank() || name.startsWith('/') || name.contains('\\') ||
        name.split('/').any { it.isBlank() || it == "." || it == ".." } || (name != MANIFEST_ENTRY && name.substringBefore('/') !in roots)

    companion object {
        const val MANIFEST_ENTRY = "manifest.txt"
        const val V2_MANIFEST_ENTRY = "manifest.json"
        private val V2_ROOTS = setOf("portable", "android")
        private val DRIVE_PATH = Regex("^[A-Za-z]:.*")
        private val V2_JSON = Json { encodeDefaults = true; ignoreUnknownKeys = false; prettyPrint = true }
        private fun safeSegment(value: String) = value.isNotBlank() && '/' !in value && '\\' !in value && value !in setOf(".", "..")
        private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun sha256(path: Path, bufferSize: Int = DEFAULT_BUFFER_SIZE): String =
            Files.newInputStream(path).use { sha256(it, bufferSize) }

        private fun sha256(input: java.io.InputStream, bufferSize: Int): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(bufferSize)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    private fun deleteTree(root: Path) {
        if (Files.notExists(root)) return
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }
}
