package vn.loi.learning.infrastructure.contentmedia

import java.nio.file.Path
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage

fun interface PackageMediaExtractor {

    fun extract(
        packageFile: Path,
        packageName: String,
        progressListener: ((processed: Int, total: Int, stage: PackageImportProgressStage, details: String?) -> Unit)?,
        cancellationSignal: PackageImportCancellationSignal?
    ): List<ContentMediaAsset>

    fun extract(
        packageFile: Path,
        packageName: String
    ): List<ContentMediaAsset> = extract(packageFile, packageName, null, null)

    companion object {
        operator fun invoke(block: (Path, String) -> List<ContentMediaAsset>): PackageMediaExtractor =
            PackageMediaExtractor { file, name, _, _ -> block(file, name) }
    }
}