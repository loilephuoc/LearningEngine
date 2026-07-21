package vn.loi.learning.application.contentpackaging

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
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

        validateMetadata(
            metadataJson = bundle.metadataJson(),
            manifest = manifestJson
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

        val importedContents =
            contentsJson.contents.map(
                PackageExportContentJson::toDomain
            )

        val library =
            ContentLibrary(
                id =
                    ContentLibraryId(
                        createLibraryId(
                            manifestJson
                        )
                    ),
                descriptor =
                    LibraryDescriptor(
                        name = manifestJson.name
                    ),
                contentIds =
                    importedContents
                        .map { content ->
                            content.id
                        }
                        .toSet()
            )

        return ImportedPackageContent(
            contents = importedContents,
            learningItems =
                learningItemsJson.learningItems.map(
                    PackageExportLearningItemJson::toDomain
                ),
            libraries = listOf(library)
        )
    }

    private fun createLibraryId(
        manifest: PackageExportManifestJson
    ): String {
        val identity =
            listOf(
                manifest.name.trim(),
                manifest.version.trim(),
                manifest.format.uppercase()
            ).joinToString(
                separator = "\u0000"
            )

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    identity.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

        val hash =
            digest.joinToString(
                separator = ""
            ) { byte ->
                "%02x".format(byte)
            }

        return "opd3-library-${hash.take(LIBRARY_ID_HASH_LENGTH)}"
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

    private fun validateMetadata(
        metadataJson: String,
        manifest: PackageExportManifestJson
    ) {
        val metadata =
            json.parseToJsonElement(
                metadataJson
            ).jsonObject

        metadata.optionalString(
            fieldName = "name"
        )?.let { name ->
            require(
                name.isNotBlank()
            ) {
                "Package metadata name must not be blank."
            }

            require(
                name == manifest.name
            ) {
                "Package metadata name '$name' does not match manifest name '${manifest.name}'."
            }
        }

        metadata.optionalString(
            fieldName = "version"
        )?.let { version ->
            require(
                version.isNotBlank()
            ) {
                "Package metadata version must not be blank."
            }

            require(
                version == manifest.version
            ) {
                "Package metadata version '$version' does not match manifest version '${manifest.version}'."
            }
        }

        metadata.optionalString(
            fieldName = "format"
        )?.let { format ->
            require(
                format.equals(
                    EXPECTED_FORMAT,
                    ignoreCase = true
                )
            ) {
                "Unsupported package metadata format: $format."
            }

            require(
                format.equals(
                    manifest.format,
                    ignoreCase = true
                )
            ) {
                "Package metadata format '$format' does not match manifest format '${manifest.format}'."
            }
        }
    }

    private fun Map<String, kotlinx.serialization.json.JsonElement>.optionalString(
        fieldName: String
    ): String? =
        get(
            fieldName
        )?.jsonPrimitive?.contentOrNull

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

        const val LIBRARY_ID_HASH_LENGTH =
            24
    }
}