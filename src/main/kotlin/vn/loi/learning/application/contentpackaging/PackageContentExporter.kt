package vn.loi.learning.application.contentpackaging

/**
 * Port dùng để xuất dữ liệu Domain thành một content package vật lý.
 */
fun interface PackageContentExporter {

    fun exportContent(
        payload: PackageExportPayload,
        manifest: PackageExportManifest,
        destination: String
    )
}
