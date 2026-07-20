package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.Serializable
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * DTO JSON biểu diễn metadata cấp package.
 */
@Serializable
data class PackageExportMetadataJson(
    val name: String,
    val version: String,
    val format: String
) {

    companion object {
        fun from(
            descriptor: PackageDescriptor
        ): PackageExportMetadataJson =
            PackageExportMetadataJson(
                name = descriptor.name,
                version = descriptor.version,
                format = descriptor.format
            )
    }
}
