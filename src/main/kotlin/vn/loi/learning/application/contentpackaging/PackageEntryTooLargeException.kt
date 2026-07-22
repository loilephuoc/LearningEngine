package vn.loi.learning.application.contentpackaging

class PackageEntryTooLargeException(
    entryName: String,
    maximumBytes: Long
) : PackageImportException(
    "Package entry '$entryName' exceeds the maximum allowed size of $maximumBytes bytes."
)
