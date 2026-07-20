package vn.loi.learning.application.contentpackaging

class MissingPackageContentException(
    contentEntryName: String
) : PackageImportException(
    "Missing package content entry: $contentEntryName"
)
