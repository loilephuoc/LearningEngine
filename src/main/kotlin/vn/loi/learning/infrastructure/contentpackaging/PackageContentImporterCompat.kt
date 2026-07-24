package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.LegacyPackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportProgressEvent
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

/**
 * Router tương thích giữa bundle OPD3, ZIP legacy và cặp JSON + PKG nhị phân.
 *
 * Định dạng container được xác định từ signature và đuôi mở rộng:
 *
 * - Nguồn `.json` luôn là thành viên của cặp legacy JSON + PKG -> [binaryPairImporter];
 * - ZIP `.opd3` được nhập bởi [PackageBundleImporter];
 * - ZIP `.pkg` được nhập bởi [JvmPackageContentImporter];
 * - magic `OPD3` binary `.pkg` được nhập bởi [binaryPairImporter].
 */
class PackageContentImporterCompat(
    private val bundleImporter: PackageBundleImporter,
    private val legacyImporter: JvmPackageContentImporter,
    private val binaryPairImporter: LegacyPackageContentImporter? = null,
    private val formatDetector: JvmPackageFormatDetector = JvmPackageFormatDetector(),
    private val pairResolver: JvmOpd3PairResolver = JvmOpd3PairResolver()
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate,
        progressListener: ((event: PackageImportProgressEvent) -> Unit)?,
        cancellationSignal: PackageImportCancellationSignal?
    ): ImportedPackageContent {
        val path = Path.of(candidate.source)
        val isJsonSource = candidate.source.endsWith(".json", ignoreCase = true)

        if (isJsonSource) {
            val importer = binaryPairImporter
                ?: throw IllegalStateException(
                    "OPD3 binary pair import requires configured media storage."
                )
            return importer.importContent(
                candidate = pairResolver.resolve(path),
                progressListener = progressListener,
                cancellationSignal = cancellationSignal
            )
        }

        return when (formatDetector.detect(path)) {
            JvmPackageFormat.ZIP_ARCHIVE ->
                if (candidate.source.endsWith(".opd3", ignoreCase = true)) {
                    bundleImporter.importContent(candidate, progressListener, cancellationSignal)
                } else {
                    legacyImporter.importContent(candidate, progressListener, cancellationSignal)
                }

            JvmPackageFormat.OPD3_BINARY_PAIR -> {
                val importer = binaryPairImporter
                    ?: throw IllegalStateException(
                        "OPD3 binary pair import requires configured media storage."
                    )
                importer.importContent(
                    candidate = pairResolver.resolve(path),
                    progressListener = progressListener,
                    cancellationSignal = cancellationSignal
                )
            }
        }
    }
}
