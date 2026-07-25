package vn.loi.learning.application.contentpackaging.export

enum class ExportProgressStage {
    RESOLVING_PACKAGE,
    COLLECTING_CONTENT,
    COLLECTING_MEDIA,
    WRITING_METADATA,
    WRITING_CONTENT,
    WRITING_MEDIA,
    VALIDATING_PACKAGE,
    FINALIZING,
    COMPLETED
}

fun interface PackageExportProgressListener {
    fun onProgress(stage: ExportProgressStage, message: String, processed: Int, total: Int)
}
