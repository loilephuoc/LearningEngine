package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId

data class InstalledPackageSummary(
    val id: PackageId,
    val name: String,
    val version: String,
    val format: String,
    val libraryCount: Int
)
