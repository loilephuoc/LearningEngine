package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

/**
 * Command đăng ký một ContentPackage vào PackageCatalog.
 */
data class RegisterContentPackageCommand(
    val catalogId: PackageCatalogId,
    val contentPackage: ContentPackage
)
