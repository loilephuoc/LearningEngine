package vn.loi.learning.application.contentpackaging

class MissingPackageContentException(
    contentEntryName: String
) : MissingRequiredPackageEntryException(
    entryName = contentEntryName,
    message = "Missing package content entry: $contentEntryName"
)
