package vn.loi.learning.application.contentpackaging

class InvalidPackageFormatException(
    actualFormat: String,
) : PackageImportException(
    "Unsupported package format: $actualFormat"
)
