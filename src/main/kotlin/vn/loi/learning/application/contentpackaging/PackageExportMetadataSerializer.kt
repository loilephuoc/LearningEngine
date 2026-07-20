package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * Chuyển PackageDescriptor thành metadata.json.
 */
class PackageExportMetadataSerializer(
    private val json: Json = Json { prettyPrint = true }
) {

    fun serialize(
        descriptor: PackageDescriptor
    ): String =
        json.encodeToString(
            PackageExportMetadataJson.from(descriptor)
        )
}
