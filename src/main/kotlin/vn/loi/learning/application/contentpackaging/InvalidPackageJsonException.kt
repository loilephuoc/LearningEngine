package vn.loi.learning.application.contentpackaging

class InvalidPackageJsonException(
    val entryName: String,
    cause: Throwable
) : PackageImportException(
    cause.message
        ?.takeIf(String::isNotBlank)
        ?: "Invalid package JSON.",
    cause
) {

    init {
        require(entryName.isNotBlank()) {
            "Invalid package JSON entry name must not be blank."
        }
    }
}
