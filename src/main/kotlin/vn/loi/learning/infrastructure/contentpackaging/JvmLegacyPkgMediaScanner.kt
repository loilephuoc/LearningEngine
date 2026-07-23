package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile
import vn.loi.learning.application.contentpackaging.LegacyPkgMediaScanner
import vn.loi.learning.infrastructure.contentmedia.LegacyOpd3MediaArchiveReader

/**
 * Adapter JVM phục vụ kiểm tra danh sách file media trong tệp PKG legacy.
 * Hỗ trợ cả định dạng binary OPD3 lẫn ZIP archive.
 */
class JvmLegacyPkgMediaScanner(
    private val formatDetector: JvmPackageFormatDetector = JvmPackageFormatDetector(),
    private val opd3ArchiveReader: LegacyOpd3MediaArchiveReader = LegacyOpd3MediaArchiveReader()
) : LegacyPkgMediaScanner {

    override fun scanMediaEntries(packageSource: String): List<String> {
        val path = Paths.get(packageSource)
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return emptyList()
        }

        return try {
            val format = formatDetector.detect(path)
            when (format) {
                JvmPackageFormat.OPD3_BINARY_PAIR -> {
                    opd3ArchiveReader.readEntries(path).map { it.fileName }
                }

                JvmPackageFormat.ZIP_ARCHIVE -> {
                    ZipFile(path.toFile()).use { zip ->
                        zip.entries().asSequence()
                            .filter { !it.isDirectory }
                            .map { it.name }
                            .toList()
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
