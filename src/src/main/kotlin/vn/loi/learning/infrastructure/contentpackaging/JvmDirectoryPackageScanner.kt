package vn.loi.learning.infrastructure.contentpackaging

import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.application.contentpackaging.PackageScanner
import java.nio.file.Files
import java.nio.file.Path

/**
 * PackageScanner dành cho filesystem JVM.
 *
 * Scanner chỉ tìm các file OPD3 trực tiếp trong thư mục nguồn.
 * Việc đọc và xác thực nội dung package thuộc các bước sau.
 */
class JvmDirectoryPackageScanner(
    private val directory: Path
) : PackageScanner {

    override fun scan(): List<PackageScanCandidate> {
        require(Files.isDirectory(directory)) {
            "Package scan source must be an existing directory: $directory"
        }

        return Files.list(directory).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) }
                .filter { path ->
                    path.fileName.toString().endsWith(OPD3_EXTENSION, ignoreCase = true)
                }
                .map { path -> PackageScanCandidate(path.toString()) }
                .sorted(compareBy(PackageScanCandidate::source))
                .toList()
        }
    }

    private companion object {
        const val OPD3_EXTENSION = ".opd3"
    }
}
