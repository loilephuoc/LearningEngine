package vn.loi.learning.application.contentpackaging

class InvalidPackageVersionException(
    actualVersion: String
) : PackageImportException(
    "Invalid package version: $actualVersion"
)

