package vn.loi.learning.infrastructure.contentpackaging

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Comparator
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.Opd3PathValidator
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.application.port.ContentMediaStorage

enum class PackageNamespaceOwnership {
    EMPTY_NAMESPACE,
    ACTIVE_NAMESPACE,
    ORPHAN_NAMESPACE
}

class Opd3BundleMediaExtractor(
    private val archiveReader: Opd3ArchiveReader = JvmOpd3ArchiveReader(),
    private val mediaStorage: ContentMediaStorage,
    private val stagingDirectoryFactory: () -> Path = {
        Files.createTempDirectory("learning-engine-package-media-")
    },
    private val stagedFileWriter: ((Path, ByteArray) -> Unit)? = null
) {
    fun resolveExistingPackageDirectory(packageName: String): Path? =
        mediaStorage.resolvePackageDirectory(packageName)

    fun prepare(
        packagePath: Path,
        packageName: String,
        manifestHashes: Map<String, String> = emptyMap(),
        progressListener: ((processed: Int, total: Int, stage: PackageImportProgressStage, details: String?) -> Unit)? = null,
        cancellationSignal: PackageImportCancellationSignal? = null,
        ownership: PackageNamespaceOwnership = PackageNamespaceOwnership.ACTIVE_NAMESPACE
    ): PreparedPackageMedia {
        require(packageName.isNotBlank()) { "Package name must not be blank." }
        cancellationSignal?.checkCancelled()
        progressListener?.invoke(0, 0, PackageImportProgressStage.OPENING_MEDIA, "Opening OPD3 bundle media archive")

        val stagingDirectory = stagingDirectoryFactory().toAbsolutePath().normalize()
        Files.createDirectories(stagingDirectory)
        val safePackageName = packageName.trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "package" }
        val entries = mutableListOf<StagedMediaEntry>()

        try {
            archiveReader.open(packagePath).use { archive ->
                val mediaEntries = archive.entries().asSequence().filter { entry ->
                    !entry.isDirectory && entry.name.startsWith("media/") && entry.name.length > 6 &&
                        entry.name.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS
                }.toList()
                progressListener?.invoke(0, mediaEntries.size, PackageImportProgressStage.INDEXING_MEDIA, "Indexed ${mediaEntries.size} media entries")

                mediaEntries.forEachIndexed { index, entry ->
                    cancellationSignal?.checkCancelled()
                    val rawLogicalPath = entry.name.removePrefix("media/").removePrefix("/")
                    require(Opd3PathValidator.isSafeMediaPath(rawLogicalPath)) {
                        "Unsafe media asset path traversal in archive: '${entry.name}'"
                    }
                    progressListener?.invoke(index + 1, mediaEntries.size, PackageImportProgressStage.EXTRACTING_MEDIA, "Staging media files (${index + 1} / ${mediaEntries.size})")

                    val stagedPath = stagingDirectory.resolve("entry-$index").normalize()
                    require(stagedPath.startsWith(stagingDirectory))

                    if (stagedFileWriter != null) {
                        val bytes = archive.getInputStream(entry).use { it.readBytes() }
                        val actualSha = sha256(bytes)
                        (manifestHashes[entry.name] ?: manifestHashes[rawLogicalPath] ?: manifestHashes["media/$rawLogicalPath"])
                            ?.let { expected -> require(actualSha.equals(expected, ignoreCase = true)) { "Media checksum mismatch for '${entry.name}': expected $expected, got $actualSha" } }
                        stagedFileWriter.invoke(stagedPath, bytes)
                    } else {
                        val digest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        Files.newOutputStream(stagedPath).buffered(DEFAULT_BUFFER_SIZE).use { out ->
                            archive.getInputStream(entry).buffered(DEFAULT_BUFFER_SIZE).use { input ->
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    digest.update(buffer, 0, read)
                                    out.write(buffer, 0, read)
                                }
                            }
                        }
                        val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                        (manifestHashes[entry.name] ?: manifestHashes[rawLogicalPath] ?: manifestHashes["media/$rawLogicalPath"])
                            ?.let { expected -> require(actualSha.equals(expected, ignoreCase = true)) { "Media checksum mismatch for '${entry.name}': expected $expected, got $actualSha" } }
                    }

                    val effectiveFileName = when {
                        rawLogicalPath.startsWith("$safePackageName/") -> rawLogicalPath.removePrefix("$safePackageName/")
                        rawLogicalPath.startsWith("$packageName/") -> rawLogicalPath.removePrefix("$packageName/")
                        else -> rawLogicalPath
                    }
                    entries += StagedMediaEntry(
                        packageName = safePackageName,
                        fileName = effectiveFileName,
                        relativePath = "$safePackageName/${effectiveFileName.replace('\\', '/')}",
                        stagedPath = stagedPath
                    )
                }
            }
            return PreparedPackageMedia(mediaStorage, stagingDirectory, entries, ownership)
        } catch (failure: Throwable) {
            deleteTree(stagingDirectory)
            throw failure
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 65536
        private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "m4a", "ogg", "jpg", "jpeg", "png", "webp")
        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        internal fun deleteTree(directory: Path) {
            if (Files.notExists(directory)) return
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }

        internal fun copyDirectory(source: Path, target: Path) {
            Files.createDirectories(target)
            Files.walk(source).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { file ->
                    val relative = source.relativize(file)
                    val dest = target.resolve(relative)
                    Files.createDirectories(requireNotNull(dest.parent))
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        internal fun streamsEqual(p1: Path, p2: Path, bufferSize: Int = DEFAULT_BUFFER_SIZE): Boolean {
            if (Files.size(p1) != Files.size(p2)) return false
            Files.newInputStream(p1).buffered(bufferSize).use { s1 ->
                Files.newInputStream(p2).buffered(bufferSize).use { s2 ->
                    val b1 = ByteArray(bufferSize)
                    val b2 = ByteArray(bufferSize)
                    while (true) {
                        val r1 = s1.read(b1)
                        val r2 = s2.read(b2)
                        if (r1 != r2) return false
                        if (r1 < 0) return true
                        if (!b1.sliceArray(0 until r1).contentEquals(b2.sliceArray(0 until r2))) return false
                    }
                }
            }
        }
    }
}

