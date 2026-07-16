package vn.loi.learning.infrastructure.importer.legacy

class LegacyImportException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)