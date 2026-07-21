package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

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

        validateManifest(
            manifestJson
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

    private fun validateManifest(
        manifest: PackageExportManifestJson
    ) {
        require(
            manifest.name.isNotBlank()
        ) {
            "Package manifest name must not be blank."
        }

        require(
            manifest.version.isNotBlank()
        ) {
            "Package manifest version must not be blank."
        }

        require(
            manifest.format.equals(
                EXPECTED_FORMAT,
                ignoreCase = true
            )
        ) {
            "Unsupported package manifest format: ${manifest.format}."
        }

        require(
            manifest.schemaVersion > 0
        ) {
            "Package manifest schema version must be positive."
        }

        require(
            manifest.schemaVersion <=
                    PackageDescriptor.CURRENT_SCHEMA_VERSION
        ) {
            "Unsupported package manifest schema version: ${manifest.schemaVersion}. Current engine schema version is ${PackageDescriptor.CURRENT_SCHEMA_VERSION}."
        }

        require(
            manifest.contentCount >= 0
        ) {
            "Package manifest content count must not be negative."
        }

        require(
            manifest.learningItemCount >= 0
        ) {
            "Package manifest learning item count must not be negative."
        }

        require(
            manifest.minimumEngineVersion == null ||
                    manifest.minimumEngineVersion.isNotBlank()
        ) {
            "Package manifest minimum engine version must be null or non-blank."
        }

        require(
            manifest.maximumEngineVersion == null ||
                    manifest.maximumEngineVersion.isNotBlank()
        ) {
            "Package manifest maximum engine version must be null or non-blank."
        }

        val duplicateDependencies =
            manifest.dependencies
                .groupBy { dependency ->
                    dependency.packageName
                }
                .filterValues { dependencies ->
                    dependencies.size > 1
                }
                .keys

        require(
            duplicateDependencies.isEmpty()
        ) {
            "Package manifest dependencies must have unique package names: ${duplicateDependencies.sorted().joinToString()}."
        }

        require(
            manifest.dependencies.none { dependency ->
                dependency.packageName ==
                        manifest.name
            }
        ) {
            "Package manifest must not depend on itself."
        }
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

    private companion object {

        const val EXPECTED_FORMAT =
            "OPD3"
    }
}