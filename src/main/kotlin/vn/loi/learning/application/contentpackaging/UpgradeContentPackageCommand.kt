package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId

data class UpgradeContentPackageCommand(
    val catalogId: PackageCatalogId,
    val currentPackageId: PackageId,
    val replacementPackage: ContentPackage
)