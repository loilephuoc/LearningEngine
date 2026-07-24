package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import vn.loi.learning.application.contentpackaging.MissingOpd3JsonPairException
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.application.contentpackaging.PackageScanner
import vn.loi.learning.application.contentpackaging.UnsupportedPackageTypeException

/**
 * PackageScanner cho việc chọn đơn nhất một file package (.opd3, .pkg, hoặc .json).
 *
 * Chỉ xử lý đúng duy nhất file nguồn được chọn và cặp companion tương ứng (nếu là legacy pair).
 * Tuyệt đối KHÔNG tự động quét hay import các file đồng cấp không liên quan trong cùng thư mục.
 */
class JvmFileScopedPackageScanner(
    private val selectedPath: Path,
    private val pairResolver: JvmOpd3PairResolver = JvmOpd3PairResolver()
) : PackageScanner {

    override fun scan(): List<PackageScanCandidate> {
        val path = selectedPath.toAbsolutePath().normalize()
        require(Files.exists(path) && Files.isRegularFile(path)) {
            "Selected package file does not exist: $selectedPath"
        }

        val fileName = path.fileName.toString().lowercase(Locale.ROOT)

        return when {
            fileName.endsWith(".opd3") -> {
                listOf(PackageScanCandidate(source = path.toString()))
            }

            fileName.endsWith(".pkg") -> {
                val formatDetector = JvmPackageFormatDetector()
                val format = try {
                    formatDetector.detect(path)
                } catch (e: Exception) {
                    JvmPackageFormat.OPD3_BINARY_PAIR
                }

                if (format == JvmPackageFormat.ZIP_ARCHIVE) {
                    listOf(PackageScanCandidate(source = path.toString()))
                } else {
                    pairResolver.resolve(path)
                    listOf(PackageScanCandidate(source = path.toString()))
                }
            }

            fileName.endsWith(".json") -> {
                val baseName = fileName.substringBeforeLast('.')
                val parent = path.parent
                val expectedPkgName = "$baseName.pkg"

                val matchingPkg = if (parent != null && Files.exists(parent)) {
                    Files.list(parent).use { paths ->
                        paths.filter(Files::isRegularFile)
                            .filter { it.fileName.toString().lowercase(Locale.ROOT) == expectedPkgName }
                            .findFirst()
                            .orElse(null)
                    }
                } else null

                if (matchingPkg == null) {
                    throw MissingOpd3JsonPairException(path.toString())
                }

                listOf(PackageScanCandidate(source = matchingPkg.toString()))
            }

            else -> {
                throw UnsupportedPackageTypeException(path.toString())
            }
        }
    }
}
