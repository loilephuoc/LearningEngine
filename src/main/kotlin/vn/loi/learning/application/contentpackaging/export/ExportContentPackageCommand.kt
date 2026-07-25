package vn.loi.learning.application.contentpackaging.export

import java.nio.file.Path
import vn.loi.learning.domain.library.model.InstalledPackageId

data class ExportContentPackageCommand(
    val installedPackageId: InstalledPackageId,
    val destinationPath: Path,
    val overwrite: Boolean = true,
    val progressListener: PackageExportProgressListener? = null
)
