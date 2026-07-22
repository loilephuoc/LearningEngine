package vn.loi.learning.application.contentpackaging

class MissingOpd3JsonPairException(
    packageSource: String
) : PackageImportException(
    "Missing matching JSON for OPD3 media package: $packageSource"
)

class AmbiguousOpd3JsonPairException(
    packageSource: String
) : PackageImportException(
    "Multiple matching JSON files found for OPD3 media package: $packageSource"
)

class UnsupportedPackageTypeException(
    packageSource: String
) : PackageImportException(
    "Unsupported package format: $packageSource"
)

class InvalidOpd3BinaryPackageException(
    message: String,
    cause: Throwable? = null
) : PackageImportException(message, cause)
