package vn.loi.learning.desktop.tts.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode

class OrderedFallbackVoicesTest {
    private val primary = voice("primary")
    private val one = voice("one")
    private val two = voice("two")
    private val three = voice("three")
    private val vi = TtsVoice("vi", "Vietnamese", "vi-VN", "vi")

    @Test
    fun `add remove and reorder preserve explicit execution order`() {
        val configured = OrderedFallbackVoices.EMPTY.add(one, primary).add(two, primary).add(three, primary)
        assertEquals(listOf("one", "two", "three"), configured.ids)
        assertEquals(listOf("two", "one", "three"), configured.move("two", -1).ids)
        assertEquals(listOf("one", "three"), configured.remove("two").ids)
    }

    @Test
    fun `duplicate primary wrong language and fourth fallback are rejected`() {
        assertFailsWith<IllegalArgumentException> { OrderedFallbackVoices.EMPTY.add(primary, primary) }
        assertFailsWith<IllegalArgumentException> { OrderedFallbackVoices.EMPTY.add(one, primary).add(one, primary) }
        assertFailsWith<IllegalArgumentException> { OrderedFallbackVoices.EMPTY.add(one, primary).add(vi, primary) }
        assertFailsWith<IllegalArgumentException> {
            OrderedFallbackVoices.EMPTY.add(one, primary).add(two, primary).add(three, primary).add(voice("four"), primary)
        }
    }

    @Test
    fun `preset hydration keeps valid order and ignores missing wrong language and primary`() {
        val hydrated = OrderedFallbackVoices.hydrate(
            listOf("two", "missing", "vi", "primary", "one", "two"),
            listOf(primary, one, two, vi), TtsLanguage.ENGLISH, primary
        )
        assertEquals(listOf("two", "one"), hydrated.ids)
    }

    @Test
    fun `runtime fallback chain is exactly primary then configured order with no catalog append`() {
        val configured = OrderedFallbackVoices.EMPTY.add(two, primary).add(one, primary)
        val strategy = VoiceStrategyConfig(
            mode = VoiceStrategyMode.FALLBACK_CHAIN,
            primaryVoice = primary,
            fallbackVoices = configured.voices,
            candidateVoices = listOf(primary) + configured.voices
        )
        assertEquals(listOf("primary", "two", "one"), strategy.candidateChainForTarget(0).map { it.id })
    }

    private fun voice(id: String) = TtsVoice(id, id, "en-US", "en")
}
