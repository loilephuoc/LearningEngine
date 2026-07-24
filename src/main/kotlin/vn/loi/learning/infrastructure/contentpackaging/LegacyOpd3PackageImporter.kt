package vn.loi.learning.infrastructure.contentpackaging

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import vn.loi.learning.adapter.jvm.JvmJsonFileReader
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.LegacyPackageCandidate
import vn.loi.learning.application.contentpackaging.LegacyPackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
import vn.loi.learning.application.contentpackaging.PackageImportDiagnostics
import vn.loi.learning.application.contentpackaging.PackageImportProgressEvent
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.infrastructure.contentmedia.ImportedMediaVerifier
import vn.loi.learning.infrastructure.contentmedia.LegacyContentMediaPathMapper
import vn.loi.learning.infrastructure.contentmedia.PackageMediaExtractor
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class LegacyOpd3PackageImporter(
    private val jsonImporter: LegacyJsonImporter,
    private val mediaExtractor: PackageMediaExtractor,
    private val mediaPathMapper: LegacyContentMediaPathMapper,
    private val mediaVerifier: ImportedMediaVerifier,
    private val jsonFileReader: JvmJsonFileReader =
        JvmJsonFileReader()
) : LegacyPackageContentImporter {

    override fun importContent(
        candidate: LegacyPackageCandidate,
        progressListener: ((event: PackageImportProgressEvent) -> Unit)?,
        cancellationSignal: PackageImportCancellationSignal?
    ): ImportedPackageContent {
        val jsonPath = Paths.get(candidate.jsonSource)
        val mediaPath = Paths.get(candidate.mediaSource)
        val packageName = derivePackageName(jsonPath)

        PackageImportDiagnostics.logResolvedPair(jsonPath, mediaPath)
        cancellationSignal?.checkCancelled()

        progressListener?.invoke(
            PackageImportProgressEvent(
                stage = PackageImportProgressStage.READING_METADATA,
                message = "Reading metadata: ${jsonPath.fileName}"
            )
        )

        val jsonText = jsonFileReader.read(jsonPath)
        cancellationSignal?.checkCancelled()

        val legacyResult = jsonImporter.import(
            sourceName = jsonPath.fileName.toString(),
            jsonText = jsonText
        )

        progressListener?.invoke(
            PackageImportProgressEvent(
                stage = PackageImportProgressStage.IMPORTING_CONTENT,
                processed = legacyResult.contents.size,
                total = legacyResult.contents.size,
                message = "Parsed ${legacyResult.contents.size} content items"
            )
        )
        cancellationSignal?.checkCancelled()

        val extractedAssets = mediaExtractor.extract(
            packageFile = mediaPath,
            packageName = packageName,
            progressListener = { processed, total, stage, details ->
                progressListener?.invoke(
                    PackageImportProgressEvent(
                        stage = stage,
                        processed = processed,
                        total = total,
                        message = details
                    )
                )
            },
            cancellationSignal = cancellationSignal
        )

        cancellationSignal?.checkCancelled()

        val mediaMappingResult = mediaPathMapper.mapWithWarnings(
            contents = legacyResult.contents,
            assets = extractedAssets
        )

        progressListener?.invoke(
            PackageImportProgressEvent(
                stage = PackageImportProgressStage.VALIDATING_MEDIA,
                message = "Validating media references..."
            )
        )
        cancellationSignal?.checkCancelled()

        mediaVerifier.verify(
            contents = mediaMappingResult.contents,
            cancellationSignal = cancellationSignal
        )

        val warnings = buildList {
            if (legacyResult.skippedRecordCount > 0) {
                add("Skipped ${legacyResult.skippedRecordCount} legacy record(s).")
            }
            if (legacyResult.duplicateCount > 0) {
                add("Detected ${legacyResult.duplicateCount} duplicate legacy record(s).")
            }
            addAll(mediaMappingResult.warnings)
        }

        return ImportedPackageContent(
            contents = mediaMappingResult.contents,
            learningItems = legacyResult.learningItems,
            libraries = listOf(
                ContentLibrary(
                    id = ContentLibraryId(createLibraryId(candidate.jsonSource)),
                    descriptor = LibraryDescriptor(name = packageName),
                    contentIds = mediaMappingResult.contents.map { it.id }.toSet()
                )
            ),
            warnings = warnings
        )
    }

    fun import(
        candidate: LegacyPackageCandidate
    ): ImportedPackageContent = importContent(candidate, null, null)

    private fun derivePackageName(
        jsonPath: Path
    ): String {
        val fileName = jsonPath.fileName.toString()
        val extensionSeparatorIndex = fileName.lastIndexOf('.')
        require(extensionSeparatorIndex > 0) {
            "Legacy JSON source must have a file extension: $jsonPath"
        }
        return fileName.substring(0, extensionSeparatorIndex)
    }

    private fun createLibraryId(source: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(source.trim().toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(ID_HASH_LENGTH)
        return "legacy-library-$hash"
    }

    private companion object {
        const val ID_HASH_LENGTH = 24
    }
}
