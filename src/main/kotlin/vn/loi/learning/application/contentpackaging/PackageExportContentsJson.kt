package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.Serializable
import vn.loi.learning.domain.content.model.Content

/**
 * DTO JSON cho danh sách Content trong package export.
 */
@Serializable
data class PackageExportContentsJson(
    val contents: List<PackageExportContentJson>
) {

    companion object {
        fun from(
            contents: List<Content>
        ): PackageExportContentsJson =
            PackageExportContentsJson(
                contents = contents.map(PackageExportContentJson::from)
            )
    }
}
