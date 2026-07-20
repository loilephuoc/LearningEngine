package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId

class InvalidPackageUpgradeException(
    val currentPackageId: PackageId,
    val replacementPackageId: PackageId,
    reason: String
) : IllegalArgumentException(
    "Cannot upgrade package $currentPackageId to $replacementPackageId: $reason."
)