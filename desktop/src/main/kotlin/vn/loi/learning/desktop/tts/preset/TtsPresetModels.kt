package vn.loi.learning.desktop.tts.preset

import kotlinx.serialization.Serializable
import vn.loi.learning.desktop.tts.TtsAudioParameters
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode

/**
 * Immutable language-specific voice and audio configuration within a TTS preset.
 */
data class TtsLanguagePresetConfig(
    val strategyMode: VoiceStrategyMode = VoiceStrategyMode.FALLBACK_CHAIN,
    val primaryVoiceId: String,
    val fallbackVoiceIds: List<String> = emptyList(),
    val candidateVoiceIds: List<String> = emptyList(),
    val audioParameters: TtsAudioParameters = TtsAudioParameters.DEFAULT
)

/**
 * Immutable user preset storing complete audio field and voice settings.
 */
data class TtsPreset(
    val id: String,
    val name: String,
    val selectedFields: Set<TtsField> = TtsField.entries.toSet(),
    val english: TtsLanguagePresetConfig,
    val vietnamese: TtsLanguagePresetConfig,
    val isBuiltIn: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "Preset id must not be blank" }
        require(name.isNotBlank()) { "Preset name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Preset name must not exceed $MAX_NAME_LENGTH characters" }
    }

    companion object {
        const val MAX_NAME_LENGTH = 64
        const val DEFAULT_PRESET_ID = "builtin-default"
        const val DEFAULT_PRESET_NAME = "Default (Bilingual)"

        fun createDefault(
            englishVoiceId: String = "en-US-AvaMultilingualNeural",
            vietnameseVoiceId: String = "vi-VN-HoaiMyNeural"
        ): TtsPreset = TtsPreset(
            id = DEFAULT_PRESET_ID,
            name = DEFAULT_PRESET_NAME,
            selectedFields = setOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE, TtsField.TRANSLATION),
            english = TtsLanguagePresetConfig(
                strategyMode = VoiceStrategyMode.FALLBACK_CHAIN,
                primaryVoiceId = englishVoiceId,
                fallbackVoiceIds = emptyList(),
                candidateVoiceIds = listOf(englishVoiceId),
                audioParameters = TtsAudioParameters.DEFAULT
            ),
            vietnamese = TtsLanguagePresetConfig(
                strategyMode = VoiceStrategyMode.FALLBACK_CHAIN,
                primaryVoiceId = vietnameseVoiceId,
                fallbackVoiceIds = emptyList(),
                candidateVoiceIds = listOf(vietnameseVoiceId),
                audioParameters = TtsAudioParameters.DEFAULT
            ),
            isBuiltIn = true
        )
    }
}

// -----------------------------------------------------------------------------
// JSON Serialization DTOs
// -----------------------------------------------------------------------------

@Serializable
data class TtsLanguageConfigPresetJson(
    val strategyMode: String = VoiceStrategyMode.FALLBACK_CHAIN.name,
    val primaryVoiceId: String,
    val fallbackVoiceIds: List<String> = emptyList(),
    val candidateVoiceIds: List<String> = emptyList(),
    val ratePercent: Int = 0,
    val pitchHz: Int = 0,
    val volumePercent: Int = 0
)

@Serializable
data class TtsPresetJson(
    val id: String,
    val name: String,
    val selectedFields: List<String> = emptyList(),
    val english: TtsLanguageConfigPresetJson,
    val vietnamese: TtsLanguageConfigPresetJson,
    val isBuiltIn: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L
)

@Serializable
data class TtsPresetDocumentJson(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val presets: List<TtsPresetJson> = emptyList(),
    val selectedPresetId: String? = null
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
