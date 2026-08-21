package vn.loi.learning.desktop.tts

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates deterministic and collision-safe filenames for TTS audio assets.
 * Format: {contentId}_{field}_{lang}_{yyyyMMdd_HH-mm-ss-SSS}.mp3
 * Example: 1842_question_en_20260822_04-44-23-184.mp3
 */
class TtsAudioFileNamer(
    private val clock: Clock = Clock.systemDefaultZone()
) {

    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HH-mm-ss-SSS")

    /**
     * Formats timestamp using the injected clock.
     */
    fun formatTimestamp(dateTime: LocalDateTime = LocalDateTime.now(clock)): String =
        dateTime.format(timestampFormatter)

    /**
     * Builds the base filename before collision resolution.
     */
    fun buildBaseFileName(
        contentId: String,
        field: TtsField,
        languageCode: String,
        timestamp: String = formatTimestamp()
    ): String {
        require(contentId.isNotBlank()) { "Content ID must not be blank" }
        require(languageCode.isNotBlank()) { "Language code must not be blank" }

        val safeContentId = sanitizeToken(contentId)
        val fieldToken = field.token
        val safeLanguage = sanitizeToken(languageCode.lowercase())

        return "${safeContentId}_${fieldToken}_${safeLanguage}_${timestamp}.mp3"
    }

    /**
     * Generates a unique filename, applying bounded collision resolution if the file already exists.
     * Collisions append _2, _3, etc. before the .mp3 extension.
     */
    fun generateUniqueFileName(
        contentId: String,
        field: TtsField,
        languageCode: String,
        exists: (String) -> Boolean
    ): String {
        val baseFileName = buildBaseFileName(contentId, field, languageCode)
        if (!exists(baseFileName)) {
            return baseFileName
        }

        val nameWithoutExt = baseFileName.removeSuffix(".mp3")
        var suffixIndex = 2
        while (suffixIndex < 1000) {
            val candidate = "${nameWithoutExt}_$suffixIndex.mp3"
            if (!exists(candidate)) {
                return candidate
            }
            suffixIndex++
        }

        throw IllegalStateException("Exceeded maximum collision attempts for TTS filename: $baseFileName")
    }

    private fun sanitizeToken(token: String): String =
        token.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
}
