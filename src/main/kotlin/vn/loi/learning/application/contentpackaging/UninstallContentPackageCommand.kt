package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId

data class UninstallContentPackageCommand(
    val catalogId: PackageCatalogId,
    val packageId: PackageId
)
