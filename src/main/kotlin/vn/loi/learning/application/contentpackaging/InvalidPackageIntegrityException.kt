package vn.loi.learning.application.contentpackaging

class InvalidPackageIntegrityException(
    val relativePath: String,
    val expectedHash: String,
    val actualHash: String
) : PackageImportException(
    "Package integrity verification failed for $relativePath. Expected $expectedHash but was $actualHash."
)