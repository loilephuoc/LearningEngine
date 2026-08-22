package vn.loi.learning.infrastructure.recovery

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.loi.learning.application.port.RecoveryOperationGate
import vn.loi.learning.application.port.RecoveryOperationBusyException
import vn.loi.learning.domain.sync.model.PackageCompatibilityStatus
import vn.loi.learning.domain.sync.model.PackageRestorePreviewItem

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
private const val DEFAULT_STREAMING_BUFFER_SIZE = 64 * 1024
private const val SAFETY_V2_RETENTION_COUNT = 2
private val SAFETY_V2_FILE = Regex("^safety-v2-[0-9]+[.]lebak$")
private val LEGACY_SAFETY_FILE = Regex("^safety-(?!v2-).+[.]lebak$")
private val PROGRESS_DATA_FILES = setOf(
    "memory-states.json",
    "review-events.json",
    "learning-trajectories.json",
    "study-sessions.json",
    "study-queues.json"
)
private val CANONICAL_PACKAGE_DATA_FILES = listOf(
    "installed-packages.json", "content-packages.json", "content-libraries.json", "contents.json",
    "learning-items.json", "memory-states.json", "review-events.json", "learning-trajectories.json",
    "study-sessions.json", "study-queues.json"
)

private class UnsupportedSchemaException(message: String, val version: Int) : RuntimeException(message)
private class InsufficientSpaceException(message: String, val requiredBytes: Long, val availableBytes: Long) : RuntimeException(message)

