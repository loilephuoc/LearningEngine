package vn.loi.learning.infrastructure.contentpackaging

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.MissingPackageContentException
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class JvmPackageContentImporter(
    private val archiveReader: Opd3ArchiveReader,
    private val entryReader: Opd3EntryReader,
    private val legacyJsonImporter: LegacyJsonImporter = LegacyJsonImporter(),
    private val contentEntryName: String = "content.json"
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate
    ): ImportedPackageContent {
        val packagePath = Path.of(candidate.source)

        val result = archiveReader.open(packagePath).use { archive ->
            val jsonText = entryReader.readText(
                archive,
                contentEntryName
            ) ?: throw MissingPackageContentException(
                contentEntryName
            )

            legacyJsonImporter.import(
                sourceName = candidate.source,
                jsonText = jsonText
            )
        }

        val library = ContentLibrary(
            id = ContentLibraryId(
                "legacy-library-${sha256(candidate.source.trim()).take(ID_HASH_LENGTH)}"
            ),
            descriptor = LibraryDescriptor(
                name = packagePath.fileName.toString()
            ),
            contentIds = result.contents
                .map { it.id }
                .toSet()
        )

        return ImportedPackageContent(
            contents = result.contents,
            learningItems = result.learningItems,
            libraries = listOf(library),
            warnings = result.skippedRecords.map {
                "Skipped legacy record ${it.index}: ${it.reason}"
            }
        )
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))

        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    private companion object {
        const val ID_HASH_LENGTH = 24
    }
}
