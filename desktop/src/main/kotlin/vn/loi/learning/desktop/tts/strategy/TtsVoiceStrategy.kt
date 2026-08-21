package vn.loi.learning.desktop.tts.strategy

import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.batch.TtsErrorCategory

/**
 * Strategy mode used to allocate and fallback voices for TTS synthesis.
 */
enum class VoiceStrategyMode(val displayName: String) {
    SINGLE_VOICE("Single Voice"),
    FALLBACK_CHAIN("Fallback Chain"),
    VOICE_ROTATION("Voice Rotation / Workflow")
}

/**
 * Record of a single synthesis attempt with a specific voice.
 */
data class VoiceAttempt(
    val voice: TtsVoice,
    val isSuccess: Boolean,
    val errorCategory: TtsErrorCategory? = null,
    val errorMessage: String? = null
)

/**
 * Configuration for voice resolution and fallback execution per language.
 */
data class VoiceStrategyConfig(
    val mode: VoiceStrategyMode = VoiceStrategyMode.SINGLE_VOICE,
    val primaryVoice: TtsVoice,
    val fallbackVoices: List<TtsVoice> = emptyList(),
    val rotationVoices: List<TtsVoice> = emptyList(),
    val continueSequenceAcrossItems: Boolean = true
) {
    /**
     * Resolves the ordered candidate voice chain for a specific target index.
     * The first voice is the primary/requested voice; subsequent voices are fallbacks.
     */
    fun candidateChainForTarget(targetIndex: Int): List<TtsVoice> {
        return when (mode) {
            VoiceStrategyMode.SINGLE_VOICE -> {
                listOf(primaryVoice)
            }
            VoiceStrategyMode.FALLBACK_CHAIN -> {
                val chain = mutableListOf(primaryVoice)
                for (fb in fallbackVoices) {
                    if (chain.none { it.id == fb.id }) {
                        chain.add(fb)
                    }
                }
                chain
            }
            VoiceStrategyMode.VOICE_ROTATION -> {
                val voices = if (rotationVoices.isNotEmpty()) rotationVoices else listOf(primaryVoice)
                val primaryIndex = (if (continueSequenceAcrossItems) targetIndex else targetIndex) % voices.size
                val chosenVoice = voices[primaryIndex]
                val chain = mutableListOf(chosenVoice)
                // Add remaining rotation voices as fallbacks
                for (other in voices) {
                    if (chain.none { it.id == other.id }) {
                        chain.add(other)
                    }
                }
                // Add explicit fallback voices
                for (fb in fallbackVoices) {
                    if (chain.none { it.id == fb.id }) {
                        chain.add(fb)
                    }
                }
                chain
            }
        }
    }
}
