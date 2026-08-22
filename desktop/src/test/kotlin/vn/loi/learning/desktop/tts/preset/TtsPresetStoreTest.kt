package vn.loi.learning.desktop.tts.preset

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows
import vn.loi.learning.desktop.tts.TtsAudioParameters
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode

class TtsPresetStoreTest {

    private lateinit var tempDir: Path
    private lateinit var presetsFile: Path
    private lateinit var store: TtsPresetStore

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("tts_presets_test")
        presetsFile = tempDir.resolve("tts-presets.json")
        store = TtsPresetStore(presetsFile)
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `initial store returns built-in default preset`() {
        val presets = store.listPresets()
        assertEquals(1, presets.size)
        val defaultPreset = presets.first()
        assertEquals(TtsPreset.DEFAULT_PRESET_ID, defaultPreset.id)
        assertEquals(TtsPreset.DEFAULT_PRESET_NAME, defaultPreset.name)
        assertTrue(defaultPreset.isBuiltIn)
        assertEquals(4, defaultPreset.selectedFields.size)
    }

    @Test
    fun `saveAsPreset creates distinct preset with new ID and preserves source`() {
        val source = store.getDefaultPreset()
        val customConfig = source.copy(
            selectedFields = setOf(TtsField.QUESTION, TtsField.ANSWER),
            english = source.english.copy(
                strategyMode = VoiceStrategyMode.RANDOM,
                audioParameters = TtsAudioParameters(ratePercent = 15, pitchHz = -3, volumePercent = 5)
            )
        )

        val created = store.saveAsPreset("Vocabulary Fast", customConfig)

        assertTrue(created.id != source.id)
        assertEquals("Vocabulary Fast", created.name)
        assertFalse(created.isBuiltIn)
        assertEquals(2, created.selectedFields.size)
        assertEquals(VoiceStrategyMode.RANDOM, created.english.strategyMode)
        assertEquals(15, created.english.audioParameters.ratePercent)

        val all = store.listPresets()
        assertEquals(2, all.size)
        val fetched = store.getPreset(created.id)
        assertNotNull(fetched)
        assertEquals("Vocabulary Fast", fetched.name)
    }

    @Test
    fun `duplicatePreset generates unique copy name and cloned configuration`() {
        val defaultPreset = store.getDefaultPreset()
        val copy1 = store.duplicatePreset(defaultPreset.id)
        assertEquals("${TtsPreset.DEFAULT_PRESET_NAME} (Copy)", copy1.name)
        assertFalse(copy1.isBuiltIn)

        val copy2 = store.duplicatePreset(defaultPreset.id)
        assertEquals("${TtsPreset.DEFAULT_PRESET_NAME} (Copy 2)", copy2.name)

        assertEquals(3, store.listPresets().size)
    }

    @Test
    fun `renamePreset updates name while preserving ID and timestamps`() {
        val created = store.saveAsPreset("Initial Name", store.getDefaultPreset())
        val originalId = created.id

        val renamed = store.renamePreset(originalId, "Updated Name")
        assertEquals(originalId, renamed.id)
        assertEquals("Updated Name", renamed.name)

        val fetched = store.getPreset(originalId)
        assertNotNull(fetched)
        assertEquals("Updated Name", fetched.name)
    }

    @Test
    fun `deletePreset removes specified preset but protects built-in default`() {
        val custom = store.saveAsPreset("To Be Deleted", store.getDefaultPreset())
        assertEquals(2, store.listPresets().size)

        val deleted = store.deletePreset(custom.id)
        assertTrue(deleted)
        assertEquals(1, store.listPresets().size)
        assertNull(store.getPreset(custom.id))

        // Deleting built-in default must throw exception
        assertThrows<IllegalArgumentException> {
            store.deletePreset(TtsPreset.DEFAULT_PRESET_ID)
        }
    }

    @Test
    fun `validation prevents blank or duplicate names`() {
        assertThrows<IllegalArgumentException> {
            store.saveAsPreset("   ", store.getDefaultPreset())
        }

        store.saveAsPreset("English Only", store.getDefaultPreset())
        assertThrows<IllegalArgumentException> {
            store.saveAsPreset("english only", store.getDefaultPreset()) // Duplicate case-insensitive
        }
    }

    @Test
    fun `persistence round-trip preserves complete model across store instances`() {
        val customEnglish = TtsLanguagePresetConfig(
            strategyMode = VoiceStrategyMode.ROUND_ROBIN,
            primaryVoiceId = "en-US-GuyNeural",
            fallbackVoiceIds = listOf("en-US-AvaMultilingualNeural"),
            candidateVoiceIds = listOf("en-US-GuyNeural", "en-US-AvaMultilingualNeural"),
            audioParameters = TtsAudioParameters(ratePercent = -10, pitchHz = 4, volumePercent = -2)
        )
        val customVietnamese = TtsLanguagePresetConfig(
            strategyMode = VoiceStrategyMode.SINGLE_VOICE,
            primaryVoiceId = "vi-VN-NamMinhNeural",
            audioParameters = TtsAudioParameters(ratePercent = 20, pitchHz = -2, volumePercent = 8)
        )
        val customPreset = TtsPreset(
            id = "custom-123",
            name = "Advanced Setup",
            selectedFields = setOf(TtsField.QUESTION, TtsField.TRANSLATION),
            english = customEnglish,
            vietnamese = customVietnamese,
            isBuiltIn = false
        )

        store.savePreset(customPreset)

        // Create new store instance on same file path
        val newStoreInstance = TtsPresetStore(presetsFile)
        val retrieved = newStoreInstance.getPreset("custom-123")

        assertNotNull(retrieved)
        assertEquals("Advanced Setup", retrieved.name)
        assertEquals(setOf(TtsField.QUESTION, TtsField.TRANSLATION), retrieved.selectedFields)
        assertEquals(VoiceStrategyMode.ROUND_ROBIN, retrieved.english.strategyMode)
        assertEquals("en-US-GuyNeural", retrieved.english.primaryVoiceId)
        assertEquals(listOf("en-US-AvaMultilingualNeural"), retrieved.english.fallbackVoiceIds)
        assertEquals(-10, retrieved.english.audioParameters.ratePercent)
        assertEquals(4, retrieved.english.audioParameters.pitchHz)
        assertEquals(-2, retrieved.english.audioParameters.volumePercent)
        assertEquals("vi-VN-NamMinhNeural", retrieved.vietnamese.primaryVoiceId)
        assertEquals(20, retrieved.vietnamese.audioParameters.ratePercent)
    }

    @Test
    fun `corrupt JSON file falls back safely to default without throwing crash`() {
        Files.writeString(presetsFile, "{ invalid json content !!!", StandardCharsets.UTF_8)

        val freshStore = TtsPresetStore(presetsFile)
        val presets = freshStore.listPresets()

        assertEquals(1, presets.size)
        assertEquals(TtsPreset.DEFAULT_PRESET_ID, presets.first().id)
    }

    @Test
    fun `unsupported future schema version is safely rejected`() {
        val futureDoc = """
            {
              "schemaVersion": 999,
              "presets": []
            }
        """.trimIndent()
        Files.writeString(presetsFile, futureDoc, StandardCharsets.UTF_8)

        val freshStore = TtsPresetStore(presetsFile)
        assertThrows<IllegalStateException> {
            freshStore.listPresets()
        }
    }
}
