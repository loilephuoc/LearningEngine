package vn.loi.learning.application.contentpackaging

class MissingPackageManifestException(
    manifestEntryName: String
) : MissingRequiredPackageEntryException(
    entryName = manifestEntryName,
    message = "Missing package manifest: $manifestEntryName"
)
