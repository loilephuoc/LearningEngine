package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Chuyển bundle package chuẩn mới thành dữ liệu Domain chưa được lưu.
 *
 * Class này không thay thế importer legacy và không truy cập repository.
 */
class BundlePackageContentImporter(
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
        },
    private val integrityVerifier:
    PackageIntegrityVerifier =
        PackageIntegrityVerifier()
) {

    fun importContent(
        bundle: PackageImportBundle
    ): ImportedPackageContent {
        val manifestJson =
            json.decodeFromString<PackageExportManifestJson>(
                bundle.manifestJson()
            )

        validateIntegrityMetadata(
            manifestJson
        )

        integrityVerifier.verify(
            bundle =
                bundle,
            manifest =
                manifestJson
        )

        val contentsJson =
            json.decodeFromString<PackageExportContentsJson>(
                bundle.contentsJson()
            )

        val learningItemsJson =
            json.decodeFromString<PackageExportLearningItemsJson>(
                bundle.learningItemsJson()
            )

        require(
            manifestJson.contentCount ==
                    contentsJson.contents.size
        ) {
            "Manifest content count ${manifestJson.contentCount} does not match imported content count ${contentsJson.contents.size}."
        }

        require(
            manifestJson.learningItemCount ==
                    learningItemsJson.learningItems.size
        ) {
            "Manifest learning item count ${manifestJson.learningItemCount} does not match imported learning item count ${learningItemsJson.learningItems.size}."
        }

        return ImportedPackageContent(
            contents =
                contentsJson.contents.map(
                    PackageExportContentJson::toDomain
                ),
            learningItems =
                learningItemsJson.learningItems.map(
                    PackageExportLearningItemJson::toDomain
                )
        )
    }

    private fun validateIntegrityMetadata(
        manifest: PackageExportManifestJson
    ) {
        if (
            manifest.fileHashes.isEmpty()
        ) {
            return
        }

        require(
            manifest.hashAlgorithm ==
                    PackageExportManifestFactory.HASH_ALGORITHM
        ) {
            "Unsupported package integrity hash algorithm: ${manifest.hashAlgorithm}."
        }

        val missingHashes =
            PackageImportBundle.REQUIRED_FILES
                .filterNot { relativePath ->
                    relativePath ==
                            PackageImportBundle.MANIFEST_FILE
                }
                .filterNot(
                    manifest.fileHashes::containsKey
                )

        require(
            missingHashes.isEmpty()
        ) {
            "Missing integrity hashes for package files: ${missingHashes.joinToString()}."
        }

        require(
            PackageImportBundle.MANIFEST_FILE !in
                    manifest.fileHashes
        ) {
            "Manifest must not contain an integrity hash for itself."
        }
    }
}