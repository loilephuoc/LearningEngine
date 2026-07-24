package vn.loi.learning.application.contentpackaging

data class PackageImportProgressEvent(
    val stage: PackageImportProgressStage,
    val processed: Int = 0,
    val total: Int = 0,
    val message: String? = null
)