private data class PackageBackupScope(
    val packageIds: Set<String>,
    val installedPackageIds: Set<String>,
    val libraryIds: Set<String>,
    val contentIds: Set<String>,
    val learningItemIds: Set<String>,
    val studySessionIds: Set<String>,
    val mediaReferences: Set<String>,
    val records: Map<String, List<JsonObject>>
)

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
    private val failureHook: (String, String?) -> Unit = { _, _ -> },
    private val safetyBackupDelete: (Path) -> Unit = Files::delete
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
        limits: PortableBackupV2Limits = PortableBackupV2Limits(),
        onProgress: (PortableBackupProgressV2) -> Unit = {},
        shouldCancel: () -> Boolean = { false }
    ): Path = try {
        gate.backup {
            failureHook("backup-v2-gate-acquired", null)
                createPortableBackupV2Locked(target, descriptor, contributor, limits, onProgress, shouldCancel)
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

    fun previewPortableBackupV2(
        source: Path,
        limits: PortableBackupV2Limits = PortableBackupV2Limits()
    ): PortableBackupV2Preview {
        val manifest = validatePortableBackupV2(source, limits)
        val packagePreviews = inspectPackageCompatibility(manifest.packages)
        return PortableBackupV2Preview(
            backupSchemaVersion = manifest.backupSchemaVersion,
            backupId = manifest.backupId,
            appVersion = manifest.appVersion,
            versionCode = manifest.versionCode,
            createdAtUtc = manifest.createdAtUtc,
            sourcePlatform = manifest.sourcePlatform,
            learnerIds = manifest.learnerIds,
            includedSections = manifest.includedSections,
            packages = manifest.packages,
            packagePreviews = packagePreviews,
            counts = manifest.counts,
            bytes = manifest.bytes,
            totalEntries = manifest.entries.size
        )
    }

    fun discoverSafetyBackups(
        limits: PortableBackupV2Limits = PortableBackupV2Limits()
    ): SafetyBackupInventory {
        if (!Files.isDirectory(safetyDirectory)) return SafetyBackupInventory()
        val valid = mutableListOf<SafetyBackupCandidate>()
        val legacy = mutableListOf<LegacySafetyBackup>()
        var invalid = 0
        Files.list(safetyDirectory).use { paths -> paths.filter(Files::isRegularFile).forEach { path ->
            val name = path.fileName.toString()
            when {
                SAFETY_V2_FILE.matches(name) -> try {
                    val preview = previewPortableBackupV2(path, limits)
                    Instant.parse(preview.createdAtUtc)
                    valid += SafetyBackupCandidate(
                        path = path.toAbsolutePath().normalize(),
                        fileName = name,
                        fileSizeBytes = Files.size(path),
                        preview = preview
                    )
                } catch (_: Exception) { invalid++ }
                LEGACY_SAFETY_FILE.matches(name) -> legacy += LegacySafetyBackup(
                    path.toAbsolutePath().normalize(), name, Files.size(path)
                )
            }
        } }
        return SafetyBackupInventory(
            validV2 = valid.sortedByDescending { Instant.parse(it.preview.createdAtUtc) },
            legacy = legacy.sortedByDescending { Files.getLastModifiedTime(it.path).toMillis() },
            invalidV2Count = invalid
        )
    }

    fun restorePortableBackupV2(
        source: Path,
        operationActive: Boolean = false,
        contributorForSafetyBackup: PortableBackupV2SnapshotContributor? = null,
        consumer: PortableBackupV2RestoreConsumer? = null,
        limits: PortableBackupV2Limits = PortableBackupV2Limits(),
        selectedPackageIds: Set<String>? = null
    ): PortableBackupV2RestoreResult {
        if (operationActive) return PortableBackupV2RestoreResult.Busy()
        if (selectedPackageIds != null && selectedPackageIds.isEmpty()) {
            return PortableBackupV2RestoreResult.ValidationFailed("Select at least one package for selective restore.")
        }
        return try {
            gate.restore {
                failureHook("restore-v2-gate-acquired", null)
                restorePortableBackupV2Locked(source, contributorForSafetyBackup, consumer, limits, selectedPackageIds)
            }
        } catch (busy: RecoveryOperationBusyException) {
            PortableBackupV2RestoreResult.Busy(busy.message ?: "Recovery gate is busy.")
        } catch (e: Exception) {
            PortableBackupV2RestoreResult.ValidationFailed("Restore failed: ${e.message}", e.message)
        }
    }

    fun previewPortableBackupCreation(descriptor: PortableBackupV2Descriptor): PortableBackupCreationPlanV2 {
        val dataRoot = roots["data"] ?: error("Portable backup requires a data root.")
        val mediaRoot = roots["media"] ?: dataRoot.resolve("media")
        val installed = readJsonArrayObjects(dataRoot.resolve("installed-packages.json"))
        val selectedIds = descriptor.specificPackageIds?.takeIf(Set<String>::isNotEmpty)
            ?: installed.mapNotNull { jsonString(it, "packageId") ?: jsonString(it, "id") }.toSet()
        val scope = buildPackageBackupScope(dataRoot, selectedIds, descriptor.includeLearningProgress)
        validatePackageBackupScope(scope, requireMedia = false)
        val packagePlans = scope.records.getValue("installed-packages.json").map { installedPackage ->
            val packageId = requireNotNull(jsonString(installedPackage, "packageId") ?: jsonString(installedPackage, "id"))
            val packageScope = buildPackageBackupScope(dataRoot, setOf(packageId), descriptor.includeLearningProgress)
            validatePackageBackupScope(packageScope, requireMedia = false)
            val packageName = jsonString(installedPackage, "name") ?: packageId
            val references = packageScope.mediaReferences.toSortedSet()
            val mediaFiles = references.map { reference ->
                val resolved = mediaRoot.resolve(reference).normalize()
                if (!resolved.startsWith(mediaRoot.normalize()) || !Files.isRegularFile(resolved)) {
                    error("Selected package media is incomplete: missing $reference")
                }
                resolved
            }
            val dataBytes = packageScope.records.values.flatten().sumOf(::estimatedJsonBytes)
            PortableBackupPackagePlanV2(
                packageId = packageId,
                packageName = packageName,
                version = jsonString(installedPackage, "version") ?: "1.0.0",
                contentCount = packageScope.records.getValue("contents.json").size,
                learningItemCount = packageScope.records.getValue("learning-items.json").size,
                memoryStateCount = packageScope.records.getValue("memory-states.json").size,
                reviewEventCount = packageScope.records.getValue("review-events.json").size,
                learningTrajectoryCount = packageScope.records.getValue("learning-trajectories.json").size,
                studySessionCount = packageScope.records.getValue("study-sessions.json").size,
                studyQueueCount = packageScope.records.getValue("study-queues.json").size,
                mediaFileCount = mediaFiles.size,
                mediaBytes = mediaFiles.sumOf(Files::size),
                estimatedDataBytes = dataBytes
            )
        }
        val totalDataBytes = packagePlans.sumOf { it.estimatedDataBytes }
        val totalMediaBytes = packagePlans.sumOf { it.mediaBytes }
        return PortableBackupCreationPlanV2(
            packages = packagePlans,
            counts = PortableBackupCountsV2(
                packages = packagePlans.size.toLong(),
                contents = packagePlans.sumOf { it.contentCount }.toLong(),
                learningItems = packagePlans.sumOf { it.learningItemCount }.toLong(),
                memoryStates = packagePlans.sumOf { it.memoryStateCount }.toLong(),
                reviewEvents = packagePlans.sumOf { it.reviewEventCount }.toLong(),
                learningTrajectories = packagePlans.sumOf { it.learningTrajectoryCount }.toLong(),
                studySessions = packagePlans.sumOf { it.studySessionCount }.toLong(),
                studyQueues = packagePlans.sumOf { it.studyQueueCount }.toLong(),
                mediaFiles = packagePlans.sumOf { it.mediaFileCount }.toLong()
            ),
            estimatedDataBytes = totalDataBytes,
            mediaBytes = totalMediaBytes,
            estimatedTotalBytes = Math.addExact(totalDataBytes, totalMediaBytes)
        )
    }

    private fun createPortableBackupV2Locked(
        target: Path,
        descriptor: PortableBackupV2Descriptor,
        contributor: PortableBackupV2SnapshotContributor?,
        limits: PortableBackupV2Limits,
        onProgress: (PortableBackupProgressV2) -> Unit,
        shouldCancel: () -> Boolean
    ): Path {
        val normalized = target.toAbsolutePath().normalize()
        if (Files.exists(normalized)) throw LearningDataRecoveryException("Backup target already exists.")
        Files.createDirectories(requireNotNull(normalized.parent))
        var temporary: Path? = null
        var staging: Path? = null
        try {
            onProgress(PortableBackupProgressV2(PortableBackupPhaseV2.PREPARING))
            temporary = Files.createTempFile(normalized.parent, ".learning-engine-backup-v2-", ".tmp")
            staging = Files.createTempDirectory(normalized.parent, ".learning-engine-snapshot-v2-")
            val fullInventory = portableInventory()
            val selective = !descriptor.specificPackageIds.isNullOrEmpty()
            val inventory = if (selective) fullInventory.filterNot { it.first.startsWith("portable/media/") } else fullInventory
            val inventoryBytes = inventory.sumOf { Files.size(it.second) }
            var copiedBytes = 0L
            inventory.forEachIndexed { index, (logical, source) ->
                if (shouldCancel()) throw PortableBackupCancelledException()
                onProgress(PortableBackupProgressV2(
                    PortableBackupPhaseV2.SCANNING_PACKAGES,
                    index.toLong(), inventory.size.toLong(), copiedBytes, inventoryBytes, logical
                ))
                val staged = staging.resolve(logical).normalize().also { require(it.startsWith(staging)) }
                Files.createDirectories(requireNotNull(staged.parent))
                Files.copy(source, staged)
                copiedBytes += Files.size(source)
            }
            if (inventory.none { it.first.startsWith("portable/data/") }) {
                val emptyInstalled = staging.resolve("portable/data/installed-packages.json")
                Files.createDirectories(requireNotNull(emptyInstalled.parent))
                writeUtf8String(emptyInstalled, "{\"schemaVersion\":1,\"records\":[]}")
            }

            var packageEntries = if (selective || !descriptor.includeLearningProgress) {
                val packageIds = descriptor.specificPackageIds?.takeIf { it.isNotEmpty() }
                    ?: extractPackageEntries(staging).map { it.packageId }.toSet()
                scopeStagedDataForPackages(
                    staging, packageIds, descriptor.includeLearningProgress, validateMedia = !selective
                )
            } else {
                extractPackageEntries(staging)
            }
            if (selective) {
                val mediaRoot = roots["media"] ?: roots.getValue("data").resolve("media")
                val selectedContents = readJsonArrayObjects(staging.resolve("portable/data/contents.json"))
                val references = selectedContents.flatMap(::mediaReferences).toSortedSet()
                val mediaBytes = references.sumOf { reference ->
                    val source = mediaRoot.resolve(reference).normalize()
                    if (!source.startsWith(mediaRoot.normalize()) || !Files.isRegularFile(source)) {
                        error("Selected package media is incomplete: missing $reference")
                    }
                    Files.size(source)
                }
                var stagedMediaBytes = 0L
                references.forEachIndexed { index, reference ->
                    if (shouldCancel()) throw PortableBackupCancelledException()
                    onProgress(PortableBackupProgressV2(
                        PortableBackupPhaseV2.CALCULATING_MEDIA,
                        index.toLong(), references.size.toLong(), stagedMediaBytes, mediaBytes, reference
                    ))
                    val source = mediaRoot.resolve(reference).normalize()
                    val targetMedia = staging.resolve("portable/media").resolve(reference).normalize()
                    require(targetMedia.startsWith(staging.resolve("portable/media").normalize()))
                    Files.createDirectories(requireNotNull(targetMedia.parent))
                    Files.copy(source, targetMedia)
                    stagedMediaBytes += Files.size(source)
                }
                packageEntries = extractPackageEntries(staging)
            }

            val stagedData = staging.resolve("portable/data")
            val validationPackageIds = packageEntries.map { it.packageId }.toSet()
            if (selective && validationPackageIds.isNotEmpty()) {
                val stagedScope = buildPackageBackupScope(stagedData, validationPackageIds, descriptor.includeLearningProgress)
                validatePackageBackupScope(stagedScope, requireMedia = true, mediaRoot = staging.resolve("portable/media"))
            }
            if (selective) {
                stagedDomainValidator(roots.keys.associateWith { staging.resolve("portable/$it") })
            }

            val stagedCanonical = mutableListOf<PortableBackupSupplementV2>()
            Files.walk(staging).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { stagedFile ->
                    val logical = staging.relativize(stagedFile).toString().replace('\\', '/')
                    if (logical != V2_MANIFEST_ENTRY && !logical.startsWith("android/")) {
                        stagedCanonical += PortableBackupSupplementV2(logical, logical.substringBefore('/'), logicalType(logical), stagedFile)
                    }
                }
            }

            val supplements = contributor?.snapshot(staging) ?: emptyList()
            val payloads = (stagedCanonical + supplements).sortedBy { it.logicalPath }
            onProgress(PortableBackupProgressV2(PortableBackupPhaseV2.CALCULATING_MEDIA, totalItems = payloads.size.toLong()))
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
                backupSchemaVersion = 2,
                backupId = descriptor.backupId ?: java.util.UUID.randomUUID().toString(),
                appVersion = descriptor.appVersion,
                versionCode = descriptor.versionCode,
                createdAtUtc = clock.instant().toString(),
                sourcePlatform = descriptor.sourcePlatform,
                learnerIds = descriptor.learnerIds.distinct().sorted(),
                selectivePackageIds = descriptor.specificPackageIds.orEmpty().toSortedSet(),
                includedSections = records.map { it.section }.distinct().sorted(),
                packages = packageEntries,
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
            onProgress(PortableBackupProgressV2(PortableBackupPhaseV2.PREPARING_ARCHIVE))
            var writtenBytes = 0L
            val totalPayloadBytes = records.sumOf { it.uncompressedSize }
            ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
                zip.putNextEntry(ZipEntry(V2_MANIFEST_ENTRY).apply { time = 0L })
                zip.write(manifestBytes)
                zip.closeEntry()
                payloads.forEachIndexed { index, payload ->
                    if (shouldCancel()) throw PortableBackupCancelledException()
                    val phase = if (payload.logicalType == "media") PortableBackupPhaseV2.WRITING_MEDIA else PortableBackupPhaseV2.WRITING_DATA
                    onProgress(PortableBackupProgressV2(
                        phase, index.toLong(), payloads.size.toLong(), writtenBytes, totalPayloadBytes, payload.logicalPath
                    ))
                    zip.putNextEntry(ZipEntry(payload.logicalPath).apply { time = 0L })
                    Files.newInputStream(payload.source).buffered(limits.ioBufferBytes).use { it.copyTo(zip, limits.ioBufferBytes) }
                    zip.closeEntry()
                    writtenBytes += Files.size(payload.source)
                }
            }
            forceFile(temporary)
            if (shouldCancel()) throw PortableBackupCancelledException()
            onProgress(PortableBackupProgressV2(PortableBackupPhaseV2.VERIFYING_BACKUP))
            validatePortableBackupV2Internal(temporary, limits)
            onProgress(PortableBackupProgressV2(PortableBackupPhaseV2.FINALIZING))
            publishAtomically(temporary, normalized)
            temporary = null
            onProgress(PortableBackupProgressV2(PortableBackupPhaseV2.COMPLETED, 1, 1, totalPayloadBytes, totalPayloadBytes))
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
                        appendLine("file.$index=$name\t${Files.size(path)}\t${sha256(path)}")
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
                val entry = zip.getEntry(record[0]) ?: error("Missing entry")
                val (size, sha) = zip.getInputStream(entry).use { stream ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(FILE_COMPARISON_BUFFER_SIZE)
                    var totalSize = 0L
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        totalSize += count
                        digest.update(buffer, 0, count)
                    }
                    totalSize to digest.digest().joinToString("") { "%02x".format(it) }
                }
                if (size != record[1].toLong() || sha != record[2]) error("Backup checksum failed.")
                if ("/media/" in record[0] && size == 0L) error("Backup contains empty managed media.")
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

    private fun assertExactRestoreV2(staging: Path, manifest: PortableBackupManifestV2) {
        val livePortable = portableInventory()
        val expectedPortableEntries = manifest.entries.filter { it.logicalPath.startsWith("portable/") }
        val livePaths = livePortable.map { it.first }.sorted()
        val expectedPaths = expectedPortableEntries.map { it.logicalPath }.sorted()
        if (livePaths != expectedPaths) {
            val missing = expectedPaths - livePaths.toSet()
            val extra = livePaths - expectedPaths.toSet()
            error("Restored canonical inventory mismatch: missing=$missing, extra=$extra")
        }
        livePortable.forEach { (logicalPath, livePath) ->
            val stagedPath = staging.resolve(logicalPath)
            if (!Files.isRegularFile(stagedPath)) {
                error("Restored entry missing in staging: $logicalPath")
            }
            val liveSha = sha256(livePath)
            val expectedRecord = expectedPortableEntries.firstOrNull { it.logicalPath == logicalPath }
                ?: error("Entry not found in manifest: $logicalPath")
            if (liveSha != expectedRecord.sha256) {
                error("Restored canonical sha256 mismatch for $logicalPath: expected=${expectedRecord.sha256}, actual=$liveSha")
            }
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
            validateCanonicalArchiveReferences(zip, manifest)
            validateRecordingReferences(zip, declared)
            manifest
        }

    private fun validateCanonicalArchiveReferences(zip: ZipFile, manifest: PortableBackupManifestV2) {
        if (manifest.selectivePackageIds.isEmpty()) return
        val temporary = Files.createTempDirectory("learning-engine-portable-validation-")
        try {
            val dataDir = temporary.resolve("data")
            CANONICAL_PACKAGE_DATA_FILES.forEach { fileName ->
                val entry = zip.getEntry("portable/data/$fileName") ?: return@forEach
                val target = dataDir.resolve(fileName)
                Files.createDirectories(requireNotNull(target.parent))
                zip.getInputStream(entry).use { Files.copy(it, target) }
            }
            val packageIds = manifest.selectivePackageIds
            val scope = buildPackageBackupScope(dataDir, packageIds, includeLearningProgress = true)
            validatePackageBackupScope(scope, requireMedia = false)
            val declaredMedia = manifest.entries.asSequence()
                .map { it.logicalPath }
                .filter { it.startsWith("portable/media/") }
                .map { it.removePrefix("portable/media/") }
                .toSet()
            require(scope.mediaReferences.all(declaredMedia::contains)) {
                "Content references media missing from the portable archive."
            }
        } finally {
            deleteTree(temporary)
        }
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

    private fun validateRecordingReferencesStaged(staging: Path, records: List<PortableBackupEntryV2>) {
        val index = records.singleOrNull { it.logicalPath == "android/recordings/index.json" } ?: return
        val indexFile = staging.resolve(index.logicalPath)
        if (!Files.isRegularFile(indexFile)) error("Recording index file missing from staging.")
        val text = Files.newBufferedReader(indexFile, StandardCharsets.UTF_8).use { it.readText() }
        val paths = Regex("\\\"recordingFile\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").findAll(text).map { it.groupValues[1] }.toList()
        val names = records.map { it.logicalPath }.toSet()
        if (paths.any { unsafeRelativeRecordingPath(it) || "android/recordings/$it" !in names }) {
            error("Recording metadata does not resolve to an archive recording.")
        }
        paths.forEach { rel ->
            val stagedTarget = staging.resolve("android/recordings").resolve(rel).normalize()
            if (!stagedTarget.startsWith(staging.resolve("android/recordings")) || !Files.isRegularFile(stagedTarget)) {
                error("Recording target file missing from staging: $rel")
            }
        }
    }

    private fun extractAndValidateToStaging(
        normalized: Path,
        staging: Path,
        limits: PortableBackupV2Limits
    ): PortableBackupManifestV2 {
        val usable = try {
            normalized.toFile().usableSpace
        } catch (_: Exception) { 0L }

        return ZipFile(normalized.toFile()).use { zip ->
            val zipEntries = zip.entries().asSequence().toList()
            if (zipEntries.size > limits.maxArchiveEntryCount) {
                error("Backup has too many entries: ${zipEntries.size}")
            }
            val names = zipEntries.map { it.name }
            if (names.size != names.toSet().size || names.map(String::lowercase).size != names.map(String::lowercase).toSet().size) {
                error("Backup contains duplicate or case-colliding paths.")
            }
            if (names.any { it != V2_MANIFEST_ENTRY && unsafeV2Name(it) }) {
                error("Backup contains unsafe entry names.")
            }
            val manifestEntry = zip.getEntry(V2_MANIFEST_ENTRY)
                ?: error("Backup v2 manifest is missing.")
            val manifest = zip.getInputStream(manifestEntry).bufferedReader(StandardCharsets.UTF_8).use {
                V2_JSON.decodeFromString<PortableBackupManifestV2>(it.readText())
            }
            if (manifest.backupSchemaVersion != 2) {
                throw UnsupportedSchemaException("Unsupported backup schema version: ${manifest.backupSchemaVersion}", manifest.backupSchemaVersion)
            }
            val requiredBytes = try {
                Math.addExact(Math.multiplyExact(manifest.bytes.totalExpandedBytes, 2L), limits.requireFreeDiskSpaceMarginBytes)
            } catch (_: Exception) { Long.MAX_VALUE }
            if (usable in 1 until requiredBytes) {
                throw InsufficientSpaceException("Insufficient free disk space for safe restore.", requiredBytes, usable)
            }
            val declared = manifest.entries
            if (declared.map { it.logicalPath }.sorted() != names.filter { it != V2_MANIFEST_ENTRY }.sorted()) {
                error("Backup inventory does not match manifest.")
            }
            if (declared.map { it.logicalPath }.distinct().size != declared.size) {
                error("Duplicate manifest entries.")
            }
            if (declared.none { it.logicalPath.startsWith("portable/data/") }) {
                error("Canonical data section is missing.")
            }
            var total = 0L
            declared.forEach { record ->
                if (unsafeV2Name(record.logicalPath)) {
                    error("Unsafe manifest path: ${record.logicalPath}")
                }
                val entry = zip.getEntry(record.logicalPath)
                    ?: error("Declared entry missing: ${record.logicalPath}")
                if (entry.size != record.uncompressedSize || entry.size > limits.maxUncompressedBytesPerEntry) {
                    error("Backup size mismatch for: ${record.logicalPath}")
                }
                total = Math.addExact(total, entry.size)
                if (total > limits.maxTotalExpandedBytes) {
                    error("Backup expanded size is oversized.")
                }
                val compressed = entry.compressedSize
                if ((entry.size > 0 && compressed == 0L) || (compressed > 0 && entry.size.toDouble() / compressed > limits.maxCompressionRatio)) {
                    error("Backup compression ratio is unsafe.")
                }
                val target = staging.resolve(record.logicalPath).normalize()
                if (!target.startsWith(staging)) {
                    error("Staged entry escaped staging directory.")
                }
                Files.createDirectories(requireNotNull(target.parent))
                zip.getInputStream(entry).use { input ->
                    Files.newOutputStream(target).use { output ->
                        val buffer = ByteArray(limits.ioBufferBytes)
                        val digest = MessageDigest.getInstance("SHA-256")
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                        }
                        val computedSha = digest.digest().joinToString("") { "%02x".format(it) }
                        if (computedSha != record.sha256) {
                            error("Checksum mismatch for: ${record.logicalPath}")
                        }
                    }
                }
            }
            manifest
        }
    }

    private fun restorePortableBackupV2Locked(
        source: Path,
        contributorForSafetyBackup: PortableBackupV2SnapshotContributor?,
        consumer: PortableBackupV2RestoreConsumer?,
        limits: PortableBackupV2Limits,
        selectedPackageIds: Set<String>? = null
    ): PortableBackupV2RestoreResult {
        val normalized = source.toAbsolutePath().normalize()
        if (!Files.isRegularFile(normalized)) {
            return PortableBackupV2RestoreResult.ValidationFailed("Backup archive does not exist.")
        }
        val usable = try {
            normalized.toFile().usableSpace
        } catch (_: Exception) { 0L }

        Files.createDirectories(safetyDirectory)
        val staging = Files.createTempDirectory(safetyDirectory, ".learning-engine-restore-v2-")
        try {
            val manifest = try {
                extractAndValidateToStaging(normalized, staging, limits)
            } catch (e: UnsupportedSchemaException) {
                return PortableBackupV2RestoreResult.UnsupportedSchema(e.message ?: "Unsupported schema", e.version)
            } catch (e: InsufficientSpaceException) {
                return PortableBackupV2RestoreResult.InsufficientSpace(e.message ?: "Insufficient space", e.requiredBytes, e.availableBytes)
            } catch (e: Exception) {
                return PortableBackupV2RestoreResult.ValidationFailed("Archive preflight validation failed: ${e.message}", e.message)
            }

            try {
                validateRecordingReferencesStaged(staging, manifest.entries)
                stagedDomainValidator(roots.keys.associateWith { staging.resolve("portable/$it") })
                consumer?.preflight(staging)
                failureHook("restore-v2-preflight-verified", null)
            } catch (e: Exception) {
                return PortableBackupV2RestoreResult.ValidationFailed("Staged domain preflight validation failed: ${e.message}", e.message)
            }

            val safety = safetyDirectory.resolve("safety-v2-${clock.instant().toEpochMilli()}.lebak")
            try {
                failureHook("restore-v2-safety-backup-start", null)
                createPortableBackupV2Locked(
                    safety,
                    PortableBackupV2Descriptor("safety-pre-restore", null, "safety", emptyList()),
                    contributorForSafetyBackup,
                    limits,
                    {},
                    { false }
                )
                validatePortableBackupV2Internal(safety, limits)
                failureHook("restore-v2-safety-backup-verified", null)
            } catch (e: Exception) {
                return PortableBackupV2RestoreResult.SafetyBackupFailed("Safety backup before restore failed.", e.message)
            }

            val capturedPlatformState = consumer?.captureCurrentState()
            try {
                failureHook("restore-v2-live-replace-start", null)
                if (selectedPackageIds == null) {
                    clear()
                    roots.forEach { (rootName, root) ->
                        val stagedRoot = staging.resolve("portable/$rootName")
                        if (Files.exists(stagedRoot)) {
                            Files.walk(stagedRoot).use { paths ->
                                paths.filter { Files.isRegularFile(it) }.forEach { sourceFile ->
                                    val relative = stagedRoot.relativize(sourceFile)
                                    val target = root.resolve(relative)
                                    Files.createDirectories(requireNotNull(target.parent))
                                    Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING)
                                }
                            }
                        }
                    }
                    cleanEmptyDirectories()
                    failureHook("restore-v2-canonical-copied", null)
                    consumer?.applyRestored(staging)
                    failureHook("restore-v2-consumer-applied", null)

                    assertExactRestoreV2(staging, manifest)
                    stagedDomainValidator(roots)
                    consumer?.validateLive()
                    failureHook("restore-v2-post-validation-complete", null)

                    return PortableBackupV2RestoreResult.Success(
                        safetyBackupPath = safety.toString(),
                        restoredEntriesCount = manifest.counts.learningItems.toInt(),
                        appVersion = manifest.appVersion,
                        restoredCounts = manifest.counts,
                        cleanupResult = applySafetyBackupRetention(setOf(safety, normalized), limits)
                    )
                } else {
                    val restoredCounts = packageScopeCounts(
                        buildPackageBackupScope(staging.resolve("portable/data"), selectedPackageIds, includeLearningProgress = true)
                    )
                    applySelectiveRestoreFromStaging(staging, selectedPackageIds)
                    cleanEmptyDirectories()
                    failureHook("restore-v2-canonical-copied", null)
                    consumer?.applyRestored(staging)
                    failureHook("restore-v2-consumer-applied", null)

                    stagedDomainValidator(roots)
                    consumer?.validateLive()
                    failureHook("restore-v2-post-validation-complete", null)

                    return PortableBackupV2RestoreResult.Success(
                        safetyBackupPath = safety.toString(),
                        restoredEntriesCount = restoredCounts.learningItems.toInt(),
                        appVersion = manifest.appVersion,
                        restoredCounts = restoredCounts,
                        cleanupResult = applySafetyBackupRetention(setOf(safety, normalized), limits)
                    )
                }
            } catch (restoreFailure: Exception) {
                try {
                    failureHook("restore-v2-rollback-start", restoreFailure.message)
                    val rollbackStaging = Files.createTempDirectory(safetyDirectory, ".learning-engine-rollback-v2-")
                    try {
                        val safetyManifest = extractAndValidateToStaging(safety, rollbackStaging, limits)
                        clear()
                        roots.forEach { (rootName, root) ->
                            val stagedRoot = rollbackStaging.resolve("portable/$rootName")
                            if (Files.exists(stagedRoot)) {
                                Files.walk(stagedRoot).use { paths ->
                                    paths.filter { Files.isRegularFile(it) }.forEach { sourceFile ->
                                        val relative = stagedRoot.relativize(sourceFile)
                                        val target = root.resolve(relative)
                                        Files.createDirectories(requireNotNull(target.parent))
                                        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING)
                                    }
                                }
                            }
                        }
                        cleanEmptyDirectories()
                        assertExactRestoreV2(rollbackStaging, safetyManifest)
                    } finally {
                        deleteTree(rollbackStaging)
                    }
                    consumer?.rollback(capturedPlatformState)
                    consumer?.validateLive()
                    failureHook("restore-v2-rollback-success", null)
                    return PortableBackupV2RestoreResult.RestoreFailedRolledBack(
                        "Restore failed and previous state was rolled back.",
                        safetyBackupPath = safety.toString(),
                        failureReason = restoreFailure.message ?: "Restore error"
                    )
                } catch (rollbackFailure: Exception) {
                    failureHook("restore-v2-rollback-failed", rollbackFailure.message)
                    return PortableBackupV2RestoreResult.RollbackFailed(
                        "Restore failed and rollback also failed. Safety backup retained at $safety",
                        safetyBackupPath = safety.toString(),
                        restoreFailure = restoreFailure.message ?: "Restore error",
                        rollbackFailure = rollbackFailure.message ?: "Rollback error"
                    )
                }
            }
        } finally {
            deleteTree(staging)
        }
    }

    private fun applySafetyBackupRetention(
        protectedPaths: Set<Path>,
        limits: PortableBackupV2Limits
    ): SafetyBackupCleanupResult = try {
        failureHook("restore-v2-safety-retention-start", null)
        val protected = protectedPaths.map { it.toAbsolutePath().normalize() }.toSet()
        val valid = discoverSafetyBackups(limits).validV2
        val keep = valid.take(SAFETY_V2_RETENTION_COUNT).map { it.path }.toMutableSet().apply { addAll(protected) }
        var deleted = 0
        valid.asSequence().map { it.path }.filterNot(keep::contains).forEach { path ->
            safetyBackupDelete(path)
            deleted++
        }
        SafetyBackupCleanupResult(
            retainedValidV2Count = discoverSafetyBackups(limits).validV2.size,
            deletedValidV2Count = deleted
        )
    } catch (failure: Exception) {
        SafetyBackupCleanupResult(
            retainedValidV2Count = runCatching { discoverSafetyBackups(limits).validV2.size }.getOrDefault(0),
            failureMessage = failure.message ?: "Safety backup cleanup failed."
        )
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
                        val inner = element["records"] ?: element["data"] ?: element["items"]
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
            learningTrajectories = countItems("learning-trajectories.json"),
            studySessions = countItems("study-sessions.json"),
            studyQueues = countItems("study-queues.json"),
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

    private fun clear() = roots.values.sortedByDescending { it.nameCount }.forEach { root ->
        if (Files.exists(root)) {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder())
                    .filter { it != root && !it.startsWith(safetyDirectory) }
                    .forEach(Files::deleteIfExists)
            }
        }
    }

    private fun cleanEmptyDirectories() = roots.values.sortedByDescending { it.nameCount }.forEach { root ->
        if (Files.exists(root)) {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder())
                    .filter { it != root && Files.isDirectory(it) && !it.startsWith(safetyDirectory) }
                    .forEach { dir ->
                        try {
                            Files.delete(dir)
                        } catch (_: Exception) {}
                    }
            }
        }
    }

    private fun extractPackageEntries(dir: Path): List<PortableBackupPackageEntryV2> {
        val installedFile = when {
            Files.isRegularFile(dir.resolve("portable/data/installed-packages.json")) -> dir.resolve("portable/data/installed-packages.json")
            Files.isRegularFile(dir.resolve("data/installed-packages.json")) -> dir.resolve("data/installed-packages.json")
            Files.isRegularFile(dir.resolve("installed-packages.json")) -> dir.resolve("installed-packages.json")
            else -> return emptyList()
        }
        val mediaDir = when {
            Files.isDirectory(dir.resolve("portable/media")) -> dir.resolve("portable/media")
            Files.isDirectory(dir.resolve("media")) -> dir.resolve("media")
            Files.isDirectory(dir.parent?.resolve("media") ?: dir.resolve("media")) -> dir.parent?.resolve("media") ?: dir.resolve("media")
            else -> dir.resolve("media")
        }
        val text = readUtf8String(installedFile)
        val element = try { V2_JSON.parseToJsonElement(text) } catch (_: Exception) { return emptyList() }
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> (element["records"] ?: element["data"] ?: element["items"]) as? JsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return array.mapNotNull { item ->
            if (item !is JsonObject) return@mapNotNull null
            val pkgId = item["packageId"]?.jsonPrimitive?.content
                ?: (item["packageId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                ?: item["id"]?.jsonPrimitive?.content
                ?: return@mapNotNull null
            val name = item["name"]?.jsonPrimitive?.content
                ?: (item["name"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                ?: pkgId
            val version = item["version"]?.jsonPrimitive?.content
                ?: (item["version"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                ?: "1.0.0"
            val contentCount = item["contentCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val learningItemCount = item["learningItemCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

            val pkgMediaFolder = mediaDir.resolve(pkgId)
            val namedMediaFolder = mediaDir.resolve(name)
            val resolvedMediaFolder = when {
                Files.isDirectory(pkgMediaFolder) -> pkgMediaFolder
                Files.isDirectory(namedMediaFolder) -> namedMediaFolder
                else -> pkgMediaFolder
            }
            val mediaCount = if (Files.isDirectory(resolvedMediaFolder)) {
                Files.walk(resolvedMediaFolder).use { paths -> paths.filter { Files.isRegularFile(it) }.count().toInt() }
            } else {
                0
            }
            val fingerprint = "$pkgId:$version:$contentCount"
            PortableBackupPackageEntryV2(
                packageId = pkgId,
                packageName = name,
                version = version,
                contentCount = contentCount,
                learningItemCount = learningItemCount,
                mediaCount = mediaCount,
                fingerprint = fingerprint
            )
        }
    }

    private fun buildPackageBackupScope(
        dataDir: Path,
        selectedPackageIds: Set<String>,
        includeLearningProgress: Boolean
    ): PackageBackupScope {
        val all = CANONICAL_PACKAGE_DATA_FILES.associateWith { readJsonArrayObjects(dataDir.resolve(it)) }
        val installed = all.getValue("installed-packages.json").filter {
            (jsonString(it, "packageId") ?: jsonString(it, "id")) in selectedPackageIds
        }
        val foundPackageIds = installed.mapNotNull { jsonString(it, "packageId") ?: jsonString(it, "id") }.toSet()
        val missing = selectedPackageIds - foundPackageIds
        require(missing.isEmpty()) { "Unknown selected package ID(s): ${missing.sorted().joinToString()}" }
        val installedIds = installed.mapNotNull { jsonString(it, "id") }.toSet()
        val contentPackages = all.getValue("content-packages.json").filter {
            (jsonString(it, "packageId") ?: jsonString(it, "id")) in foundPackageIds
        }
        val libraryIds = contentPackages.flatMap { jsonStringArray(it, "libraryIds") }.toSet()
        val libraries = all.getValue("content-libraries.json").filter { jsonString(it, "id") in libraryIds }
        val contentIds = libraries.flatMap { jsonStringArray(it, "contentIds") }.toSet()
        val contents = all.getValue("contents.json").filter { jsonString(it, "id") in contentIds }
        val items = all.getValue("learning-items.json").filter { jsonString(it, "contentId") in contentIds }
        val itemIds = items.mapNotNull { jsonString(it, "id") }.toSet()

        val memory = if (includeLearningProgress) all.getValue("memory-states.json").filter {
            learningItemReference(it) in itemIds
        } else emptyList()
        val reviews = if (includeLearningProgress) all.getValue("review-events.json").filter {
            val references = reviewEventLearningItemReferences(it)
            references.any(itemIds::contains)
        } else emptyList()
        val trajectories = if (includeLearningProgress) all.getValue("learning-trajectories.json").filter {
            jsonString(it, "contentId") in contentIds
        } else emptyList()
        val sessions = if (includeLearningProgress) all.getValue("study-sessions.json").filter {
            jsonString(it, "installedPackageId") in installedIds
        } else emptyList()
        val sessionIds = sessions.mapNotNull { jsonString(it, "id") }.toSet()
        val queues = if (includeLearningProgress) all.getValue("study-queues.json").filter {
            jsonString(it, "sessionId") in sessionIds
        } else emptyList()
        val media = contents.flatMap(::mediaReferences).toSet()
        return PackageBackupScope(
            packageIds = foundPackageIds,
            installedPackageIds = installedIds,
            libraryIds = libraryIds,
            contentIds = contentIds,
            learningItemIds = itemIds,
            studySessionIds = sessionIds,
            mediaReferences = media,
            records = mapOf(
                "installed-packages.json" to installed,
                "content-packages.json" to contentPackages,
                "content-libraries.json" to libraries,
                "contents.json" to contents,
                "learning-items.json" to items,
                "memory-states.json" to memory,
                "review-events.json" to reviews,
                "learning-trajectories.json" to trajectories,
                "study-sessions.json" to sessions,
                "study-queues.json" to queues
            )
        )
    }

    private fun packageScopeCounts(scope: PackageBackupScope): PortableBackupCountsV2 = PortableBackupCountsV2(
        packages = scope.records.getValue("installed-packages.json").size.toLong(),
        contents = scope.records.getValue("contents.json").size.toLong(),
        learningItems = scope.records.getValue("learning-items.json").size.toLong(),
        memoryStates = scope.records.getValue("memory-states.json").size.toLong(),
        reviewEvents = scope.records.getValue("review-events.json").size.toLong(),
        learningTrajectories = scope.records.getValue("learning-trajectories.json").size.toLong(),
        studySessions = scope.records.getValue("study-sessions.json").size.toLong(),
        studyQueues = scope.records.getValue("study-queues.json").size.toLong(),
        mediaFiles = scope.mediaReferences.size.toLong()
    )

    private fun validatePackageBackupScope(scope: PackageBackupScope, requireMedia: Boolean, mediaRoot: Path? = null) {
        val records = scope.records
        val packageIds = records.getValue("content-packages.json").mapNotNull { jsonString(it, "id") }.toSet()
        val libraryIds = records.getValue("content-libraries.json").mapNotNull { jsonString(it, "id") }.toSet()
        val contentIds = records.getValue("contents.json").mapNotNull { jsonString(it, "id") }.toSet()
        val itemIds = records.getValue("learning-items.json").mapNotNull { jsonString(it, "id") }.toSet()
        val reviewIds = records.getValue("review-events.json").mapNotNull { jsonString(it, "id") }.toSet()
        val sessionIds = records.getValue("study-sessions.json").mapNotNull { jsonString(it, "id") }.toSet()

        require(scope.packageIds == packageIds) { "Selected InstalledPackage is missing its ContentPackage." }
        require(records.getValue("content-packages.json").all { jsonStringArray(it, "libraryIds").all(libraryIds::contains) }) {
            "ContentPackage references missing ContentLibrary."
        }
        require(records.getValue("content-libraries.json").all { jsonStringArray(it, "contentIds").all(contentIds::contains) }) {
            "ContentLibrary references missing Content."
        }
        require(records.getValue("learning-items.json").all { jsonString(it, "contentId") in contentIds }) {
            "LearningItem references missing Content."
        }
        require(records.getValue("memory-states.json").all { learningItemReference(it) in itemIds }) {
            "MemoryState references missing LearningItem."
        }
        require(records.getValue("review-events.json").all { review ->
            val references = reviewEventLearningItemReferences(review)
            references.size == 2 && references.all(itemIds::contains)
        }) { "ReviewEvent references missing LearningItem." }
        require(records.getValue("learning-trajectories.json").all { trajectory ->
            jsonString(trajectory, "contentId") in contentIds &&
                learningTrajectoryEntries(trajectory).all { entry ->
                    jsonString(entry, "sessionId") in sessionIds &&
                        (jsonString(entry, "reviewEventId")?.let(reviewIds::contains) != false)
                }
        }) {
            "LearningTrajectory contains an unresolved Content, StudySession, or ReviewEvent reference."
        }
        require(records.getValue("study-sessions.json").all { session ->
            jsonString(session, "installedPackageId") in scope.installedPackageIds &&
                studySessionContentReferences(session).all(contentIds::contains) &&
                studySessionLearningItemReferences(session).all(itemIds::contains) &&
                studySessionReviewEventReferences(session).all(reviewIds::contains)
        }) { "StudySession contains an unresolved package, Content, LearningItem, or ReviewEvent reference." }
        require(records.getValue("study-queues.json").all { queue ->
            jsonString(queue, "sessionId") in sessionIds &&
                studyQueueLearningItemReferences(queue).all(itemIds::contains) &&
                studyQueueContentReferences(queue).all(contentIds::contains)
        }) { "StudyQueue contains an unresolved StudySession, LearningItem, or Content reference." }
        if (requireMedia) {
            val root = requireNotNull(mediaRoot).normalize()
            require(scope.mediaReferences.all { reference ->
                val file = root.resolve(reference).normalize()
                file.startsWith(root) && Files.isRegularFile(file)
            }) { "Selected package media is incomplete." }
        }
    }

    private fun scopeStagedDataForPackages(
        staging: Path,
        specificPackageIds: Set<String>,
        includeLearningProgress: Boolean,
        validateMedia: Boolean = true
    ): List<PortableBackupPackageEntryV2> {
        val dataDir = staging.resolve("portable/data")
        val scope = buildPackageBackupScope(dataDir, specificPackageIds, includeLearningProgress)
        validatePackageBackupScope(scope, requireMedia = false)
        scope.records.forEach { (fileName, records) ->
            val file = dataDir.resolve(fileName)
            if (!includeLearningProgress && fileName in PROGRESS_DATA_FILES) {
                Files.deleteIfExists(file)
            } else if (Files.isRegularFile(file)) {
                writeCanonicalRecords(file, records)
                require(readJsonArrayObjects(file) == records) { "Canonical filtered records did not round-trip: $fileName" }
            }
        }

        val mediaDir = staging.resolve("portable/media")
        if (Files.isDirectory(mediaDir)) {
            Files.walk(mediaDir).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { file ->
                    val rel = mediaDir.relativize(file).toString().replace('\\', '/')
                val belongsToTarget = rel in scope.mediaReferences
                    if (!belongsToTarget) {
                        Files.deleteIfExists(file)
                    }
                }
            }
            cleanEmptyTree(mediaDir)
        }
        if (validateMedia) {
            val missingMedia = scope.mediaReferences.filterNot { reference ->
                val resolved = mediaDir.resolve(reference).normalize()
                resolved.startsWith(mediaDir.normalize()) && Files.isRegularFile(resolved)
            }
            if (missingMedia.isNotEmpty()) {
                error("Selected package media is incomplete: ${missingMedia.size} referenced file(s) are missing; first=${missingMedia.first()}")
            }
        }

        return extractPackageEntries(staging)
    }

    private fun inspectPackageCompatibility(
        packages: List<PortableBackupPackageEntryV2>
    ): List<PackageRestorePreviewItem> {
        val liveData = roots["data"] ?: return packages.map {
            PackageRestorePreviewItem(
                packageId = it.packageId,
                packageName = it.packageName,
                version = it.version,
                contentCount = it.contentCount,
                learningItemCount = it.learningItemCount,
                mediaCount = it.mediaCount,
                status = PackageCompatibilityStatus.NEW,
                statusDetail = "Chưa cài đặt trên thiết bị này"
            )
        }
        val liveInstalledFile = liveData.resolve("installed-packages.json")
        val livePackages = if (Files.isRegularFile(liveInstalledFile)) {
            extractPackageEntries(liveData)
        } else {
            emptyList()
        }
        val livePkgMap = livePackages.associateBy { it.packageId }

        return packages.map { pkg ->
            val local = livePkgMap[pkg.packageId] ?: livePackages.firstOrNull { it.packageName == pkg.packageName }
            if (local == null) {
                PackageRestorePreviewItem(
                    packageId = pkg.packageId,
                    packageName = pkg.packageName,
                    version = pkg.version,
                    contentCount = pkg.contentCount,
                    learningItemCount = pkg.learningItemCount,
                    mediaCount = pkg.mediaCount,
                    status = PackageCompatibilityStatus.NEW,
                    statusDetail = "Chưa cài đặt trên thiết bị này"
                )
            } else if (local.contentCount == pkg.contentCount && local.version == pkg.version) {
                PackageRestorePreviewItem(
                    packageId = pkg.packageId,
                    packageName = pkg.packageName,
                    version = pkg.version,
                    contentCount = pkg.contentCount,
                    learningItemCount = pkg.learningItemCount,
                    mediaCount = pkg.mediaCount,
                    status = PackageCompatibilityStatus.PRESENT,
                    statusDetail = "Đã có và tương thích"
                )
            } else {
                PackageRestorePreviewItem(
                    packageId = pkg.packageId,
                    packageName = pkg.packageName,
                    version = pkg.version,
                    contentCount = pkg.contentCount,
                    learningItemCount = pkg.learningItemCount,
                    mediaCount = pkg.mediaCount,
                    status = PackageCompatibilityStatus.CONFLICT,
                    statusDetail = "Nội dung cục bộ đã phân kỳ (Sao lưu: ${pkg.contentCount} thẻ, Cục bộ: ${local.contentCount} thẻ)"
                )
            }
        }
    }

    private fun applySelectiveRestoreFromStaging(staging: Path, selectedPackageIds: Set<String>) {
        val liveData = requireNotNull(roots["data"]) { "Live data root missing." }
        val liveMedia = roots["media"]
        val stagedData = staging.resolve("portable/data")
        val scope = buildPackageBackupScope(stagedData, selectedPackageIds, includeLearningProgress = true)
        validatePackageBackupScope(scope, requireMedia = true, mediaRoot = staging.resolve("portable/media"))

        fun merge(fileName: String, staged: List<JsonObject>, remove: (JsonObject) -> Boolean) {
            val target = liveData.resolve(fileName)
            val retained = readJsonArrayObjects(target).filterNot(remove)
            writeRecords(target, retained + staged)
        }

        merge("installed-packages.json", scope.records.getValue("installed-packages.json")) {
            (jsonString(it, "packageId") ?: jsonString(it, "id")) in scope.packageIds
        }
        merge("content-packages.json", scope.records.getValue("content-packages.json")) {
            (jsonString(it, "packageId") ?: jsonString(it, "id")) in scope.packageIds
        }
        merge("content-libraries.json", scope.records.getValue("content-libraries.json")) {
            jsonString(it, "id") in scope.libraryIds
        }
        merge("contents.json", scope.records.getValue("contents.json")) { jsonString(it, "id") in scope.contentIds }
        merge("learning-items.json", scope.records.getValue("learning-items.json")) { jsonString(it, "id") in scope.learningItemIds }
        merge("memory-states.json", scope.records.getValue("memory-states.json")) {
            learningItemReference(it) in scope.learningItemIds
        }
        val stagedReviewIds = scope.records.getValue("review-events.json").mapNotNull { jsonString(it, "id") }.toSet()
        merge("review-events.json", scope.records.getValue("review-events.json")) {
            jsonString(it, "id") in stagedReviewIds || reviewEventLearningItemReferences(it).any(scope.learningItemIds::contains)
        }
        merge("learning-trajectories.json", scope.records.getValue("learning-trajectories.json")) {
            jsonString(it, "contentId") in scope.contentIds
        }
        val liveSelectedSessionIds = readJsonArrayObjects(liveData.resolve("study-sessions.json"))
            .filter { jsonString(it, "installedPackageId") in scope.installedPackageIds }
            .mapNotNull { jsonString(it, "id") }.toSet()
        merge("study-sessions.json", scope.records.getValue("study-sessions.json")) {
            jsonString(it, "installedPackageId") in scope.installedPackageIds
        }
        merge("study-queues.json", scope.records.getValue("study-queues.json")) {
            jsonString(it, "sessionId") in liveSelectedSessionIds || jsonString(it, "sessionId") in scope.studySessionIds
        }

        if (liveMedia != null) {
            val stagedMedia = staging.resolve("portable/media").normalize()
            scope.mediaReferences.forEach { reference ->
                val source = stagedMedia.resolve(reference).normalize()
                val target = liveMedia.resolve(reference).normalize()
                require(source.startsWith(stagedMedia) && target.startsWith(liveMedia.normalize()))
                Files.createDirectories(requireNotNull(target.parent))
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun readJsonArrayObjects(file: Path): List<JsonObject> {
        if (!Files.isRegularFile(file)) return emptyList()
        val text = readUtf8String(file)
        val element = try { V2_JSON.parseToJsonElement(text) } catch (_: Exception) { return emptyList() }
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> (element["records"] ?: element["data"] ?: element["items"]) as? JsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return array.filterIsInstance<JsonObject>()
    }

    private fun jsonString(obj: JsonObject?, field: String): String? {
        val value = obj?.get(field) ?: return null
        return when (value) {
            is JsonObject -> value["value"]?.jsonPrimitive?.content
            else -> value.jsonPrimitive.content
        }?.takeUnless { it == "null" }
    }

    private fun jsonStringArray(obj: JsonObject?, field: String): List<String> =
        (obj?.get(field) as? JsonArray).orEmpty().mapNotNull { element ->
            element.jsonPrimitive.content.takeUnless { it == "null" }
        }

    private fun learningItemReference(obj: JsonObject): String? =
        jsonString(obj, "learningItemId") ?: jsonString(obj, "itemId")

    private fun reviewEventLearningItemReferences(obj: JsonObject): List<String> = listOfNotNull(
        learningItemReference(obj["stateBefore"] as? JsonObject ?: JsonObject(emptyMap())),
        learningItemReference(obj["stateAfter"] as? JsonObject ?: JsonObject(emptyMap()))
    )

    private fun studySessionLearningItemReferences(obj: JsonObject): Set<String> = buildSet {
        addAll(jsonStringArray(obj, "reviewedItemIds"))
        listOf("currentLearningItemId", "pendingReviewLearningItemId").mapNotNullTo(this) { jsonString(obj, it) }
        (obj["undoableReview"] as? JsonObject)?.let { undo ->
            jsonString(undo, "learningItemId")?.let(::add)
            addAll(jsonStringArray(undo, "reviewedItemIdsBefore"))
            learningItemReference(undo["memoryStateBefore"] as? JsonObject ?: JsonObject(emptyMap()))?.let(::add)
        }
    }

    private fun learningTrajectoryEntries(obj: JsonObject): List<JsonObject> =
        (obj["chains"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().flatMap { chain ->
            (chain["entries"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
        }

    private fun studySessionContentReferences(obj: JsonObject): Set<String> = buildSet {
        listOf("includedContentIds", "reviewedContentIds", "introducedContentIds", "lapsedContentIds")
            .forEach { addAll(jsonStringArray(obj, it)) }
        (obj["undoableReview"] as? JsonObject)?.let { undo ->
            jsonString(undo, "contentId")?.let(::add)
            addAll(jsonStringArray(undo, "reviewedContentIdsBefore"))
            addAll(jsonStringArray(undo, "lapsedContentIdsBefore"))
        }
    }

    private fun studySessionReviewEventReferences(obj: JsonObject): Set<String> = buildSet {
        jsonString(obj, "pendingReviewEventId")?.let(::add)
        jsonString(obj["undoableReview"] as? JsonObject, "reviewEventId")?.let(::add)
    }

    private fun studyQueueLearningItemReferences(obj: JsonObject): Set<String> = buildSet {
        addAll(jsonStringArray(obj, "learningItemIds"))
        addAll(jsonStringArray(obj, "fixedPracticeMembership"))
        listOf("itemOrigins", "itemContentIds", "practiceReinforcementStates", "coverageReinforcementStates")
            .forEach { field -> addAll((obj[field] as? JsonObject)?.keys.orEmpty()) }
        (obj["practiceMembershipUndo"] as? JsonObject)?.let { undo ->
            jsonString(undo, "learningItemId")?.let(::add)
            addAll(jsonStringArray(undo, "previousMembership"))
            addAll(jsonStringArray(undo, "previousQueue"))
        }
        (obj["coverageReinforcementUndo"] as? JsonObject)?.let { undo ->
            jsonString(undo, "learningItemId")?.let(::add)
            addAll(jsonStringArray(undo, "discardedTail"))
        }
    }

    private fun studyQueueContentReferences(obj: JsonObject): Set<String> =
        (obj["itemContentIds"] as? JsonObject)?.values.orEmpty().mapNotNullTo(linkedSetOf()) { value ->
            value.jsonPrimitive.content.takeUnless { it == "null" }
        }

    private fun mediaReferences(obj: JsonObject): List<String> = listOf(
        "primaryAudio",
        "translatedAudio",
        "image",
        "exampleAudio",
        "exampleTranslatedAudio"
    ).mapNotNull { field ->
        (jsonString(obj, field) ?: jsonString(obj["media"] as? JsonObject, field))
            ?.trim()?.replace('\\', '/')?.takeIf(String::isNotEmpty)
    }

    private fun estimatedJsonBytes(obj: JsonObject): Long =
        obj.toString().toByteArray(StandardCharsets.UTF_8).size.toLong()

    private fun writeCanonicalRecords(file: Path, records: List<JsonObject>) {
        val existing = V2_JSON.parseToJsonElement(readUtf8String(file))
        val output = if (existing is JsonObject) {
            JsonObject((existing - "data" - "items") + ("records" to JsonArray(records)))
        } else {
            JsonArray(records)
        }
        writeUtf8String(file, V2_JSON.encodeToString(output))
    }

    private fun writeRecords(file: Path, records: List<JsonObject>) {
        if (Files.isRegularFile(file)) {
            writeCanonicalRecords(file, records)
        } else {
            Files.createDirectories(requireNotNull(file.parent))
            writeUtf8String(
                file,
                V2_JSON.encodeToString(JsonObject(mapOf(
                    "schemaVersion" to kotlinx.serialization.json.JsonPrimitive(1),
                    "records" to JsonArray(records)
                )))
            )
        }
    }

    private fun readUtf8String(path: Path): String = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    private fun writeUtf8String(path: Path, text: String) {
        Files.write(path, text.toByteArray(StandardCharsets.UTF_8))
    }

    private fun cleanEmptyTree(dir: Path) {
        if (!Files.isDirectory(dir)) return
        Files.walk(dir).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach {
                if (Files.isDirectory(it) && it != dir) {
                    try {
                        Files.delete(it)
                    } catch (_: Exception) {}
                }
            }
        }
    }

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
        private val V2_JSON = Json { encodeDefaults = true; ignoreUnknownKeys = true; prettyPrint = true }
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
