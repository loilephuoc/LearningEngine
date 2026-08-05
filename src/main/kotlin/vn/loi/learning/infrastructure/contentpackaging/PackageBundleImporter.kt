package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.BundlePackageContentImporter
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportProgressEvent
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

/**
 * Adapter JVM nhập package theo bundle format mới.
 *
 * Tiêu thụ OPD3 bundle ZIP và giải phóng cả domain data lẫn canonical media assets.
 */
class PackageBundleImporter(
    private val bundleReader: BundlePackageReader,
    private val bundleContentImporter: BundlePackageContentImporter = BundlePackageContentImporter(),
    private val mediaExtractor: Opd3BundleMediaExtractor? = null
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate,
        progressListener: ((event: PackageImportProgressEvent) -> Unit)?,
        cancellationSignal: PackageImportCancellationSignal?
    ): ImportedPackageContent {
        val path = Path.of(candidate.source)
        val bundle = bundleReader.read(path)

        val manifest = runCatching { bundleContentImporter.parseManifest(bundle.manifestJson()) }.getOrNull()
        val defaultName = path.fileName?.toString()?.removeSuffix(".opd3")?.removeSuffix(".zip") ?: candidate.source
        val packageName = manifest?.name?.ifBlank { defaultName } ?: defaultName

        val extracted = mediaExtractor?.extract(
            packagePath = path,
            packageName = packageName,
            manifestHashes = manifest?.fileHashes.orEmpty(),
            progressListener = { processed, total, stage, details ->
                progressListener?.invoke(
                    PackageImportProgressEvent(
                        stage = stage,
                        processed = processed,
                        total = total,
                        message = details
                    )
                )
            },
            cancellationSignal = cancellationSignal
        ).orEmpty()

        val imported = bundleContentImporter.importContent(bundle)
        return imported.copy(
            onRollback = {
                mediaExtractor?.rollback(extracted)
            }
        )
    }
}
