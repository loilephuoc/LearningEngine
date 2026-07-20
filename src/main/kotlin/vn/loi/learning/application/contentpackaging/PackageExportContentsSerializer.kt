package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.model.Content

/**
 * Chuyển danh sách Content thành contents.json.
 */
class PackageExportContentsSerializer(
    private val json: Json = Json { prettyPrint = true }
) {

    fun serialize(
        contents: List<Content>
    ): String =
        json.encodeToString(
            PackageExportContentsJson.from(contents)
        )
}
