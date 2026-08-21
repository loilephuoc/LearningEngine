package vn.loi.learning.desktop.tts.profile

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TtsVoiceProfileTest {

    private lateinit var tempDir: Path
    private lateinit var preferencesStore: TtsVoiceProfilePreferencesStore

    private val avaVoice = TtsVoice("en-US-AvaMultilingualNeural", "Ava", "en-US", "en", "Female")
    private val guyVoice = TtsVoice("en-US-GuyNeural", "Guy", "en-US", "en", "Male")
    private val hoaiMyVoice = TtsVoice("vi-VN-HoaiMyNeural", "HoaiMy", "vi-VN", "vi", "Female")
    private val namMinhVoice = TtsVoice("vi-VN-NamMinhNeural", "NamMinh", "vi-VN", "vi", "Male")

    private val testVoices = listOf(avaVoice, guyVoice, hoaiMyVoice, namMinhVoice)

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("tts-profile-test-")
        preferencesStore = TtsVoiceProfilePreferencesStore(tempDir)
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `default profiles have valid English and Vietnamese configurations`() {
        val profiles = TtsVoiceProfiles()

        assertEquals(TtsLanguage.ENGLISH, profiles.english.language)
        assertEquals("en-US", profiles.english.region)
        assertEquals("en-US-AvaMultilingualNeural", profiles.english.voiceId)

        assertEquals(TtsLanguage.VIETNAMESE, profiles.vietnamese.language)
        assertEquals("vi-VN", profiles.vietnamese.region)
        assertEquals("vi-VN-HoaiMyNeural", profiles.vietnamese.voiceId)
    }

    @Test
    fun `preferences store persists and reloads custom profiles`() {
        val customProfiles = TtsVoiceProfiles(
            english = TtsVoiceProfile(TtsLanguage.ENGLISH, "en-US", "en-US-GuyNeural", 10),
            vietnamese = TtsVoiceProfile(TtsLanguage.VIETNAMESE, "vi-VN", "vi-VN-NamMinhNeural", -5)
        )

        preferencesStore.saveProfiles(customProfiles)

        val reloaded = preferencesStore.loadProfiles()
        assertEquals("en-US-GuyNeural", reloaded.english.voiceId)
        assertEquals(10, reloaded.english.rate)
        assertEquals("vi-VN-NamMinhNeural", reloaded.vietnamese.voiceId)
        assertEquals(-5, reloaded.vietnamese.rate)
    }

    @Test
    fun `resetToDefaults restores default configurations`() {
        val customProfiles = TtsVoiceProfiles(
            english = TtsVoiceProfile(TtsLanguage.ENGLISH, "en-US", "en-US-GuyNeural", 10),
            vietnamese = TtsVoiceProfile(TtsLanguage.VIETNAMESE, "vi-VN", "vi-VN-NamMinhNeural", -5)
        )
        preferencesStore.saveProfiles(customProfiles)

        val reset = preferencesStore.resetToDefaults()
        assertEquals(TtsVoiceProfile.DEFAULT_ENGLISH.voiceId, reset.english.voiceId)
        assertEquals(TtsVoiceProfile.DEFAULT_VIETNAMESE.voiceId, reset.vietnamese.voiceId)
    }

    @Test
    fun `resolveVoice falls back gracefully when voiceId is unavailable`() {
        val unavailableProfile = TtsVoiceProfiles(
            english = TtsVoiceProfile(TtsLanguage.ENGLISH, "en-US", "non-existent-voice-id")
        )

        // Resolves to another matching en-US voice from available list
        val resolved = unavailableProfile.resolveVoice(TtsLanguage.ENGLISH, testVoices)
        assertNotNull(resolved)
        assertEquals("en", resolved.language)
    }

    @Test
    fun `profileForField routes correctly based on domain field`() {
        val profiles = TtsVoiceProfiles()

        assertEquals(profiles.english, profiles.profileForField(TtsField.QUESTION))
        assertEquals(profiles.english, profiles.profileForField(TtsField.ANSWER))
        assertEquals(profiles.english, profiles.profileForField(TtsField.EXAMPLE))
        assertEquals(profiles.vietnamese, profiles.profileForField(TtsField.TRANSLATION))
    }
}
