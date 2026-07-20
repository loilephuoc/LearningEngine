package vn.loi.learning.application.contentpackaging

fun interface PackageImportProgressListener {
    fun onProgress(
        event: PackageImportProgressEvent
    )
}
