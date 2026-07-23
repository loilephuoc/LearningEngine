package vn.loi.learning.application.contentpackaging

import java.nio.file.Paths
import java.util.Locale
import vn.loi.learning.adapter.jvm.JvmJsonFileReader
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.infrastructure.contentpackaging.JvmLegacyPkgMediaScanner
import vn.loi.learning.infrastructure.importer.legacy.LegacyImportException
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

/**
 * Service ứng dụng chuyển đổi một ValidatedLegacyTopicPair hợp lệ sang
 * CanonicalTopicPackage độc lập nền tảng và chuẩn hóa định danh.
 */
class LegacyPairCanonicalConverter(
    private val jsonImporter: LegacyJsonImporter = LegacyJsonImporter(),
    private val jsonSourceReader: LegacyJsonSourceReader = LegacyJsonSourceReader { path ->
        JvmJsonFileReader().read(Paths.get(path))
    },
    private val pkgMediaScanner: LegacyPkgMediaScanner = JvmLegacyPkgMediaScanner()
) {

    fun convert(
        pair: ValidatedLegacyTopicPair
    ): LegacyPairCanonicalConversionResult {
        val diagnostics = mutableListOf<CanonicalConversionDiagnostic>()

        val jsonText = try {
            jsonSourceReader.readJsonText(pair.jsonSource)
        } catch (exception: Exception) {
            diagnostics += CanonicalConversionDiagnostic(
                code = CanonicalConversionDiagnosticCode.MALFORMED_LEGACY_JSON,
                severity = CanonicalConversionDiagnosticSeverity.FATAL,
                message = "Cannot read legacy JSON source '${pair.jsonSource}': ${exception.message ?: "read error"}",
                source = pair.jsonSource
            )
            return LegacyPairCanonicalConversionResult(
                canonicalPackage = null,
                diagnostics = diagnostics.sortedWith(diagnosticComparator),
                isReadyForExport = false
            )
        }

        val legacyResult = try {
            jsonImporter.import(
                sourceName = pair.logicalTopicName + ".json",
                jsonText = jsonText
            )
        } catch (exception: LegacyImportException) {
            diagnostics += CanonicalConversionDiagnostic(
                code = CanonicalConversionDiagnosticCode.MALFORMED_LEGACY_JSON,
                severity = CanonicalConversionDiagnosticSeverity.FATAL,
                message = exception.message ?: "Failed to parse legacy JSON source.",
                source = pair.jsonSource
            )
            return LegacyPairCanonicalConversionResult(
                canonicalPackage = null,
                diagnostics = diagnostics.sortedWith(diagnosticComparator),
                isReadyForExport = false
            )
        } catch (exception: Exception) {
            diagnostics += CanonicalConversionDiagnostic(
                code = CanonicalConversionDiagnosticCode.MALFORMED_LEGACY_JSON,
                severity = CanonicalConversionDiagnosticSeverity.FATAL,
                message = "Invalid legacy JSON source '${pair.jsonSource}': ${exception.message ?: "parse error"}",
                source = pair.jsonSource
            )
            return LegacyPairCanonicalConversionResult(
                canonicalPackage = null,
                diagnostics = diagnostics.sortedWith(diagnosticComparator),
                isReadyForExport = false
            )
        }

        // Validate skipped records (truong bat buộc bị thiếu)
        if (legacyResult.skippedRecordCount > 0) {
            legacyResult.skippedRecords.forEach { skipped ->
                diagnostics += CanonicalConversionDiagnostic(
                    code = CanonicalConversionDiagnosticCode.INVALID_REQUIRED_FIELD,
                    severity = CanonicalConversionDiagnosticSeverity.FATAL,
                    message = "Skipped legacy record at index ${skipped.index}: ${skipped.reason}",
                    source = pair.jsonSource
                )
            }
        }

        // Validate duplicates trong legacy JSON
        if (legacyResult.duplicateCount > 0) {
            diagnostics += CanonicalConversionDiagnostic(
                code = CanonicalConversionDiagnosticCode.DUPLICATE_CONTENT_IDENTITY,
                severity = CanonicalConversionDiagnosticSeverity.FATAL,
                message = "Detected ${legacyResult.duplicateCount} duplicate legacy record(s) in JSON source.",
                source = pair.jsonSource
            )
        }

        // Validate ContentId trùng lặp
        val contentIds = legacyResult.contents.map { it.id }
        val duplicateContentIds = contentIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateContentIds.isNotEmpty()) {
            duplicateContentIds.forEach { dupId ->
                diagnostics += CanonicalConversionDiagnostic(
                    code = CanonicalConversionDiagnosticCode.DUPLICATE_CONTENT_IDENTITY,
                    severity = CanonicalConversionDiagnosticSeverity.FATAL,
                    message = "Duplicate content identity detected: ${dupId.value}",
                    contentId = dupId,
                    source = pair.jsonSource
                )
            }
        }

        // Validate LearningItemId trùng lặp
        val itemIds = legacyResult.learningItems.map { it.id }
        val duplicateItemIds = itemIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateItemIds.isNotEmpty()) {
            duplicateItemIds.forEach { dupItemId ->
                diagnostics += CanonicalConversionDiagnostic(
                    code = CanonicalConversionDiagnosticCode.DUPLICATE_LEARNING_ITEM_IDENTITY,
                    severity = CanonicalConversionDiagnosticSeverity.FATAL,
                    message = "Duplicate learning item identity detected: ${dupItemId.value}",
                    source = pair.jsonSource
                )
            }
        }

        // Validate content-to-item relationship
        val validContentIdSet = contentIds.toSet()
        legacyResult.learningItems.forEach { item ->
            if (item.contentId !in validContentIdSet) {
                diagnostics += CanonicalConversionDiagnostic(
                    code = CanonicalConversionDiagnosticCode.UNRESOLVED_CONTENT_ITEM_RELATIONSHIP,
                    severity = CanonicalConversionDiagnosticSeverity.FATAL,
                    message = "Learning item '${item.id.value}' references missing content ID '${item.contentId.value}'.",
                    contentId = item.contentId,
                    source = pair.jsonSource
                )
            }
        }

        // Scan PKG entry filenames
        val pkgEntries = pkgMediaScanner.scanMediaEntries(pair.packageSource)
        val normalizedPkgEntries = pkgEntries.map { normalizePath(it) }.toSet()

        // Extract media references
        val mediaReferences = mutableListOf<CanonicalMediaReference>()
        legacyResult.contents.forEach { content ->
            extractMediaReferences(
                content = content,
                pkgEntries = normalizedPkgEntries,
                mediaReferences = mediaReferences,
                diagnostics = diagnostics
            )
        }

        // Collect tags
        val tags = legacyResult.contents.flatMap { it.metadata.tags }.toSet()

        // Deterministic sorting
        val sortedContents = legacyResult.contents.sortedWith(contentComparator)
        val sortedLearningItems = legacyResult.learningItems.sortedWith(learningItemComparator)
        val sortedMediaReferences = mediaReferences.sortedWith(mediaReferenceComparator)
        val sortedDiagnostics = diagnostics.sortedWith(diagnosticComparator)

        val hasFatal = sortedDiagnostics.any { it.severity == CanonicalConversionDiagnosticSeverity.FATAL }
        val isReadyForExport = !hasFatal

        val canonicalPackage = CanonicalTopicPackage(
            topicId = pair.topicId,
            logicalTopicName = pair.logicalTopicName,
            sourceMetadata = LegacyTopicSourceMetadata(
                logicalTopicName = pair.logicalTopicName,
                jsonSource = pair.jsonSource,
                packageSource = pair.packageSource
            ),
            contents = sortedContents,
            learningItems = sortedLearningItems,
            mediaReferences = sortedMediaReferences,
            tags = tags
        )

        return LegacyPairCanonicalConversionResult(
            canonicalPackage = canonicalPackage,
            diagnostics = sortedDiagnostics,
            isReadyForExport = isReadyForExport
        )
    }

    private fun extractMediaReferences(
        content: Content,
        pkgEntries: Set<String>,
        mediaReferences: MutableList<CanonicalMediaReference>,
        diagnostics: MutableList<CanonicalConversionDiagnostic>
    ) {
        val media = content.media

        checkAndAddMediaRef(
            contentId = content.id,
            rawRef = media.primaryAudio,
            mediaType = CanonicalMediaType.AUDIO,
            mediaTypeName = "primary audio",
            pkgEntries = pkgEntries,
            mediaReferences = mediaReferences,
            diagnostics = diagnostics
        )

        checkAndAddMediaRef(
            contentId = content.id,
            rawRef = media.translatedAudio,
            mediaType = CanonicalMediaType.AUDIO,
            mediaTypeName = "translated audio",
            pkgEntries = pkgEntries,
            mediaReferences = mediaReferences,
            diagnostics = diagnostics
        )

        checkAndAddMediaRef(
            contentId = content.id,
            rawRef = media.image,
            mediaType = CanonicalMediaType.IMAGE,
            mediaTypeName = "image",
            pkgEntries = pkgEntries,
            mediaReferences = mediaReferences,
            diagnostics = diagnostics
        )

        checkAndAddMediaRef(
            contentId = content.id,
            rawRef = media.exampleAudio,
            mediaType = CanonicalMediaType.AUDIO,
            mediaTypeName = "example audio",
            pkgEntries = pkgEntries,
            mediaReferences = mediaReferences,
            diagnostics = diagnostics
        )

        checkAndAddMediaRef(
            contentId = content.id,
            rawRef = media.exampleTranslatedAudio,
            mediaType = CanonicalMediaType.AUDIO,
            mediaTypeName = "example translated audio",
            pkgEntries = pkgEntries,
            mediaReferences = mediaReferences,
            diagnostics = diagnostics
        )
    }

    private fun checkAndAddMediaRef(
        contentId: ContentId,
        rawRef: String?,
        mediaType: CanonicalMediaType,
        mediaTypeName: String,
        pkgEntries: Set<String>,
        mediaReferences: MutableList<CanonicalMediaReference>,
        diagnostics: MutableList<CanonicalConversionDiagnostic>
    ) {
        if (rawRef.isNullOrBlank()) return

        val normalized = normalizePath(rawRef)
        val isPresent = normalized in pkgEntries
        val status = if (isPresent) CanonicalMediaStatus.PRESENT else CanonicalMediaStatus.MISSING

        mediaReferences += CanonicalMediaReference(
            referencedAsset = rawRef,
            logicalPath = normalized,
            mediaType = mediaType,
            owningContentId = contentId,
            owningLearningItemId = null,
            status = status
        )

        if (!isPresent) {
            diagnostics += CanonicalConversionDiagnostic(
                code = CanonicalConversionDiagnosticCode.UNRESOLVED_MEDIA_REFERENCE,
                severity = CanonicalConversionDiagnosticSeverity.WARNING,
                message = "Missing $mediaTypeName for content ${contentId.value}: $rawRef",
                contentId = contentId
            )
        }
    }

    private fun normalizePath(path: String): String =
        path.trim()
            .replace('\\', '/')
            .removePrefix("./")

    private companion object {

        val contentComparator: Comparator<Content> =
            compareBy { it.id.value }

        val learningItemComparator: Comparator<LearningItem> =
            compareBy<LearningItem> { it.contentId.value }
                .thenBy { it.mode.name }
                .thenBy { it.id.value }

        val mediaReferenceComparator: Comparator<CanonicalMediaReference> =
            compareBy<CanonicalMediaReference> { it.owningContentId.value }
                .thenBy { it.mediaType.name }
                .thenBy { it.logicalPath }

        val diagnosticComparator: Comparator<CanonicalConversionDiagnostic> =
            compareBy<CanonicalConversionDiagnostic> {
                if (it.severity == CanonicalConversionDiagnosticSeverity.FATAL) 0 else 1
            }
                .thenBy { it.code.name }
                .thenBy { it.contentId?.value.orEmpty() }
                .thenBy { it.message.lowercase(Locale.ROOT) }
    }
}
