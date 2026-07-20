package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Chuyển PackageExportManifest thành JSON và ngược lại.
 */
class PackageExportManifestSerializer(
    private val json: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
) {

    fun serialize(
        manifest: PackageExportManifest
    ): String =
        json.encodeToString(
            PackageExportManifestJson.from(
                manifest
            )
        )

    fun deserialize(
        content: String
    ): PackageExportManifest {
        val manifest =
            json.decodeFromString<PackageExportManifestJson>(
                content
            )

        return PackageExportManifest(
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
                    .map(
                        PackageDependencyJson::toDomain
                    )
                    .toSet(),
            hashAlgorithm =
                manifest.hashAlgorithm,
            fileHashes =
                manifest.fileHashes
        )
    }
}