package vn.loi.learning.infrastructure.contentpackaging

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import vn.loi.learning.application.contentpackaging.LegacyTopicDiscoveryFile
import vn.loi.learning.application.contentpackaging.LegacyTopicFileKind
import vn.loi.learning.application.contentpackaging.LegacyTopicFolderReader
import vn.loi.learning.application.contentpackaging.UnsupportedPackageTypeException

class JvmLegacyTopicFolderReader(
    private val formatDetector:
    JvmPackageFormatDetector =
        JvmPackageFormatDetector()
) : LegacyTopicFolderReader {

    override fun read(
        folder: String
    ): List<LegacyTopicDiscoveryFile> {
        val directory =
            Paths.get(folder)

        require(
            Files.isDirectory(
                directory
            )
        ) {
            "Legacy topic discovery source must be an existing directory: $directory"
        }

        return Files.list(
            directory
        ).use { paths ->
            paths
                .filter(
                    Files::isRegularFile
                )
                .map(
                    ::toDiscoveryFile
                )
                .sorted(
                    compareBy(
                        LegacyTopicDiscoveryFile::source
                    )
                )
                .toList()
        }
    }

    private fun toDiscoveryFile(
        path: Path
    ): LegacyTopicDiscoveryFile {
        val fileName =
            path.fileName.toString()
        val kind =
            when (
                fileName
                    .substringAfterLast(
                        delimiter = '.',
                        missingDelimiterValue = ""
                    )
                    .lowercase(
                        Locale.ROOT
                    )
            ) {
                "json" ->
                    LegacyTopicFileKind.JSON

                "pkg" ->
                    LegacyTopicFileKind.PKG

                else ->
                    LegacyTopicFileKind.UNSUPPORTED
            }
        val filesystemReadable =
            Files.isReadable(
                path
            )
        val inspection =
            when {
                kind == LegacyTopicFileKind.UNSUPPORTED ->
                    FileInspection(
                        readable =
                            filesystemReadable,
                        supportedFormat = false
                    )

                !filesystemReadable ->
                    FileInspection(
                        readable = false,
                        supportedFormat = true
                    )

                kind == LegacyTopicFileKind.JSON ->
                    FileInspection(
                        readable = true,
                        supportedFormat = true
                    )

                else ->
                    inspectPackageFormat(
                        path
                    )
            }

        return LegacyTopicDiscoveryFile(
            source =
                path.toString(),
            fileName =
                fileName,
            kind =
                kind,
            readable =
                inspection.readable,
            supportedFormat =
                inspection.supportedFormat
        )
    }

    private fun inspectPackageFormat(
        path: Path
    ): FileInspection =
        try {
            formatDetector.detect(
                path
            )
            FileInspection(
                readable = true,
                supportedFormat = true
            )
        } catch (_: UnsupportedPackageTypeException) {
            FileInspection(
                readable = true,
                supportedFormat = false
            )
        } catch (_: IOException) {
            FileInspection(
                readable = false,
                supportedFormat = true
            )
        }

    private data class FileInspection(
        val readable: Boolean,
        val supportedFormat: Boolean
    )
}
