package vn.loi.learning.application.contentpackaging

class InvalidPackageArchiveStructureException(
    detail: String
) : PackageImportException(
    "Invalid OPD3 archive structure: $detail"
)
