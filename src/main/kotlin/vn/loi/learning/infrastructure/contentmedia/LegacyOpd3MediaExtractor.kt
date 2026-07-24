package vn.loi.learning.infrastructure.contentmedia

import java.io.RandomAccessFile
import java.nio.file.Path
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.application.port.ContentMediaStorage

class LegacyOpd3MediaExtractor(
    private val archiveReader: LegacyOpd3MediaArchiveReader,
    private val mediaStorage: ContentMediaStorage
) : PackageMediaExtractor {

    override fun extract(
        packageFile: Path,
        packageName: String,
        progressListener: ((processed: Int, total: Int, stage: PackageImportProgressStage, details: String?) -> Unit)?,
        cancellationSignal: PackageImportCancellationSignal?
    ): List<ContentMediaAsset> {
        require(packageName.isNotBlank()) {
            "Package name must not be blank."
        }

        cancellationSignal?.checkCancelled()
        progressListener?.invoke(0, 0, PackageImportProgressStage.OPENING_MEDIA, "Opening legacy media package")

        val entries = archiveReader.readEntries(packageFile, cancellationSignal)
        val total = entries.size

        progressListener?.invoke(0, total, PackageImportProgressStage.INDEXING_MEDIA, "Indexed $total media entries")

        val extractedAssets = ArrayList<ContentMediaAsset>(total)

        if (total == 0) return extractedAssets

        RandomAccessFile(packageFile.toFile(), "r").use { fileHandle ->
            entries.forEachIndexed { index, entry ->
                cancellationSignal?.checkCancelled()

                if (index % 25 == 0 || index == total - 1) {
                    progressListener?.invoke(
                        index + 1,
                        total,
                        PackageImportProgressStage.EXTRACTING_MEDIA,
                        "Extracting media files (${index + 1} / $total)"
                    )
                }

                val content = archiveReader.readBytesFromHandle(
                    file = fileHandle,
                    packageFile = packageFile,
                    entry = entry
                )

                extractedAssets += mediaStorage.store(
                    packageName = packageName,
                    fileName = entry.fileName,
                    content = content
                )
            }
        }

        return extractedAssets
    }

    override fun extract(
        packageFile: Path,
        packageName: String
    ): List<ContentMediaAsset> = extract(packageFile, packageName, null, null)
}