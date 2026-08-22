package vn.loi.learning.desktop.tts.strategy

import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.batch.TtsErrorCategory

/**
 * Strategy mode used to allocate and fallback voices for TTS synthesis.
 */
enum class VoiceStrategyMode(val displayName: String) {
    SINGLE_VOICE("Single Voice"),
    RANDOM("Random"),
    FALLBACK_CHAIN("Fallback Chain"),
    ROUND_ROBIN("Round Robin");

    companion object {
        val VOICE_ROTATION: VoiceStrategyMode get() = ROUND_ROBIN
    }
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

data class VoiceStrategyConfig(
    val mode: VoiceStrategyMode = VoiceStrategyMode.SINGLE_VOICE,
    val primaryVoice: TtsVoice,
    val fallbackVoices: List<TtsVoice> = emptyList(),
    val candidateVoices: List<TtsVoice> = emptyList(),
    val rotationVoices: List<TtsVoice> = candidateVoices,
    val continueSequenceAcrossItems: Boolean = true,
    val randomSource: kotlin.random.Random = kotlin.random.Random.Default
) {
    val effectiveCandidateVoices: List<TtsVoice>
        get() = when {
            candidateVoices.isNotEmpty() -> candidateVoices
            rotationVoices.isNotEmpty() -> rotationVoices
            else -> (listOf(primaryVoice) + fallbackVoices).distinctBy { it.id }
        }

    /**
     * Resolves the ordered candidate voice chain for a specific executable target index.
     * The first voice is the primary/requested voice; subsequent voices are fallbacks.
     */
    fun candidateChainForTarget(
        targetIndex: Int,
        random: kotlin.random.Random = randomSource
    ): List<TtsVoice> {
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
            VoiceStrategyMode.ROUND_ROBIN -> {
                val pool = effectiveCandidateVoices
                val poolIndex = if (pool.isNotEmpty()) (targetIndex.coerceAtLeast(0) % pool.size) else 0
                val chosenVoice = pool.getOrNull(poolIndex) ?: primaryVoice
                val chain = mutableListOf(chosenVoice)
                for (other in pool) {
                    if (chain.none { it.id == other.id }) {
                        chain.add(other)
                    }
                }
                for (fb in fallbackVoices) {
                    if (chain.none { it.id == fb.id }) {
                        chain.add(fb)
                    }
                }
                chain
            }
            VoiceStrategyMode.RANDOM -> {
                val pool = effectiveCandidateVoices
                val chosenVoice = if (pool.size > 1) {
                    pool[random.nextInt(pool.size)]
                } else {
                    pool.firstOrNull() ?: primaryVoice
                }
                val chain = mutableListOf(chosenVoice)
                for (other in pool) {
                    if (chain.none { it.id == other.id }) {
                        chain.add(other)
                    }
                }
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
