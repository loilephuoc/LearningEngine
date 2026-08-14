package vn.loi.learning.infrastructure.contentpackaging

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Comparator
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.Opd3PathValidator
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.application.port.ContentMediaStorage

class Opd3BundleMediaExtractor(
    private val archiveReader: Opd3ArchiveReader = JvmOpd3ArchiveReader(),
    private val mediaStorage: ContentMediaStorage,
    private val stagingDirectoryFactory: () -> Path = {
        Files.createTempDirectory("learning-engine-package-media-")
    },
    private val stagedFileWriter: (Path, ByteArray) -> Unit = { path, bytes -> Files.write(path, bytes) }
) {
    fun prepare(
        packagePath: Path,
        packageName: String,
        manifestHashes: Map<String, String> = emptyMap(),
        progressListener: ((processed: Int, total: Int, stage: PackageImportProgressStage, details: String?) -> Unit)? = null,
        cancellationSignal: PackageImportCancellationSignal? = null
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
                    val bytes = ByteArrayOutputStream().also { output ->
                        archive.getInputStream(entry).use { it.copyTo(output) }
                    }.toByteArray()
                    (manifestHashes[entry.name] ?: manifestHashes[rawLogicalPath] ?: manifestHashes["media/$rawLogicalPath"])
                        ?.let { expected -> require(sha256(bytes).equals(expected, ignoreCase = true)) { "Media checksum mismatch for '${entry.name}': expected $expected, got ${sha256(bytes)}" } }

                    val effectiveFileName = when {
                        rawLogicalPath.startsWith("$safePackageName/") -> rawLogicalPath.removePrefix("$safePackageName/")
                        rawLogicalPath.startsWith("$packageName/") -> rawLogicalPath.removePrefix("$packageName/")
                        else -> rawLogicalPath
                    }
                    val stagedPath = stagingDirectory.resolve("entry-$index").normalize()
                    require(stagedPath.startsWith(stagingDirectory))
                    stagedFileWriter(stagedPath, bytes)
                    entries += StagedMediaEntry(
                        packageName = safePackageName,
                        fileName = effectiveFileName,
                        relativePath = "$safePackageName/${effectiveFileName.replace('\\', '/')}",
                        stagedPath = stagedPath
                    )
                }
            }
            return PreparedPackageMedia(mediaStorage, stagingDirectory, entries)
        } catch (failure: Throwable) {
            deleteTree(stagingDirectory)
            throw failure
        }
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "m4a", "ogg", "jpg", "jpeg", "png", "webp")
        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        internal fun deleteTree(directory: Path) {
            if (Files.notExists(directory)) return
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
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
    private val entries: List<StagedMediaEntry>
) {
    private val createdFiles = mutableListOf<Path>()
    val assets: List<ContentMediaAsset> = entries.map {
        ContentMediaAsset(it.packageName, it.fileName, it.relativePath)
    }

    fun commit() {
        try {
            entries.forEach { entry ->
                val bytes = Files.readAllBytes(entry.stagedPath)
                val existing = mediaStorage.resolve(entry.relativePath)
                if (existing != null) {
                    require(Files.readAllBytes(existing).contentEquals(bytes)) {
                        "Media destination collision has different bytes: ${entry.relativePath}"
                    }
                    return@forEach
                }
                try {
                    val asset = mediaStorage.store(entry.packageName, entry.fileName, bytes)
                    mediaStorage.resolve(asset.relativePath)?.let(createdFiles::add)
                } catch (failure: Throwable) {
                    mediaStorage.resolve(entry.relativePath)?.let { Files.deleteIfExists(it) }
                    throw failure
                }
            }
            Opd3BundleMediaExtractor.deleteTree(stagingDirectory)
        } catch (failure: Throwable) {
            rollback()
            throw failure
        }
    }

    fun rollback() {
        createdFiles.asReversed().forEach { runCatching { Files.deleteIfExists(it) } }
        createdFiles.clear()
        Opd3BundleMediaExtractor.deleteTree(stagingDirectory)
    }
}
