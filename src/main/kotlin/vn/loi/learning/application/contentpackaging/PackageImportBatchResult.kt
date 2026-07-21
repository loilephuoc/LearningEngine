package vn.loi.learning.application.contentpackaging

/**
 * Non-fail-fast result for directory imports where each discovered package is
 * reported independently. Successful packages remain committed even when a
 * later candidate is incompatible or malformed.
 */
data class PackageImportBatchResult(
    val successfulImports: List<PackageImportResult> = emptyList(),
    val failures: List<PackageImportFailure> = emptyList()
) {
    val discoveredPackageCount: Int
        get() = successfulImports.size + failures.size

    val hasFailures: Boolean
        get() = failures.isNotEmpty()
}

data class PackageImportFailure(
    val source: String,
    val message: String
)
