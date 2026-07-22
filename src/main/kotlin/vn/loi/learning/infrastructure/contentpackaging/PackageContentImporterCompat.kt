package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.LegacyPackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

/**
 * Router tương thích giữa bundle OPD3, ZIP legacy và cặp JSON + PKG nhị phân.
 *
 * Định dạng container được xác định từ signature, không chỉ từ phần mở rộng:
 *
 * - ZIP `.opd3` được nhập bởi [PackageBundleImporter];
 * - ZIP `.pkg` được nhập bởi [JvmPackageContentImporter];
 * - magic `OPD3` được ghép với JSON cùng basename và nhập bởi importer legacy hiện có.
 */
class PackageContentImporterCompat(
    private val bundleImporter: PackageBundleImporter,
    private val legacyImporter: JvmPackageContentImporter,
    private val binaryPairImporter: LegacyPackageContentImporter? = null,
    private val formatDetector: JvmPackageFormatDetector = JvmPackageFormatDetector(),
    private val pairResolver: JvmOpd3PairResolver = JvmOpd3PairResolver()
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate
    ): ImportedPackageContent =
        when (formatDetector.detect(Path.of(candidate.source))) {
            JvmPackageFormat.ZIP_ARCHIVE ->
                if (candidate.source.endsWith(".opd3", ignoreCase = true)) {
                    bundleImporter.importContent(candidate)
                } else {
                    legacyImporter.importContent(candidate)
                }

            JvmPackageFormat.OPD3_BINARY_PAIR -> {
                val importer = binaryPairImporter
                    ?: throw IllegalStateException(
                        "OPD3 binary pair import requires configured media storage."
                    )
                importer.importContent(
                    pairResolver.resolve(Path.of(candidate.source))
                )
            }
        }
}
