package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.Serializable
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * DTO JSON tương thích ngược với manifest OPD3 cũ.
 */
@Serializable
data class PackageExportManifestJson(
    val name: String,
    val version: String,
    val format: String,
    val contentCount: Int,
    val learningItemCount: Int,
    val schemaVersion: Int =
        PackageDescriptor.CURRENT_SCHEMA_VERSION,
    val minimumEngineVersion: String? =
        null,
    val maximumEngineVersion: String? =
        null,
    val dependencies: List<PackageDependencyJson> =
        emptyList(),
    val hashAlgorithm: String? =
        "SHA-256",
    val fileHashes: Map<String, String> =
        emptyMap(),
    val files: Map<String, String> =
        fileHashes
) {

    companion object {

        fun from(
            manifest: PackageExportManifest
        ): PackageExportManifestJson =
            PackageExportManifestJson(
                name =
                    manifest.name,
                version =
                    manifest.version,
                format =
                    manifest.format,
                contentCount =
                    manifest.contentCount,
                learningItemCount =
                    manifest.learningItemCount,
                schemaVersion =
                    manifest.schemaVersion,
                minimumEngineVersion =
                    manifest.minimumEngineVersion,
                maximumEngineVersion =
                    manifest.maximumEngineVersion,
                dependencies =
                    manifest.dependencies
                        .sortedBy { dependency ->
                            dependency.packageName
                        }
                        .map(
                            PackageDependencyJson::from
                        ),
                hashAlgorithm =
                    manifest.hashAlgorithm,
                fileHashes =
                    manifest.fileHashes,
                files =
                    manifest.fileHashes
            )
    }
}

@Serializable
data class PackageDependencyJson(
    val packageName: String,
    val minimumVersion: String? =
        null,
    val maximumVersion: String? =
        null
) {

    fun toDomain(): PackageDependency =
        PackageDependency(
            packageName =
                packageName,
            minimumVersion =
                minimumVersion,
            maximumVersion =
                maximumVersion
        )

    companion object {

        fun from(
            dependency: PackageDependency
        ): PackageDependencyJson =
            PackageDependencyJson(
                packageName =
                    dependency.packageName,
                minimumVersion =
                    dependency.minimumVersion,
                maximumVersion =
                    dependency.maximumVersion
            )
    }
}