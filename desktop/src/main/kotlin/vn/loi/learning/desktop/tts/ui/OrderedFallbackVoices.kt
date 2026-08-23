package vn.loi.learning.desktop.tts.ui

import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice

internal class OrderedFallbackVoices private constructor(val voices: List<TtsVoice>) {
    val ids: List<String> get() = voices.map { it.id }

    fun add(voice: TtsVoice, primary: TtsVoice?): OrderedFallbackVoices {
        require(voices.size < MAX_FALLBACKS) { "At most $MAX_FALLBACKS fallback voices are allowed" }
        require(primary?.id != voice.id) { "Primary voice cannot also be a fallback" }
        require(voices.none { it.id == voice.id }) { "Fallback voices must be unique" }
        require(voices.all { it.language.equals(voice.language, true) }) { "Fallback voices must use one language" }
        return OrderedFallbackVoices(voices + voice)
    }

    fun remove(voiceId: String) = OrderedFallbackVoices(voices.filterNot { it.id == voiceId })

    fun move(voiceId: String, offset: Int): OrderedFallbackVoices {
        val from = voices.indexOfFirst { it.id == voiceId }
        if (from < 0) return this
        val to = (from + offset).coerceIn(0, voices.lastIndex)
        if (from == to) return this
        val reordered = voices.toMutableList()
        val voice = reordered.removeAt(from)
        reordered.add(to, voice)
        return OrderedFallbackVoices(reordered)
    }

    companion object {
        const val MAX_FALLBACKS = 3
        val EMPTY = OrderedFallbackVoices(emptyList())

        fun hydrate(ids: List<String>, catalog: List<TtsVoice>, language: TtsLanguage, primary: TtsVoice?): OrderedFallbackVoices {
            val byId = catalog.associateBy { it.id }
            val resolved = ids.asSequence()
                .distinct()
                .mapNotNull(byId::get)
                .filter { it.language.equals(language.code, true) || it.locale.startsWith(language.code, true) }
                .filterNot { it.id == primary?.id }
                .take(MAX_FALLBACKS)
                .toList()
            return OrderedFallbackVoices(resolved)
        }
    }
}
