package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.application.contentpackaging.PackageScanner

/**
 * PackageScanner dành cho filesystem JVM.
 *
 * Scanner chỉ tìm các file package được hỗ trợ trực tiếp trong thư mục nguồn:
 * - bundle package `.opd3`;
 * - legacy package `.pkg`.
 *
 * Việc đọc, nhận diện định dạng và xác thực nội dung package thuộc các bước sau.
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
                .filter { path ->
                    Files.isRegularFile(path)
                }
                .filter { path ->
                    isSupportedPackageFile(path)
                }
                .map { path ->
                    PackageScanCandidate(
                        source = path.toString()
                    )
                }
                .sorted(
                    compareBy(
                        PackageScanCandidate::source
                    )
                )
                .toList()
        }
    }

    private fun isSupportedPackageFile(
        path: Path
    ): Boolean {
        val fileName =
            path.fileName.toString()

        return SUPPORTED_EXTENSIONS.any { extension ->
            fileName.endsWith(
                extension,
                ignoreCase = true
            )
        }
    }

    private companion object {

        val SUPPORTED_EXTENSIONS =
            listOf(
                ".opd3",
                ".pkg"
            )
    }
}