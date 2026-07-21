package vn.loi.learning.application.contentlibrary

import vn.loi.learning.domain.content.packaging.model.PackageId

class ContentPackageNotFoundException(
    val packageId: PackageId
) : IllegalStateException(
    "Content package not found: $packageId"
)