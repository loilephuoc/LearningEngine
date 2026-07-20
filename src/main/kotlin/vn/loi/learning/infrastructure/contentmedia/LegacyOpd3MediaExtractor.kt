package vn.loi.learning.infrastructure.contentmedia

import java.nio.file.Path
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage

class LegacyOpd3MediaExtractor(
    private val archiveReader: LegacyOpd3MediaArchiveReader,
    private val mediaStorage: ContentMediaStorage
) : PackageMediaExtractor {

    override fun extract(
        packageFile: Path,
        packageName: String
    ): List<ContentMediaAsset> {
        require(packageName.isNotBlank()) {
            "Package name must not be blank."
        }

        val entries =
            archiveReader.readEntries(
                packageFile
            )

        val extractedAssets =
            ArrayList<ContentMediaAsset>(
                entries.size
            )

        entries.forEach { entry ->
            val content =
                archiveReader.readBytes(
                    packageFile = packageFile,
                    entry = entry
                )

            extractedAssets +=
                mediaStorage.store(
                    packageName = packageName,
                    fileName = entry.fileName,
                    content = content
                )
        }

        return extractedAssets
    }
}