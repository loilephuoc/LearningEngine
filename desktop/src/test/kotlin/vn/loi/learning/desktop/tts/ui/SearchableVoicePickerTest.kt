package vn.loi.learning.desktop.tts.ui

import vn.loi.learning.desktop.tts.TtsVoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchableVoicePickerTest {

    private val voices = listOf(
        TtsVoice("en-US-AvaMultilingualNeural", "Microsoft Ava", "en-US", "en", "Female"),
        TtsVoice("en-US-AndrewMultilingualNeural", "Microsoft Andrew", "en-US", "en", "Male"),
        TtsVoice("en-GB-SoniaNeural", "Microsoft Sonia", "en-GB", "en", "Female"),
        TtsVoice("en-AU-NatashaNeural", "Microsoft Natasha", "en-AU", "en", "Female"),
        TtsVoice("en-SG-LunaNeural", "Microsoft Luna", "en-SG", "en", "Female"),
        TtsVoice("en-CA-ClaraNeural", "Microsoft Clara", "en-CA", "en", "Female"),
        TtsVoice("vi-VN-HoaiMyNeural", "Microsoft HoaiMy", "vi-VN", "vi", "Female"),
        TtsVoice("vi-VN-NamMinhNeural", "Microsoft NamMinh", "vi-VN", "vi", "Male")
    )

    @Test
    fun `extractRegions extracts unique formatted regions from catalog`() {
        val regions = SearchableVoicePickerHelper.extractRegions(voices)
        val regionCodes = regions.map { it.localeCode }.toSet()

        assertTrue(regionCodes.contains("en-US"))
        assertTrue(regionCodes.contains("en-GB"))
        assertTrue(regionCodes.contains("en-AU"))
        assertTrue(regionCodes.contains("en-SG"))
        assertTrue(regionCodes.contains("en-CA"))
        assertTrue(regionCodes.contains("vi-VN"))
        assertEquals(6, regions.size)
    }

    @Test
    fun `matchesQuery searches accurately across display name, id, locale, region, and synonyms`() {
        val ava = voices[0]
        val sonia = voices[2]
        val hoaiMy = voices[6]
        val luna = voices[4]

        // Match by display name
        assertTrue(SearchableVoicePickerHelper.matchesQuery(ava, "Ava"))
        assertFalse(SearchableVoicePickerHelper.matchesQuery(ava, "Sonia"))

        // Match by ID
        assertTrue(SearchableVoicePickerHelper.matchesQuery(ava, "AvaMultilingual"))

        // Match by locale code
        assertTrue(SearchableVoicePickerHelper.matchesQuery(sonia, "en-GB"))
        assertTrue(SearchableVoicePickerHelper.matchesQuery(hoaiMy, "vi-VN"))

        // Match by geographic synonym
        assertTrue(SearchableVoicePickerHelper.matchesQuery(ava, "US"))
        assertTrue(SearchableVoicePickerHelper.matchesQuery(ava, "American"))
        assertTrue(SearchableVoicePickerHelper.matchesQuery(sonia, "UK"))
        assertTrue(SearchableVoicePickerHelper.matchesQuery(sonia, "British"))
        assertTrue(SearchableVoicePickerHelper.matchesQuery(luna, "Singapore"))
        assertTrue(SearchableVoicePickerHelper.matchesQuery(hoaiMy, "Vietnam"))
    }
}
