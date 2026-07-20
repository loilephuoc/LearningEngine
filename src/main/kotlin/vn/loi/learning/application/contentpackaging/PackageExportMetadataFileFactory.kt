package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * Tạo metadata.json từ PackageDescriptor.
 */
class PackageExportMetadataFileFactory(
    private val serializer: PackageExportMetadataSerializer = PackageExportMetadataSerializer()
) {

    fun create(
        descriptor: PackageDescriptor
    ): PackageExportFile =
        PackageExportFile(
            relativePath = "metadata.json",
            content = serializer.serialize(descriptor)
        )
}
