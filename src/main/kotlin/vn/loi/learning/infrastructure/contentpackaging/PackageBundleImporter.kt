package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.BundlePackageContentImporter
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

/**
 * Adapter JVM nhập package theo bundle format mới.
 *
 * Adapter này không thay thế hoặc sửa importer legacy.
 */
class PackageBundleImporter(
    private val bundleReader: BundlePackageReader,
    private val bundleContentImporter: BundlePackageContentImporter =
        BundlePackageContentImporter()
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate,
        progressListener: ((event: vn.loi.learning.application.contentpackaging.PackageImportProgressEvent) -> Unit)?,
        cancellationSignal: vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal?
    ): ImportedPackageContent =
        bundleContentImporter.importContent(
            bundleReader.read(
                Path.of(candidate.source)
            )
        )
}
