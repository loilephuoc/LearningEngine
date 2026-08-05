package vn.loi.learning.infrastructure.contentpackaging

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.Opd3PathValidator
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.application.port.ContentMediaStorage

class Opd3BundleMediaExtractor(
    private val archiveReader: Opd3ArchiveReader = JvmOpd3ArchiveReader(),
    private val mediaStorage: ContentMediaStorage
) {
    fun extract(
        packagePath: Path,
        packageName: String,
        manifestHashes: Map<String, String> = emptyMap(),
        progressListener: ((processed: Int, total: Int, stage: PackageImportProgressStage, details: String?) -> Unit)? = null,
        cancellationSignal: PackageImportCancellationSignal? = null
    ): List<ContentMediaAsset> {
        require(packageName.isNotBlank()) { "Package name must not be blank." }

        cancellationSignal?.checkCancelled()
        progressListener?.invoke(0, 0, PackageImportProgressStage.OPENING_MEDIA, "Opening OPD3 bundle media archive")

        val extractedAssets = mutableListOf<ContentMediaAsset>()
        val newlyCreatedFiles = mutableListOf<Path>()

        val safePackageName = packageName.trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "package" }

        try {
            archiveReader.open(packagePath).use { archive ->
                val allEntries = archive.entries().asSequence().toList()
                val mediaEntries = allEntries.filter { entry ->
                    if (entry.isDirectory) return@filter false
                    val name = entry.name
                    if (!name.startsWith("media/") || name.length <= 6) return@filter false
                    val ext = name.substringAfterLast('.', "").lowercase()
                    ext in SUPPORTED_EXTENSIONS
                }

                val total = mediaEntries.size
                if (total == 0) return emptyList()

                progressListener?.invoke(0, total, PackageImportProgressStage.INDEXING_MEDIA, "Indexed $total media entries")

                mediaEntries.forEachIndexed { index, entry ->
                    cancellationSignal?.checkCancelled()

                    val rawLogicalPath = entry.name.removePrefix("media/").removePrefix("/")
                    require(Opd3PathValidator.isSafeMediaPath(rawLogicalPath)) {
                        "Unsafe media asset path traversal in archive: '${entry.name}'"
                    }

                    if (index % 25 == 0 || index == total - 1) {
                        progressListener?.invoke(
                            index + 1,
                            total,
                            PackageImportProgressStage.EXTRACTING_MEDIA,
                            "Extracting media files (${index + 1} / $total)"
                        )
                    }

                    val outStream = ByteArrayOutputStream()
                    archive.getInputStream(entry).use { inputStream ->
                        inputStream.copyTo(outStream)
                    }
                    val bytes = outStream.toByteArray()

                    val expectedHash = manifestHashes[entry.name]
                        ?: manifestHashes[rawLogicalPath]
                        ?: manifestHashes["media/$rawLogicalPath"]
                    if (expectedHash != null) {
                        val actualHash = MessageDigest.getInstance("SHA-256")
                            .digest(bytes)
                            .joinToString("") { "%02x".format(it) }
                        require(actualHash.equals(expectedHash, ignoreCase = true)) {
                            "Media checksum mismatch for '${entry.name}': expected $expectedHash, got $actualHash"
                        }
                    }

                    val effectiveFileName = if (rawLogicalPath.startsWith("$safePackageName/")) {
                        rawLogicalPath.removePrefix("$safePackageName/")
                    } else if (rawLogicalPath.startsWith("$packageName/")) {
                        rawLogicalPath.removePrefix("$packageName/")
                    } else {
                        rawLogicalPath
                    }

                    val asset = mediaStorage.store(
                        packageName = safePackageName,
                        fileName = effectiveFileName,
                        content = bytes
                    )
                    extractedAssets.add(asset)

                    mediaStorage.resolve(asset.relativePath)?.let { path ->
                        newlyCreatedFiles.add(path)
                    }
                }
            }
            return extractedAssets
        } catch (e: Exception) {
            rollbackFiles(newlyCreatedFiles)
            throw e
        }
    }

    fun rollback(assets: List<ContentMediaAsset>) {
        assets.forEach { asset ->
            mediaStorage.resolve(asset.relativePath)?.let { path ->
                runCatching { Files.deleteIfExists(path) }
            }
        }
    }

    private fun rollbackFiles(files: List<Path>) {
        files.forEach { path ->
            runCatching { Files.deleteIfExists(path) }
        }
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "wav", "m4a", "ogg", "jpg", "jpeg", "png", "webp"
        )
    }
}
