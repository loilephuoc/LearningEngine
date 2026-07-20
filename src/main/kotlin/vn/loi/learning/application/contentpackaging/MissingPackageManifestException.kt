package vn.loi.learning.application.contentpackaging

class MissingPackageManifestException(
    manifestEntryName: String
) : PackageImportException(
    "Missing package manifest: $manifestEntryName"
)
