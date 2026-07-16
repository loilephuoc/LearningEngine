package vn.loi.learning.infrastructure.importer.legacy

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

/**
 * Chuyển JSON từ ứng dụng Android cũ sang Domain Model mới.
 *
 * Importer:
 * - biết format JSON cũ;
 * - không lưu database;
 * - không phụ thuộc Android;
 * - không sửa Domain;
 * - tạo ID ổn định dựa trên nội dung nguồn.
 */
class LegacyJsonImporter(
    private val json: Json = defaultJson()
) {

    fun import(
        sourceName: String,
        jsonText: String
    ): LegacyImportResult {
        require(sourceName.isNotBlank()) {
            "Source name must not be blank."
        }

        require(jsonText.isNotBlank()) {
            "JSON text must not be blank."
        }

        val records = try {
            json.decodeFromString<List<LegacySentenceJsonDto>>(
                jsonText
            )
        } catch (exception: SerializationException) {
            throw LegacyImportException(
                message = "Cannot parse legacy JSON source '$sourceName'.",
                cause = exception
            )
        } catch (exception: IllegalArgumentException) {
            throw LegacyImportException(
                message = "Invalid legacy JSON source '$sourceName'.",
                cause = exception
            )
        }

        val contents = mutableListOf<Content>()
        val learningItems = mutableListOf<LearningItem>()
        val skippedRecords = mutableListOf<SkippedLegacyRecord>()

        records.forEachIndexed { index, record ->
            val result = convertRecord(
                sourceName = sourceName,
                index = index,
                record = record
            )

            when (result) {
                is ConversionResult.Success -> {
                    contents += result.content
                    learningItems += result.learningItems
                }

                is ConversionResult.Skipped -> {
                    skippedRecords += SkippedLegacyRecord(
                        index = index,
                        reason = result.reason
                    )
                }
            }
        }

        return LegacyImportResult(
            contents = contents,
            learningItems = learningItems,
            skippedRecords = skippedRecords
        )
    }

    private fun convertRecord(
        sourceName: String,
        index: Int,
        record: LegacySentenceJsonDto
    ): ConversionResult {
        val primaryText = record.en.cleanOrNull()
            ?: return ConversionResult.Skipped(
                reason = "Field 'en' is missing or blank."
            )

        val normalizedSource = sourceName.trim()

        val identitySeed = buildString {
            append(normalizedSource)
            append('|')
            append(record.group.cleanOrNull().orEmpty())
            append('|')
            append(record.section.cleanOrNull().orEmpty())
            append('|')
            append(record.lesson.cleanOrNull().orEmpty())
            append('|')
            append(primaryText)
            append('|')
            append(record.vi.cleanOrNull().orEmpty())
            append('|')
            append(index)
        }

        val hash = sha256(identitySeed).take(ID_HASH_LENGTH)
        val contentId = ContentId("legacy-content-$hash")

        val content = Content(
            id = contentId,
            type = inferContentType(
                sourceName = normalizedSource,
                record = record
            ),
            text = ContentText(
                primaryText = primaryText,
                translatedText = record.vi.cleanOrNull(),
                pronunciation = record.ipa.cleanOrNull(),
                exampleText = record.example.cleanOrNull(),
                exampleTranslation = record.exampleVi.cleanOrNull()
            ),
            media = ContentMedia(
                primaryAudio = record.audio.cleanOrNull(),
                translatedAudio = record.audioVi.cleanOrNull(),
                image = record.image.cleanOrNull(),
                exampleAudio = record.exampleAudio.cleanOrNull(),
                exampleTranslatedAudio =
                    record.exampleAudioVi.cleanOrNull()
            ),
            metadata = ContentMetadata(
                title = createTitle(primaryText),
                group = record.group.cleanOrNull(),
                section = record.section.cleanOrNull(),
                lesson = record.lesson.cleanOrNull(),
                tags = createTags(record),
                source = normalizedSource
            )
        )

        return ConversionResult.Success(
            content = content,
            learningItems = createLearningItems(content, record)
        )
    }

    private fun createLearningItems(
        content: Content,
        record: LegacySentenceJsonDto
    ): List<LearningItem> {
        val modes = linkedSetOf<LearningMode>()

        /*
         * Meaning Recognition là nhiệm vụ mặc định khi có câu nguồn.
         */
        modes += LearningMode.MEANING_RECOGNITION

        /*
         * Có bản dịch thì có thể luyện nhớ ngược từ nghĩa sang câu nguồn.
         */
        if (record.vi.cleanOrNull() != null) {
            modes += LearningMode.MEANING_RECALL
        }

        /*
         * Có audio nguồn thì có thể luyện nghe và chính tả.
         */
        if (record.audio.cleanOrNull() != null) {
            modes += LearningMode.LISTENING_RECOGNITION
            modes += LearningMode.DICTATION
            modes += LearningMode.SHADOWING
        }

        return modes.map { mode ->
            val modeName = mode.name
                .lowercase()
                .replace('_', '-')

            LearningItem(
                id = LearningItemId(
                    "${content.id.value}-$modeName"
                ),
                contentId = content.id,
                mode = mode
            )
        }
    }

    private fun inferContentType(
        sourceName: String,
        record: LegacySentenceJsonDto
    ): ContentType {
        val searchableText = buildString {
            append(sourceName)
            append(' ')
            append(record.group.orEmpty())
            append(' ')
            append(record.section.orEmpty())
        }.lowercase()

        return when {
            "vocabulary" in searchableText ->
                ContentType.WORD

            "conversation" in searchableText ||
                    "dialogue" in searchableText ->
                ContentType.DIALOGUE

            "short stor" in searchableText ||
                    "story" in searchableText ->
                ContentType.STORY

            "grammar" in searchableText ->
                ContentType.GRAMMAR_POINT

            else ->
                ContentType.SENTENCE
        }
    }

    private fun createTags(
        record: LegacySentenceJsonDto
    ): Set<String> =
        buildSet {
            record.group.cleanOrNull()?.let {
                add(normalizeTag(it))
            }

            record.section.cleanOrNull()?.let {
                add(normalizeTag(it))
            }

            record.lesson.cleanOrNull()?.let {
                add(normalizeTag(it))
            }

            add("legacy-import")
        }

    private fun createTitle(primaryText: String): String {
        val normalized = primaryText
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (normalized.length <= MAX_TITLE_LENGTH) {
            normalized
        } else {
            normalized.take(MAX_TITLE_LENGTH - 1) + "…"
        }
    }

    private fun normalizeTag(value: String): String =
        value
            .trim()
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
            .trim('-')
            .ifBlank { "legacy" }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")

        return digest
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte ->
                "%02x".format(byte)
            }
    }

    private fun String?.cleanOrNull(): String? =
        this
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private sealed interface ConversionResult {

        data class Success(
            val content: Content,
            val learningItems: List<LearningItem>
        ) : ConversionResult

        data class Skipped(
            val reason: String
        ) : ConversionResult
    }

    companion object {
        private const val ID_HASH_LENGTH = 24
        private const val MAX_TITLE_LENGTH = 120

        fun defaultJson(): Json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                explicitNulls = false
            }
    }
}