package vn.loi.learning.application.contentpackaging

class InvalidPackageTextEncodingException(
    entryName: String,
    cause: Throwable? = null
) : PackageImportException(
    "Package entry '$entryName' is not valid UTF-8 text.",
    cause
)
