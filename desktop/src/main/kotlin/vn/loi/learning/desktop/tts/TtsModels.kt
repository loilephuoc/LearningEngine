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
 * Canonical parameters for audio synthesis (Speed, Pitch, Volume) with strict bounds and unit formatting.
 */
data class TtsAudioParameters(
    val ratePercent: Int = 0,
    val pitchHz: Int = 0,
    val volumePercent: Int = 0
) {
    init {
        require(ratePercent in MIN_RATE..MAX_RATE) {
            "Rate percent must be between $MIN_RATE and $MAX_RATE, but was $ratePercent"
        }
        require(pitchHz in MIN_PITCH..MAX_PITCH) {
            "Pitch Hz must be between $MIN_PITCH and $MAX_PITCH, but was $pitchHz"
        }
        require(volumePercent in MIN_VOLUME..MAX_VOLUME) {
            "Volume percent must be between $MIN_VOLUME and $MAX_VOLUME, but was $volumePercent"
        }
    }

    fun toProviderRate(): Int = ratePercent
    fun toProviderPitch(): String = if (pitchHz >= 0) "+${pitchHz}Hz" else "${pitchHz}Hz"
    fun toProviderVolume(): String = if (volumePercent >= 0) "+${volumePercent}%" else "${volumePercent}%"

    companion object {
        const val MIN_RATE = -50
        const val MAX_RATE = 50
        const val MIN_PITCH = -50
        const val MAX_PITCH = 50
        const val MIN_VOLUME = -50
        const val MAX_VOLUME = 50

        val DEFAULT = TtsAudioParameters()

        fun validateRate(value: Int): String? =
            if (value in MIN_RATE..MAX_RATE) null else "Rate must be between $MIN_RATE% and +$MAX_RATE%"

        fun validatePitch(value: Int): String? =
            if (value in MIN_PITCH..MAX_PITCH) null else "Pitch must be between $MIN_PITCH Hz and +$MAX_PITCH Hz"

        fun validateVolume(value: Int): String? =
            if (value in MIN_VOLUME..MAX_VOLUME) null else "Volume must be between $MIN_VOLUME% and +$MAX_VOLUME%"

        fun parsePitchHz(raw: String?, defaultHz: Int = 0): Int {
            if (raw.isNullOrBlank()) return defaultHz
            val clean = raw.trim().replace("Hz", "", ignoreCase = true).replace("+", "").trim()
            return clean.toIntOrNull()?.coerceIn(MIN_PITCH, MAX_PITCH) ?: defaultHz
        }

        fun parseVolumePercent(raw: String?, defaultPercent: Int = 0): Int {
            if (raw.isNullOrBlank()) return defaultPercent
            val clean = raw.trim().replace("%", "").replace("+", "").trim()
            return clean.toIntOrNull()?.coerceIn(MIN_VOLUME, MAX_VOLUME) ?: defaultPercent
        }
    }
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
