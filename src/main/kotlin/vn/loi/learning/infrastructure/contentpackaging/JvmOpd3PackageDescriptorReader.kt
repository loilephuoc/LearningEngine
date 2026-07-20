package vn.loi.learning.infrastructure.contentpackaging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import vn.loi.learning.application.contentpackaging.InvalidPackageFormatException
import vn.loi.learning.application.contentpackaging.InvalidPackageVersionException
import vn.loi.learning.application.contentpackaging.MissingPackageManifestException
import vn.loi.learning.application.contentpackaging.PackageDescriptorReader
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class JvmOpd3PackageDescriptorReader(
    private val archiveReader: Opd3ArchiveReader,
    private val entryReader: Opd3EntryReader,
    private val json: Json =
        defaultJson(),
    private val manifestEntryName: String =
        "manifest.json"
) : PackageDescriptorReader {

    override fun read(
        candidate: PackageScanCandidate
    ): PackageDescriptor {
        val packagePath =
            java.nio.file.Path.of(
                candidate.source
            )

        val manifest =
            archiveReader.open(packagePath)
                .use { archive ->
                    val text =
                        entryReader.readText(
                            archive,
                            manifestEntryName
                        ) ?: throw MissingPackageManifestException(
                            manifestEntryName
                        )

                    json.decodeFromString<Opd3PackageManifestDto>(
                        text
                    )
                }

        if (manifest.format != "OPD3") {
            throw InvalidPackageFormatException(
                manifest.format
            )
        }

        if (manifest.version.isBlank()) {
            throw InvalidPackageVersionException(
                manifest.version
            )
        }

        return PackageDescriptor(
            name =
                manifest.name,
            version =
                manifest.version,
            format =
                manifest.format,
            schemaVersion =
                manifest.schemaVersion,
            minimumEngineVersion =
                manifest.minimumEngineVersion,
            maximumEngineVersion =
                manifest.maximumEngineVersion,
            dependencies =
                manifest.dependencies
                    .map(
                        Opd3PackageDependencyDto::toDomain
                    )
                    .toSet()
        )
    }

    @Serializable
    private data class Opd3PackageManifestDto(
        val name: String,
        val version: String,
        val format: String,
        val schemaVersion: Int =
            PackageDescriptor.CURRENT_SCHEMA_VERSION,
        val minimumEngineVersion: String? =
            null,
        val maximumEngineVersion: String? =
            null,
        val dependencies: List<Opd3PackageDependencyDto> =
            emptyList()
    )

    @Serializable
    private data class Opd3PackageDependencyDto(
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
    }

    private companion object {

        fun defaultJson(): Json =
            Json {
                ignoreUnknownKeys = true
            }
    }
}