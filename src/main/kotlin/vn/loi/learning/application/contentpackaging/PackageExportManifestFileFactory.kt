package vn.loi.learning.application.contentpackaging

/**
 * Chuyển manifest thành file logic để Infrastructure ghi ra package.
 */
class PackageExportManifestFileFactory(
    private val serializer: PackageExportManifestSerializer = PackageExportManifestSerializer()
) {

    fun create(
        manifest: PackageExportManifest
    ): PackageExportFile =
        PackageExportFile(
            relativePath = "manifest.json",
            content = serializer.serialize(manifest)
        )
}
