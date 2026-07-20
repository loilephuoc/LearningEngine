package vn.loi.learning.application.contentpackaging

/**
 * Exception được ném khi quá trình export package thất bại.
 */
class PackageExportException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
