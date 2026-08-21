package vn.loi.learning.desktop.tts

import java.nio.file.Path

/**
 * Logical field of learning content for TTS audio generation.
 */
enum class TtsField(val token: String, val displayName: String) {
    QUESTION("question", "Question"),
    ANSWER("answer", "Answer"),
    EXAMPLE("example", "Example"),
    TRANSLATION("translation", "Translation");

    companion object {
        fun fromToken(token: String): TtsField? =
            entries.firstOrNull { it.token.equals(token, ignoreCase = true) }
    }
}

/**
 * Primary target language supported by Desktop TTS.
 */
enum class TtsLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    VIETNAMESE("vi", "Vietnamese");

    companion object {
        fun fromCode(code: String?): TtsLanguage = when (code?.trim()?.lowercase()) {
            "vi", "vi-vn", "vietnamese" -> VIETNAMESE
            else -> ENGLISH
        }
    }
}

/**
 * Representation of a TTS voice.
 */
data class TtsVoice(
    val id: String,
    val displayName: String,
    val locale: String,
    val language: String,
    val gender: String? = null
) {
    val isEnglish: Boolean get() = locale.startsWith("en", ignoreCase = true)
    val isVietnamese: Boolean get() = locale.startsWith("vi", ignoreCase = true)
}

/**
 * Request payload for text synthesis.
 */
data class TtsSynthesisRequest(
    val text: String,
    val voice: TtsVoice,
    val rate: Int = 0,
    val pitch: String? = null,
    val volume: String? = null
)

/**
 * Result of a successful synthesis operation.
 */
data class TtsSynthesisResult(
    val outputFile: Path,
    val byteCount: Long
)

/**
 * Typed domain errors for TTS operations.
 */
sealed class TtsError(val message: String) {
    data class InvalidText(val reason: String) : TtsError("Invalid text: $reason")
    data class VoiceUnavailable(val voiceId: String) : TtsError("Voice unavailable: $voiceId")
    object NoNetwork : TtsError("No network connection available")
    object Timeout : TtsError("TTS service request timed out")
    data class ProviderUnavailable(val details: String) : TtsError("TTS provider unavailable: $details")
    data class GenerationFailed(val details: String) : TtsError("Audio generation failed: $details")
    data class OutputWriteFailed(val details: String) : TtsError("Failed to write output audio: $details")
    object Cancelled : TtsError("TTS operation was cancelled")
}

/**
 * Exception carrying a typed [TtsError].
 */
class TtsException(
    val error: TtsError,
    cause: Throwable? = null
) : Exception(error.message, cause)
