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

private class UnsupportedSchemaException(message: String, val version: Int) : RuntimeException(message)
private class InsufficientSpaceException(message: String, val requiredBytes: Long, val availableBytes: Long) : RuntimeException(message)

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

    fun restorePortableBackupV2(
        source: Path,
        operationActive: Boolean = false,
        contributorForSafetyBackup: PortableBackupV2SnapshotContributor? = null,
        consumer: PortableBackupV2RestoreConsumer? = null,
        limits: PortableBackupV2Limits = PortableBackupV2Limits(),
        selectedPackageIds: Set<String>? = null
    ): PortableBackupV2RestoreResult {
        if (operationActive) return PortableBackupV2RestoreResult.Busy()
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
        val selected = installed.filter { pkg ->
            val packageId = jsonString(pkg, "packageId") ?: jsonString(pkg, "id")
            descriptor.specificPackageIds.isNullOrEmpty() || packageId in descriptor.specificPackageIds
        }
        if (!descriptor.specificPackageIds.isNullOrEmpty()) {
            val found = selected.mapNotNull { jsonString(it, "packageId") ?: jsonString(it, "id") }.toSet()
            val missing = descriptor.specificPackageIds - found
            if (missing.isNotEmpty()) error("Unknown selected package ID(s): ${missing.sorted().joinToString()}")
        }

        val contentPackages = readJsonArrayObjects(dataRoot.resolve("content-packages.json"))
        val contentLibraries = readJsonArrayObjects(dataRoot.resolve("content-libraries.json"))
        val contents = readJsonArrayObjects(dataRoot.resolve("contents.json"))
        val learningItems = readJsonArrayObjects(dataRoot.resolve("learning-items.json"))
        val memoryStates = readJsonArrayObjects(dataRoot.resolve("memory-states.json"))
        val reviewEvents = readJsonArrayObjects(dataRoot.resolve("review-events.json"))
        val libraryContentIds = contentLibraries.associate { library ->
            jsonString(library, "id").orEmpty() to jsonStringArray(library, "contentIds").toSet()
        }
        val packagePlans = selected.map { installedPackage ->
            val packageId = jsonString(installedPackage, "packageId") ?: jsonString(installedPackage, "id")
                ?: error("Installed package identity is missing.")
            val packageName = jsonString(installedPackage, "name") ?: packageId
            val packageRecord = contentPackages.firstOrNull {
                (jsonString(it, "packageId") ?: jsonString(it, "id")) == packageId
            }
            val ownedContentIds = packageRecord?.let { record ->
                jsonStringArray(record, "libraryIds").flatMap { libraryContentIds[it].orEmpty() }.toSet()
            }.orEmpty()
            val packageContents = contents.filter { content ->
                val contentId = jsonString(content, "id")
                contentId in ownedContentIds || mediaReferences(content).any { it == packageName || it.startsWith("$packageName/") }
            }
            val contentIds = packageContents.mapNotNull { jsonString(it, "id") }.toSet()
            val packageItems = learningItems.filter { jsonString(it, "contentId") in contentIds }
            val itemIds = packageItems.mapNotNull { jsonString(it, "id") }.toSet()
            val packageMemory = if (descriptor.includeLearningProgress)
                memoryStates.filter { learningItemReference(it) in itemIds } else emptyList()
            val packageReviews = if (descriptor.includeLearningProgress)
                reviewEvents.filter { learningItemReference(it) in itemIds } else emptyList()
            val references = packageContents.flatMap(::mediaReferences).toSortedSet()
            val mediaFiles = references.map { reference ->
                val resolved = mediaRoot.resolve(reference).normalize()
                if (!resolved.startsWith(mediaRoot.normalize()) || !Files.isRegularFile(resolved)) {
                    error("Selected package media is incomplete: missing $reference")
                }
                resolved
            }
            val dataBytes = (listOf(installedPackage) + listOfNotNull(packageRecord) +
                contentLibraries.filter { jsonString(it, "id") in jsonStringArray(packageRecord, "libraryIds") } +
                packageContents + packageItems + packageMemory + packageReviews).sumOf(::estimatedJsonBytes)
            PortableBackupPackagePlanV2(
                packageId = packageId,
                packageName = packageName,
                version = jsonString(installedPackage, "version") ?: "1.0.0",
                contentCount = packageContents.size,
                learningItemCount = packageItems.size,
                memoryStateCount = packageMemory.size,
                reviewEventCount = packageReviews.size,
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
            portableInventory().forEach { (logical, source) ->
                val staged = staging.resolve(logical).normalize().also { require(it.startsWith(staging)) }
                Files.createDirectories(requireNotNull(staged.parent))
                Files.copy(source, staged)
            }

            val packageEntries = if (!descriptor.specificPackageIds.isNullOrEmpty() || !descriptor.includeLearningProgress) {
                val packageIds = descriptor.specificPackageIds?.takeIf { it.isNotEmpty() }
                    ?: extractPackageEntries(staging).map { it.packageId }.toSet()
                scopeStagedDataForPackages(staging, packageIds, descriptor.includeLearningProgress)
            } else {
                extractPackageEntries(staging)
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
                    limits
                )
                validatePortableBackupV2Internal(safety, limits)
                failureHook("restore-v2-safety-backup-verified", null)
            } catch (e: Exception) {
                return PortableBackupV2RestoreResult.SafetyBackupFailed("Safety backup before restore failed.", e.message)
            }

            val capturedPlatformState = consumer?.captureCurrentState()
            try {
                failureHook("restore-v2-live-replace-start", null)
                if (selectedPackageIds == null || selectedPackageIds.isEmpty()) {
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
                        restoredEntriesCount = manifest.entries.size,
                        appVersion = manifest.appVersion
                    )
                } else {
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
                        restoredEntriesCount = selectedPackageIds.size,
                        appVersion = manifest.appVersion
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
            is JsonObject -> (element["data"] ?: element["items"] ?: element["records"]) as? JsonArray ?: JsonArray(emptyList())
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

    private fun scopeStagedDataForPackages(
        staging: Path,
        specificPackageIds: Set<String>,
        includeLearningProgress: Boolean
    ): List<PortableBackupPackageEntryV2> {
        val installedFile = staging.resolve("portable/data/installed-packages.json")
        val targetPkgIds = mutableSetOf<String>()
        val targetPackageNames = mutableSetOf<String>()
        if (Files.isRegularFile(installedFile)) {
            val text = readUtf8String(installedFile)
            val element = V2_JSON.parseToJsonElement(text)
            val (array, isEnvelope) = when (element) {
                is JsonArray -> element to false
                is JsonObject -> ((element["data"] ?: element["items"] ?: element["records"]) as? JsonArray ?: JsonArray(emptyList())) to true
                else -> JsonArray(emptyList()) to false
            }
            val filtered = array.filterIsInstance<JsonObject>().filter { obj ->
                val pId = obj["packageId"]?.jsonPrimitive?.content
                    ?: (obj["packageId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    ?: obj["id"]?.jsonPrimitive?.content
                val idVal = obj["id"]?.jsonPrimitive?.content
                (pId != null && pId in specificPackageIds) || (idVal != null && idVal in specificPackageIds)
            }
            filtered.forEach { obj ->
                obj["packageId"]?.jsonPrimitive?.content?.let { targetPkgIds.add(it) }
                (obj["packageId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content?.let { targetPkgIds.add(it) }
                obj["id"]?.jsonPrimitive?.content?.let { targetPkgIds.add(it) }
                obj["name"]?.jsonPrimitive?.content?.let { targetPackageNames.add(it) }
            }
            targetPkgIds.addAll(specificPackageIds)
            val newJson = if (isEnvelope && element is JsonObject) {
                JsonObject(element + ("data" to JsonArray(filtered)))
            } else {
                JsonArray(filtered)
            }
            writeUtf8String(installedFile, V2_JSON.encodeToString(newJson))
        } else {
            targetPkgIds.addAll(specificPackageIds)
        }

        val contentPkgFile = staging.resolve("portable/data/content-packages.json")
        val targetLibraryIds = mutableSetOf<String>()
        if (Files.isRegularFile(contentPkgFile)) {
            filterJsonArrayFile(contentPkgFile) { obj ->
                val pId = obj["packageId"]?.jsonPrimitive?.content ?: obj["id"]?.jsonPrimitive?.content
                val matches = pId != null && pId in targetPkgIds
                if (matches) {
                    (obj["libraryIds"] as? JsonArray).orEmpty().forEach { libraryId ->
                        libraryId.jsonPrimitive.content.let(targetLibraryIds::add)
                    }
                    obj["name"]?.jsonPrimitive?.content?.let(targetPackageNames::add)
                }
                matches
            }
        }

        val contentsFile = staging.resolve("portable/data/contents.json")
        val targetContentIds = mutableSetOf<String>()
        val targetMediaReferences = mutableSetOf<String>()
        val contentLibrariesFile = staging.resolve("portable/data/content-libraries.json")
        if (Files.isRegularFile(contentLibrariesFile) && targetLibraryIds.isNotEmpty()) {
            filterJsonArrayFile(contentLibrariesFile) { obj ->
                val libraryId = obj["id"]?.jsonPrimitive?.content
                val matches = libraryId != null && libraryId in targetLibraryIds
                if (matches) {
                    (obj["contentIds"] as? JsonArray).orEmpty().forEach { contentId ->
                        contentId.jsonPrimitive.content.let(targetContentIds::add)
                    }
                }
                matches
            }
        }
        if (Files.isRegularFile(contentsFile)) {
            val text = readUtf8String(contentsFile)
            val element = V2_JSON.parseToJsonElement(text)
            val (array, isEnvelope) = when (element) {
                is JsonArray -> element to false
                is JsonObject -> ((element["data"] ?: element["items"] ?: element["records"]) as? JsonArray ?: JsonArray(emptyList())) to true
                else -> JsonArray(emptyList()) to false
            }
            val filtered = array.filterIsInstance<JsonObject>().filter { obj ->
                val id = obj["id"]?.jsonPrimitive?.content
                    ?: (obj["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    ?: return@filter false
                val primaryAudio = obj["primaryAudio"]?.jsonPrimitive?.content
                    ?: (obj["media"] as? JsonObject)?.get("primaryAudio")?.jsonPrimitive?.content
                val transAudio = obj["translatedAudio"]?.jsonPrimitive?.content
                    ?: (obj["media"] as? JsonObject)?.get("translatedAudio")?.jsonPrimitive?.content
                val img = obj["image"]?.jsonPrimitive?.content
                    ?: (obj["media"] as? JsonObject)?.get("image")?.jsonPrimitive?.content
                val customPkg = (obj["customFields"] as? JsonObject)?.get("packageId")?.jsonPrimitive?.content

                val matchesByIdentity = targetPkgIds.any { pkg ->
                    primaryAudio?.startsWith("$pkg/") == true ||
                    transAudio?.startsWith("$pkg/") == true ||
                    img?.startsWith("$pkg/") == true ||
                    primaryAudio?.startsWith(pkg) == true ||
                    customPkg == pkg
                }
                val matchesByName = targetPackageNames.any { packageName ->
                    mediaReferences(obj).any { it == packageName || it.startsWith("$packageName/") }
                }
                val matches = id in targetContentIds || matchesByIdentity || matchesByName
                if (matches) {
                    targetContentIds.add(id)
                    listOf(
                        "primaryAudio",
                        "translatedAudio",
                        "image",
                        "exampleAudio",
                        "exampleTranslatedAudio"
                    ).forEach { field ->
                        val reference = obj[field]?.jsonPrimitive?.content
                            ?: (obj["media"] as? JsonObject)?.get(field)?.jsonPrimitive?.content
                        reference?.trim()?.replace('\\', '/')
                            ?.takeIf { it.isNotEmpty() && it != "null" }
                            ?.let(targetMediaReferences::add)
                    }
                }
                matches
            }
            val newJson = if (isEnvelope && element is JsonObject) {
                JsonObject(element + ("data" to JsonArray(filtered)))
            } else {
                JsonArray(filtered)
            }
            writeUtf8String(contentsFile, V2_JSON.encodeToString(newJson))
        }

        val itemsFile = staging.resolve("portable/data/learning-items.json")
        val targetItemIds = mutableSetOf<String>()
        if (Files.isRegularFile(itemsFile)) {
            filterJsonArrayFile(itemsFile) { obj ->
                val cId = obj["contentId"]?.jsonPrimitive?.content
                    ?: (obj["contentId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                val matches = cId != null && cId in targetContentIds
                if (matches) {
                    val itemId = obj["id"]?.jsonPrimitive?.content
                        ?: (obj["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    if (itemId != null) targetItemIds.add(itemId)
                }
                matches
            }
        }

        val memoryFile = staging.resolve("portable/data/memory-states.json")
        if (Files.isRegularFile(memoryFile)) {
            if (!includeLearningProgress) Files.delete(memoryFile) else
                filterJsonArrayFile(memoryFile) { obj ->
                    val itemId = obj["learningItemId"]?.jsonPrimitive?.content
                        ?: (obj["learningItemId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                        ?: obj["itemId"]?.jsonPrimitive?.content
                    itemId != null && itemId in targetItemIds
                }
        }

        val reviewFile = staging.resolve("portable/data/review-events.json")
        if (Files.isRegularFile(reviewFile)) {
            if (!includeLearningProgress) Files.delete(reviewFile) else filterJsonArrayFile(reviewFile) { obj ->
                val itemId = obj["learningItemId"]?.jsonPrimitive?.content
                    ?: (obj["learningItemId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    ?: obj["itemId"]?.jsonPrimitive?.content
                itemId != null && itemId in targetItemIds
            }
        }

        val trajFile = staging.resolve("portable/data/learning-trajectories.json")
        if (Files.isRegularFile(trajFile)) {
            if (!includeLearningProgress) Files.delete(trajFile) else filterJsonArrayFile(trajFile) { obj ->
                val itemId = obj["learningItemId"]?.jsonPrimitive?.content
                    ?: (obj["learningItemId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                itemId != null && itemId in targetItemIds
            }
        }

        val mediaDir = staging.resolve("portable/media")
        if (Files.isDirectory(mediaDir)) {
            Files.walk(mediaDir).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { file ->
                    val rel = mediaDir.relativize(file).toString().replace('\\', '/')
                    val belongsToTarget = rel in targetMediaReferences
                    if (!belongsToTarget) {
                        Files.deleteIfExists(file)
                    }
                }
            }
            cleanEmptyTree(mediaDir)
        }
        val missingMedia = targetMediaReferences.filterNot { reference ->
            val resolved = mediaDir.resolve(reference).normalize()
            resolved.startsWith(mediaDir.normalize()) && Files.isRegularFile(resolved)
        }
        if (missingMedia.isNotEmpty()) {
            error("Selected package media is incomplete: ${missingMedia.size} referenced file(s) are missing; first=${missingMedia.first()}")
        }

        return extractPackageEntries(staging)
    }

    private fun filterJsonArrayFile(file: Path, predicate: (JsonObject) -> Boolean): List<JsonObject> {
        val text = readUtf8String(file)
        val element = try { V2_JSON.parseToJsonElement(text) } catch (_: Exception) { return emptyList() }
        val (array, isEnvelope) = when (element) {
            is JsonArray -> element to false
            is JsonObject -> ((element["data"] ?: element["items"] ?: element["records"]) as? JsonArray ?: JsonArray(emptyList())) to true
            else -> JsonArray(emptyList()) to false
        }
        val filtered = array.filterIsInstance<JsonObject>().filter(predicate)
        val newJson = if (isEnvelope && element is JsonObject) {
            JsonObject(element + ("data" to JsonArray(filtered)))
        } else {
            JsonArray(filtered)
        }
        writeUtf8String(file, V2_JSON.encodeToString(newJson))
        return filtered
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

        val stagedInstalled = staging.resolve("portable/data/installed-packages.json")
        val liveInstalled = liveData.resolve("installed-packages.json")
        val stagedPkgIds = mutableSetOf<String>()
        if (Files.isRegularFile(stagedInstalled)) {
            val stagedObjects = readJsonArrayObjects(stagedInstalled).filter { obj ->
                val pId = obj["packageId"]?.jsonPrimitive?.content
                    ?: (obj["packageId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    ?: obj["id"]?.jsonPrimitive?.content
                pId != null && pId in selectedPackageIds
            }
            stagedObjects.forEach { obj ->
                obj["packageId"]?.jsonPrimitive?.content?.let { stagedPkgIds.add(it) }
                (obj["packageId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content?.let { stagedPkgIds.add(it) }
                obj["id"]?.jsonPrimitive?.content?.let { stagedPkgIds.add(it) }
            }
            stagedPkgIds.addAll(selectedPackageIds)

            val liveObjects = if (Files.isRegularFile(liveInstalled)) {
                readJsonArrayObjects(liveInstalled).filter { obj ->
                    val pId = obj["packageId"]?.jsonPrimitive?.content
                        ?: (obj["packageId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                        ?: obj["id"]?.jsonPrimitive?.content
                    pId == null || pId !in stagedPkgIds
                }
            } else {
                emptyList()
            }
            writeJsonArrayObjects(liveInstalled, liveObjects + stagedObjects)
        } else {
            stagedPkgIds.addAll(selectedPackageIds)
        }

        val stagedContentsFile = staging.resolve("portable/data/contents.json")
        val liveContentsFile = liveData.resolve("contents.json")
        val targetContentIds = mutableSetOf<String>()
        if (Files.isRegularFile(stagedContentsFile)) {
            val stagedContents = readJsonArrayObjects(stagedContentsFile).filter { obj ->
                val id = obj["id"]?.jsonPrimitive?.content
                    ?: (obj["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    ?: return@filter false
                val primaryAudio = obj["primaryAudio"]?.jsonPrimitive?.content
                    ?: (obj["media"] as? JsonObject)?.get("primaryAudio")?.jsonPrimitive?.content
                val transAudio = obj["translatedAudio"]?.jsonPrimitive?.content
                    ?: (obj["media"] as? JsonObject)?.get("translatedAudio")?.jsonPrimitive?.content
                val img = obj["image"]?.jsonPrimitive?.content
                    ?: (obj["media"] as? JsonObject)?.get("image")?.jsonPrimitive?.content
                val customPkg = (obj["customFields"] as? JsonObject)?.get("packageId")?.jsonPrimitive?.content

                val matches = stagedPkgIds.any { pkg ->
                    primaryAudio?.startsWith("$pkg/") == true ||
                    transAudio?.startsWith("$pkg/") == true ||
                    img?.startsWith("$pkg/") == true ||
                    primaryAudio?.startsWith(pkg) == true ||
                    customPkg == pkg
                }
                if (matches) targetContentIds.add(id)
                matches
            }

            val liveContents = if (Files.isRegularFile(liveContentsFile)) {
                readJsonArrayObjects(liveContentsFile).filter { obj ->
                    val id = obj["id"]?.jsonPrimitive?.content
                        ?: (obj["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    id == null || id !in targetContentIds
                }
            } else {
                emptyList()
            }
            writeJsonArrayObjects(liveContentsFile, liveContents + stagedContents)
        }

        val stagedItemsFile = staging.resolve("portable/data/learning-items.json")
        val liveItemsFile = liveData.resolve("learning-items.json")
        val targetItemIds = mutableSetOf<String>()
        if (Files.isRegularFile(stagedItemsFile)) {
            val stagedItems = readJsonArrayObjects(stagedItemsFile).filter { obj ->
                val cId = obj["contentId"]?.jsonPrimitive?.content
                    ?: (obj["contentId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                val matches = cId != null && cId in targetContentIds
                if (matches) {
                    val id = obj["id"]?.jsonPrimitive?.content
                        ?: (obj["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    if (id != null) targetItemIds.add(id)
                }
                matches
            }

            val liveItems = if (Files.isRegularFile(liveItemsFile)) {
                readJsonArrayObjects(liveItemsFile).filter { obj ->
                    val id = obj["id"]?.jsonPrimitive?.content
                        ?: (obj["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    id == null || id !in targetItemIds
                }
            } else {
                emptyList()
            }
            writeJsonArrayObjects(liveItemsFile, liveItems + stagedItems)
        }

        val stagedMemoryFile = staging.resolve("portable/data/memory-states.json")
        val liveMemoryFile = liveData.resolve("memory-states.json")
        if (Files.isRegularFile(stagedMemoryFile)) {
            val stagedMemory = readJsonArrayObjects(stagedMemoryFile).filter { obj ->
                val itemId = obj["learningItemId"]?.jsonPrimitive?.content
                    ?: (obj["learningItemId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    ?: obj["itemId"]?.jsonPrimitive?.content
                itemId != null && itemId in targetItemIds
            }
            val liveMemory = if (Files.isRegularFile(liveMemoryFile)) {
                readJsonArrayObjects(liveMemoryFile).filter { obj ->
                    val itemId = obj["learningItemId"]?.jsonPrimitive?.content
                        ?: (obj["learningItemId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                        ?: obj["itemId"]?.jsonPrimitive?.content
                    itemId == null || itemId !in targetItemIds
                }
            } else {
                emptyList()
            }
            writeJsonArrayObjects(liveMemoryFile, liveMemory + stagedMemory)
        }

        val stagedReviewFile = staging.resolve("portable/data/review-events.json")
        val liveReviewFile = liveData.resolve("review-events.json")
        if (Files.isRegularFile(stagedReviewFile)) {
            val stagedReviews = readJsonArrayObjects(stagedReviewFile).filter { obj ->
                val itemId = obj["learningItemId"]?.jsonPrimitive?.content
                    ?: (obj["learningItemId"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                    ?: obj["itemId"]?.jsonPrimitive?.content
                itemId != null && itemId in targetItemIds
            }
            val liveReviews = if (Files.isRegularFile(liveReviewFile)) {
                readJsonArrayObjects(liveReviewFile)
            } else {
                emptyList()
            }
            val liveReviewIds = liveReviews.mapNotNull {
                it["id"]?.jsonPrimitive?.content ?: (it["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
            }.toSet()
            val newStagedReviews = stagedReviews.filter { obj ->
                val id = obj["id"]?.jsonPrimitive?.content ?: (obj["id"] as? JsonObject)?.get("value")?.jsonPrimitive?.content
                id == null || id !in liveReviewIds
            }
            writeJsonArrayObjects(liveReviewFile, liveReviews + newStagedReviews)
        }

        if (liveMedia != null) {
            stagedPkgIds.forEach { pkgId ->
                val stagedPkgMedia = staging.resolve("portable/media/$pkgId")
                val livePkgMedia = liveMedia.resolve(pkgId)
                if (Files.isDirectory(stagedPkgMedia)) {
                    if (Files.exists(livePkgMedia)) {
                        deleteTree(livePkgMedia)
                    }
                    Files.createDirectories(livePkgMedia)
                    Files.walk(stagedPkgMedia).use { paths ->
                        paths.filter { Files.isRegularFile(it) }.forEach { file ->
                            val rel = stagedPkgMedia.relativize(file)
                            val target = livePkgMedia.resolve(rel)
                            Files.createDirectories(requireNotNull(target.parent))
                            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
            }
        }
    }

    private fun readJsonArrayObjects(file: Path): List<JsonObject> {
        if (!Files.isRegularFile(file)) return emptyList()
        val text = readUtf8String(file)
        val element = try { V2_JSON.parseToJsonElement(text) } catch (_: Exception) { return emptyList() }
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> (element["data"] ?: element["items"] ?: element["records"]) as? JsonArray ?: JsonArray(emptyList())
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

    private fun writeJsonArrayObjects(file: Path, objects: List<JsonObject>) {
        Files.createDirectories(requireNotNull(file.parent))
        val existing = if (Files.isRegularFile(file)) {
            try { V2_JSON.parseToJsonElement(readUtf8String(file)) } catch (_: Exception) { null }
        } else null
        val newJson = if (existing is JsonObject && "data" in existing) {
            JsonObject(existing + ("data" to JsonArray(objects)))
        } else {
            JsonArray(objects)
        }
        writeUtf8String(file, V2_JSON.encodeToString(newJson))
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
