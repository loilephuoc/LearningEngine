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
import vn.loi.learning.infrastructure.contentmedia.ImportedMediaVerifier
import vn.loi.learning.infrastructure.contentmedia.LegacyContentMediaPathMapper
import vn.loi.learning.infrastructure.contentmedia.PackageMediaExtractor
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class JvmPackageContentImporter(
    private val archiveReader: Opd3ArchiveReader,
    private val entryReader: Opd3EntryReader,
    private val legacyJsonImporter: LegacyJsonImporter = LegacyJsonImporter(),
    private val contentEntryName: String = "content.json",
    private val mediaExtractor: PackageMediaExtractor? = null,
    private val mediaPathMapper: LegacyContentMediaPathMapper =
        LegacyContentMediaPathMapper(),
    private val importedMediaVerifier: ImportedMediaVerifier? = null
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate
    ): ImportedPackageContent {
        val packagePath =
            Path.of(candidate.source)

        val importedResult =
            archiveReader.open(packagePath).use { archive ->
                val jsonText =
                    entryReader.readText(
                        archive = archive,
                        entryName = contentEntryName
                    ) ?: throw MissingPackageContentException(
                        contentEntryName
                    )

                legacyJsonImporter.import(
                    sourceName = candidate.source,
                    jsonText = jsonText
                )
            }

        val packageStorageName =
            createPackageStorageName(
                candidate.source
            )

        val mediaAssets =
            mediaExtractor?.extract(
                packageFile = packagePath,
                packageName = packageStorageName
            ).orEmpty()

        val mappedContents =
            mediaPathMapper.map(
                contents = importedResult.contents,
                assets = mediaAssets
            )

        importedMediaVerifier?.verify(
            mappedContents
        )

        val library =
            ContentLibrary(
                id =
                    ContentLibraryId(
                        packageStorageName
                    ),
                descriptor =
                    LibraryDescriptor(
                        name =
                            packagePath
                                .fileName
                                .toString()
                    ),
                contentIds =
                    mappedContents
                        .map { content ->
                            content.id
                        }
                        .toSet()
            )

        return ImportedPackageContent(
            contents = mappedContents,
            learningItems =
                importedResult.learningItems,
            libraries =
                listOf(library),
            warnings =
                importedResult.skippedRecords.map { skippedRecord ->
                    "Skipped legacy record ${skippedRecord.index}: ${skippedRecord.reason}"
                }
        )
    }

    private fun createPackageStorageName(
        source: String
    ): String =
        "legacy-library-${
            sha256(
                source.trim()
            ).take(ID_HASH_LENGTH)
        }"

    private fun sha256(
        value: String
    ): String {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    value.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

        return digest.joinToString(
            separator = ""
        ) { byte ->
            "%02x".format(byte)
        }
    }

    private companion object {

        const val ID_HASH_LENGTH =
            24
    }
}