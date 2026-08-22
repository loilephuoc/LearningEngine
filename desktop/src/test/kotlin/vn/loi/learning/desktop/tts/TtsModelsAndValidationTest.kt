package vn.loi.learning.desktop.tts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TtsModelsAndValidationTest {

    @Test
    fun `TtsField fromToken handles casing and nulls`() {
        assertEquals(TtsField.QUESTION, TtsField.fromToken("question"))
        assertEquals(TtsField.QUESTION, TtsField.fromToken("QUESTION"))
        assertEquals(TtsField.ANSWER, TtsField.fromToken("answer"))
        assertEquals(TtsField.EXAMPLE, TtsField.fromToken("example"))
        assertEquals(TtsField.TRANSLATION, TtsField.fromToken("translation"))
        assertEquals(null, TtsField.fromToken("invalid"))
        assertEquals(null, TtsField.fromToken(""))
    }

    @Test
    fun `TtsLanguage fromCode resolves English and Vietnamese`() {
        assertEquals(TtsLanguage.ENGLISH, TtsLanguage.fromCode("en"))
        assertEquals(TtsLanguage.ENGLISH, TtsLanguage.fromCode("EN"))
        assertEquals(TtsLanguage.VIETNAMESE, TtsLanguage.fromCode("vi"))
        assertEquals(TtsLanguage.VIETNAMESE, TtsLanguage.fromCode("vi-VN"))
        assertEquals(TtsLanguage.VIETNAMESE, TtsLanguage.fromCode("Vietnamese"))
        assertEquals(TtsLanguage.ENGLISH, TtsLanguage.fromCode("unknown"))
        assertEquals(TtsLanguage.ENGLISH, TtsLanguage.fromCode(null))
    }

    @Test
    fun `TtsVoice language predicate works accurately`() {
        val enVoice = TtsVoice(
            id = "en-US-AvaMultilingualNeural",
            displayName = "Microsoft Ava",
            locale = "en-US",
            language = "en",
            gender = "Female"
        )
        assertTrue(enVoice.isEnglish)
        assertFalse(enVoice.isVietnamese)

        val viVoice = TtsVoice(
            id = "vi-VN-HoaiMyNeural",
            displayName = "Microsoft HoaiMy",
            locale = "vi-VN",
            language = "vi",
            gender = "Female"
        )
        assertTrue(viVoice.isVietnamese)
        assertFalse(viVoice.isEnglish)
    }

    @Test
    fun `TtsError messages provide structured diagnostics`() {
        val err1 = TtsError.InvalidText("Empty input")
        assertEquals("Invalid text: Empty input", err1.message)

        val err2 = TtsError.VoiceUnavailable("voice-123")
        assertEquals("Voice unavailable: voice-123", err2.message)

        val err3 = TtsError.NoNetwork
        assertEquals("No network connection available", err3.message)

        val err4 = TtsError.Timeout
        assertEquals("TTS service request timed out", err4.message)

        val err5 = TtsError.Cancelled
        assertEquals("TTS operation was cancelled", err5.message)
    }

    @Test
    fun `TtsAudioParameters validates bounds strictly`() {
        val validDefault = TtsAudioParameters()
        assertEquals(0, validDefault.ratePercent)
        assertEquals(0, validDefault.pitchHz)
        assertEquals(0, validDefault.volumePercent)
        assertEquals("+0Hz", validDefault.toProviderPitch())
        assertEquals("+0%", validDefault.toProviderVolume())

        val minParams = TtsAudioParameters(ratePercent = -50, pitchHz = -50, volumePercent = -50)
        assertEquals(-50, minParams.toProviderRate())
        assertEquals("-50Hz", minParams.toProviderPitch())
        assertEquals("-50%", minParams.toProviderVolume())

        val maxParams = TtsAudioParameters(ratePercent = 50, pitchHz = 50, volumePercent = 50)
        assertEquals(50, maxParams.toProviderRate())
        assertEquals("+50Hz", maxParams.toProviderPitch())
        assertEquals("+50%", maxParams.toProviderVolume())

        val arbitrary = TtsAudioParameters(ratePercent = 25, pitchHz = -5, volumePercent = 10)
        assertEquals(25, arbitrary.toProviderRate())
        assertEquals("-5Hz", arbitrary.toProviderPitch())
        assertEquals("+10%", arbitrary.toProviderVolume())
    }

    @Test
    fun `TtsAudioParameters throws on out of bound values`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TtsAudioParameters(ratePercent = -51)
        }
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TtsAudioParameters(ratePercent = 51)
        }
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TtsAudioParameters(pitchHz = -51)
        }
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TtsAudioParameters(pitchHz = 51)
        }
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TtsAudioParameters(volumePercent = -51)
        }
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TtsAudioParameters(volumePercent = 51)
        }
    }

    @Test
    fun `TtsAudioParameters parse and validation helpers handle edge cases`() {
        assertEquals(null, TtsAudioParameters.validateRate(-50))
        assertEquals(null, TtsAudioParameters.validateRate(0))
        assertEquals(null, TtsAudioParameters.validateRate(50))
        assertTrue(TtsAudioParameters.validateRate(-51) != null)
        assertTrue(TtsAudioParameters.validateRate(51) != null)

        assertEquals(15, TtsAudioParameters.parsePitchHz("+15Hz"))
        assertEquals(-10, TtsAudioParameters.parsePitchHz("-10Hz"))
        assertEquals(0, TtsAudioParameters.parsePitchHz("0"))
        assertEquals(0, TtsAudioParameters.parsePitchHz("invalid"))

        assertEquals(25, TtsAudioParameters.parseVolumePercent("+25%"))
        assertEquals(-15, TtsAudioParameters.parseVolumePercent("-15%"))
        assertEquals(0, TtsAudioParameters.parseVolumePercent(null))
    }
}
