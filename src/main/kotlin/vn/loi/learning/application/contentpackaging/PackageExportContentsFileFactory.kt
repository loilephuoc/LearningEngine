package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.model.Content

/**
 * Tạo contents.json từ danh sách Content.
 */
class PackageExportContentsFileFactory(
    private val serializer: PackageExportContentsSerializer = PackageExportContentsSerializer()
) {

    fun create(
        contents: List<Content>
    ): PackageExportFile =
        PackageExportFile(
            relativePath = "contents.json",
            content = serializer.serialize(contents)
        )
}
