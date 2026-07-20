package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId

class DuplicatePackageException(
    packageId: PackageId
) : PackageImportException(
    "Package already imported: $packageId"
)

