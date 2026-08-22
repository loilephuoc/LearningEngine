package vn.loi.learning.desktop.tts.strategy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.tts.TtsVoice

class TtsVoiceStrategyTest {

    private val ava = TtsVoice("en-US-AvaMultilingualNeural", "Ava", "en-US", "en", "Female")
    private val jenny = TtsVoice("en-US-JennyNeural", "Jenny", "en-US", "en", "Female")
    private val guy = TtsVoice("en-US-GuyNeural", "Guy", "en-US", "en", "Male")
    private val andrew = TtsVoice("en-US-AndrewMultilingualNeural", "Andrew", "en-US", "en", "Male")

    @Test
    fun `Single Voice strategy returns only primary voice for all targets`() {
        val config = VoiceStrategyConfig(
            mode = VoiceStrategyMode.SINGLE_VOICE,
            primaryVoice = ava
        )

        assertEquals(listOf(ava), config.candidateChainForTarget(0))
        assertEquals(listOf(ava), config.candidateChainForTarget(1))
        assertEquals(listOf(ava), config.candidateChainForTarget(42))
    }

    @Test
    fun `Fallback Chain strategy returns primary voice followed by unique fallbacks`() {
        val config = VoiceStrategyConfig(
            mode = VoiceStrategyMode.FALLBACK_CHAIN,
            primaryVoice = ava,
            fallbackVoices = listOf(jenny, guy, ava, andrew) // ava is duplicate, should be filtered
        )

        val chain = config.candidateChainForTarget(0)
        assertEquals(4, chain.size)
        assertEquals(ava.id, chain[0].id)
        assertEquals(jenny.id, chain[1].id)
        assertEquals(guy.id, chain[2].id)
        assertEquals(andrew.id, chain[3].id)
    }

    @Test
    fun `Voice Rotation strategy cycles through voices across targets`() {
        val config = VoiceStrategyConfig(
            mode = VoiceStrategyMode.VOICE_ROTATION,
            primaryVoice = ava,
            rotationVoices = listOf(ava, guy, jenny, andrew)
        )

        val chain0 = config.candidateChainForTarget(0)
        assertEquals(ava.id, chain0.first().id)

        val chain1 = config.candidateChainForTarget(1)
        assertEquals(guy.id, chain1.first().id)

        val chain2 = config.candidateChainForTarget(2)
        assertEquals(jenny.id, chain2.first().id)

        val chain3 = config.candidateChainForTarget(3)
        assertEquals(andrew.id, chain3.first().id)

        val chain4 = config.candidateChainForTarget(4)
        assertEquals(ava.id, chain4.first().id) // Rotates back to start!
    }

    @Test
    fun `Voice Rotation strategy includes other rotation voices as fallbacks`() {
        val config = VoiceStrategyConfig(
            mode = VoiceStrategyMode.ROUND_ROBIN,
            primaryVoice = ava,
            candidateVoices = listOf(ava, guy, jenny)
        )

        val chain1 = config.candidateChainForTarget(1) // Guy
        assertEquals(guy.id, chain1[0].id)
        assertTrue(chain1.any { it.id == ava.id })
        assertTrue(chain1.any { it.id == jenny.id })
    }

    @Test
    fun `Random strategy with deterministic seed selects random voice and includes fallbacks`() {
        val config = VoiceStrategyConfig(
            mode = VoiceStrategyMode.RANDOM,
            primaryVoice = ava,
            candidateVoices = listOf(ava, guy, jenny, andrew)
        )

        val seededRandom = kotlin.random.Random(42)
        val chain0 = config.candidateChainForTarget(0, seededRandom)
        val selected0 = chain0.first()

        assertTrue(selected0 in listOf(ava, guy, jenny, andrew))
        assertEquals(4, chain0.size)
        // All candidate voices are in the chain
        assertTrue(chain0.any { it.id == ava.id })
        assertTrue(chain0.any { it.id == guy.id })
        assertTrue(chain0.any { it.id == jenny.id })
        assertTrue(chain0.any { it.id == andrew.id })
    }

    @Test
    fun `Random strategy with single voice returns only that voice`() {
        val config = VoiceStrategyConfig(
            mode = VoiceStrategyMode.RANDOM,
            primaryVoice = ava,
            candidateVoices = listOf(ava)
        )

        val chain = config.candidateChainForTarget(0)
        assertEquals(listOf(ava), chain)
    }
}
