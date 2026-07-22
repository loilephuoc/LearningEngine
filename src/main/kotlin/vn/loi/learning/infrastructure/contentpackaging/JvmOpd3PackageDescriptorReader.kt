package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import kotlinx.serialization.json.Json
import vn.loi.learning.application.contentpackaging.InvalidPackageFormatException
import vn.loi.learning.application.contentpackaging.InvalidPackageVersionException
import vn.loi.learning.application.contentpackaging.MissingPackageManifestException
import vn.loi.learning.application.contentpackaging.PackageDescriptorReader
import vn.loi.learning.application.contentpackaging.PackageExportManifestJson
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * Đọc PackageDescriptor trực tiếp từ manifest của bundle OPD3.
 *
 * Reader dùng chung [PackageExportManifestJson] với import pipeline để tránh
 * duy trì hai schema manifest độc lập.
 */
class JvmOpd3PackageDescriptorReader(
    private val archiveReader: Opd3ArchiveReader,
    private val entryReader: Opd3EntryReader,
    private val archiveStructureValidator:
    Opd3ArchiveStructureValidator =
        Opd3ArchiveStructureValidator(),
    private val json: Json =
        defaultJson(),
    private val manifestEntryName: String =
        MANIFEST_ENTRY_NAME
) : PackageDescriptorReader {

    override fun read(
        candidate: PackageScanCandidate
    ): PackageDescriptor {
        val manifest =
            readManifest(
                candidate
            )

        if (
            !manifest.format.equals(
                SUPPORTED_FORMAT,
                ignoreCase = true
            )
        ) {
            throw InvalidPackageFormatException(
                manifest.format
            )
        }

        if (
            manifest.version.isBlank()
        ) {
            throw InvalidPackageVersionException(
                manifest.version
            )
        }

        require(
            manifest.name.isNotBlank()
        ) {
            "Package manifest name must not be blank."
        }

        require(
            manifest.schemaVersion > 0
        ) {
            "Package manifest schema version must be positive."
        }

        return PackageDescriptor(
            name =
                manifest.name,
            version =
                manifest.version,
            format =
                SUPPORTED_FORMAT,
            schemaVersion =
                manifest.schemaVersion,
            minimumEngineVersion =
                manifest.minimumEngineVersion,
            maximumEngineVersion =
                manifest.maximumEngineVersion,
            dependencies =
                manifest.dependencies
                    .map { dependency ->
                        dependency.toDomain()
                    }
                    .toSet()
        )
    }

    private fun readManifest(
        candidate: PackageScanCandidate
    ): PackageExportManifestJson {
        val packagePath =
            Path.of(
                candidate.source
            )

        return archiveReader
            .open(
                packagePath
            )
            .use { archive ->
                archiveStructureValidator.validate(
                    archive
                )

                val manifestText =
                    entryReader.readText(
                        archive,
                        manifestEntryName
                    ) ?: throw MissingPackageManifestException(
                        manifestEntryName
                    )

                json.decodeFromString<PackageExportManifestJson>(
                    manifestText
                )
            }
    }

    private companion object {

        const val MANIFEST_ENTRY_NAME =
            "manifest.json"

        const val SUPPORTED_FORMAT =
            "OPD3"

        fun defaultJson(): Json =
            Json {
                ignoreUnknownKeys = true
            }
    }
}
