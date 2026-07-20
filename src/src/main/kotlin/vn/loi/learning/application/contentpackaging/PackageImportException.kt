package vn.loi.learning.application.contentpackaging

open class PackageImportException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
