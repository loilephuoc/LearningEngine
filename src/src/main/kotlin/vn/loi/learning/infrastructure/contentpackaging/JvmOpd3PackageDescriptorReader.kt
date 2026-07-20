package vn.loi.learning.infrastructure.contentpackaging

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import vn.loi.learning.application.contentpackaging.InvalidPackageFormatException
import vn.loi.learning.application.contentpackaging.InvalidPackageVersionException
import vn.loi.learning.application.contentpackaging.MissingPackageManifestException
import vn.loi.learning.application.contentpackaging.PackageDescriptorReader
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class JvmOpd3PackageDescriptorReader(
    private val archiveReader: Opd3ArchiveReader,
    private val entryReader: Opd3EntryReader,
    private val json: Json = defaultJson(),
    private val manifestEntryName: String = "manifest.json"
 ) : PackageDescriptorReader {

    override fun read(
        candidate: PackageScanCandidate
    ): PackageDescriptor {
        val packagePath = java.nio.file.Path.of(candidate.source)

        val manifest =
            archiveReader.open(packagePath).use { archive ->
                val text = entryReader.readText(
                    archive,
                    manifestEntryName
                ) ?: throw MissingPackageManifestException(
                    manifestEntryName
                )

                json.decodeFromString<Opd3PackageManifestDto>(text)
            }

        if (manifest.format != "OPD3") {
            throw InvalidPackageFormatException(manifest.format)
        }

        if (manifest.version.isBlank()) {
            throw InvalidPackageVersionException(manifest.version)
        }

        return PackageDescriptor(
            name = manifest.name,
            version = manifest.version,
            format = manifest.format
        )
    }

    @Serializable
    private data class Opd3PackageManifestDto(
        val name: String,
        val version: String,
        val format: String
    )

    private companion object {
        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
        }
    }
}