internal data class StagedMediaEntry(
    val packageName: String,
    val fileName: String,
    val relativePath: String,
    val stagedPath: Path
)

class PreparedPackageMedia internal constructor(
    private val mediaStorage: ContentMediaStorage,
    private val stagingDirectory: Path,
    private val entries: List<StagedMediaEntry>,
    val ownership: PackageNamespaceOwnership = PackageNamespaceOwnership.ACTIVE_NAMESPACE
) {
    private val createdFiles = mutableListOf<Path>()
    private val displacedOrphanNamespaces = mutableMapOf<String, Path>()
    val assets: List<ContentMediaAsset> = entries.map {
        ContentMediaAsset(it.packageName, it.fileName, it.relativePath)
    }

    fun commit() {
        try {
            val packageNames = entries.map { it.packageName }.distinct()
            if (ownership == PackageNamespaceOwnership.ORPHAN_NAMESPACE) {
                packageNames.forEach { pkgName ->
                    val existingPkgDir = mediaStorage.resolvePackageDirectory(pkgName)
                    if (existingPkgDir != null && Files.isDirectory(existingPkgDir)) {
                        val rollbackBackup = requireNotNull(existingPkgDir.parent).resolve(".rollback-$pkgName-${System.nanoTime()}")
                        try {
                            Files.move(existingPkgDir, rollbackBackup, StandardCopyOption.ATOMIC_MOVE)
                        } catch (_: Exception) {
                            Opd3BundleMediaExtractor.copyDirectory(existingPkgDir, rollbackBackup)
                            Opd3BundleMediaExtractor.deleteTree(existingPkgDir)
                        }
                        displacedOrphanNamespaces[pkgName] = rollbackBackup
                    }
                }
            }

            entries.forEach { entry ->
                val existing = mediaStorage.resolve(entry.relativePath)
                if (existing != null && ownership == PackageNamespaceOwnership.ACTIVE_NAMESPACE) {
                    require(Opd3BundleMediaExtractor.streamsEqual(entry.stagedPath, existing)) {
                        "Media destination collision has different bytes: ${entry.relativePath}"
                    }
                    return@forEach
                }
                try {
                    val asset = mediaStorage.storeStream(entry.packageName, entry.fileName, entry.stagedPath)
                    mediaStorage.resolve(asset.relativePath)?.let(createdFiles::add)
                } catch (failure: Throwable) {
                    mediaStorage.resolve(entry.relativePath)?.let { Files.deleteIfExists(it) }
                    throw failure
                }
            }
            displacedOrphanNamespaces.values.forEach { Opd3BundleMediaExtractor.deleteTree(it) }
            displacedOrphanNamespaces.clear()
            Opd3BundleMediaExtractor.deleteTree(stagingDirectory)
        } catch (failure: Throwable) {
            rollback()
            throw failure
        }
    }

    fun rollback() {
        createdFiles.asReversed().forEach { runCatching { Files.deleteIfExists(it) } }
        createdFiles.clear()
        displacedOrphanNamespaces.forEach { (pkgName, rollbackBackup) ->
            if (Files.exists(rollbackBackup)) {
                val targetDir = requireNotNull(rollbackBackup.parent).resolve(pkgName)
                Opd3BundleMediaExtractor.deleteTree(targetDir)
                try {
                    Files.move(rollbackBackup, targetDir, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: Exception) {
                    Opd3BundleMediaExtractor.copyDirectory(rollbackBackup, targetDir)
                    Opd3BundleMediaExtractor.deleteTree(rollbackBackup)
                }
            }
        }
        displacedOrphanNamespaces.clear()
        Opd3BundleMediaExtractor.deleteTree(stagingDirectory)
    }
}
