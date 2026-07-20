package vn.loi.learning.infrastructure.contentmedia

import java.nio.file.Path
import vn.loi.learning.application.contentmedia.ContentMediaAsset

fun interface PackageMediaExtractor {

    fun extract(
        packageFile: Path,
        packageName: String
    ): List<ContentMediaAsset>
}