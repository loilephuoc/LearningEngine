package vn.loi.learning.application.importing

class LegacyImportException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
