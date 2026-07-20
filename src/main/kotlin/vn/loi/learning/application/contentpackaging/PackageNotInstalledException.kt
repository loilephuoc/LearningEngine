package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId

class PackageNotInstalledException(
    val packageId: PackageId
) : IllegalStateException(
    "Package $packageId is not installed."
)
