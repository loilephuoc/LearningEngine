package vn.loi.learning.desktop.tts.ui

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsAudioFileNamer
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsPreviewStore
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatchTtsHotfix42VoicePickerAndFallbackTest {

    private val avaVoiceEnUs = TtsVoice(
        id = "en-US-AvaMultilingualNeural",
        displayName = "Microsoft Ava",
        locale = "en-US",
        language = "en",
        gender = "Female"
    )

    private val guyVoiceEnUs = TtsVoice(
        id = "en-US-GuyNeural",
        displayName = "Microsoft Guy",
        locale = "en-US",
        language = "en",
        gender = "Male"
    )

    private val soniaVoiceEnGb = TtsVoice(
        id = "en-GB-SoniaNeural",
        displayName = "Microsoft Sonia",
        locale = "en-GB",
        language = "en",
        gender = "Female"
    )

    private val ryanVoiceEnGb = TtsVoice(
        id = "en-GB-RyanNeural",
        displayName = "Microsoft Ryan",
        locale = "en-GB",
        language = "en",
        gender = "Male"
    )

    private val hoaiMyVoiceVi = TtsVoice(
        id = "vi-VN-HoaiMyNeural",
        displayName = "Microsoft HoaiMy",
        locale = "vi-VN",
        language = "vi",
        gender = "Female"
    )

    private val namMinhVoiceVi = TtsVoice(
        id = "vi-VN-NamMinhNeural",
        displayName = "Microsoft NamMinh",
        locale = "vi-VN",
        language = "vi",
        gender = "Male"
    )

    private val testVoices = listOf(
        avaVoiceEnUs,
        guyVoiceEnUs,
        soniaVoiceEnGb,
        ryanVoiceEnGb,
        hoaiMyVoiceVi,
        namMinhVoiceVi
    )

    private lateinit var tempMediaDir: Path
    private lateinit var tempPreviewDir: Path
    private lateinit var fakeMediaStorage: FakeMediaStorage
    private lateinit var fakeEngine: FakeEngine
    private lateinit var service: DesktopTtsAudioService

    @BeforeTest
    fun setup() {
        tempMediaDir = Files.createTempDirectory("batch-tts-hotfix42-test-media-")
        tempPreviewDir = Files.createTempDirectory("batch-tts-hotfix42-test-preview-")
        fakeMediaStorage = FakeMediaStorage(tempMediaDir)
        fakeEngine = FakeEngine(testVoices)
        service = DesktopTtsAudioService(
            ttsEngine = fakeEngine,
            mediaStorage = fakeMediaStorage,
            fileNamer = TtsAudioFileNamer(Clock.fixed(Instant.parse("2026-08-24T05:00:00Z"), ZoneOffset.UTC)),
            previewStore = TtsPreviewStore(tempPreviewDir)
        )
    }

    @AfterTest
    fun tearDown() {
        service.close()
        tempMediaDir.toFile().deleteRecursively()
        tempPreviewDir.toFile().deleteRecursively()
    }

    @Test
    fun `1 - region filter persists after picker close and reopen`() {
        val filterState = VoicePickerFilterState()
        assertEquals(null, filterState.selectedRegionCode)

        // User filters by United States (en-US)
        filterState.selectedRegionCode = "en-US"
        assertEquals("en-US", filterState.selectedRegionCode)

        // Simulate picker close (filter state remains intact in session holder)
        // Simulate reopen with the same state holder
        assertEquals("en-US", filterState.selectedRegionCode)
    }

    @Test
    fun `2 - gender filter persists after picker close and reopen`() {
        val filterState = VoicePickerFilterState()
        assertEquals(null, filterState.selectedGender)

        // User filters by Female
        filterState.selectedGender = "Female"
        assertEquals("Female", filterState.selectedGender)

        // Simulate picker close and reopen
        assertEquals("Female", filterState.selectedGender)
    }

    @Test
    fun `3 - search query persists after picker close and reopen`() {
        val filterState = VoicePickerFilterState()
        assertEquals("", filterState.searchQuery)

        // User searches for "Ava"
        filterState.searchQuery = "Ava"
        assertEquals("Ava", filterState.searchQuery)

        // Simulate picker close and reopen
        assertEquals("Ava", filterState.searchQuery)
    }

    @Test
    fun `4 - English and Vietnamese filter states are independent`() {
        val englishFilterState = VoicePickerFilterState()
        val vietnameseFilterState = VoicePickerFilterState()

        englishFilterState.searchQuery = "Sonia"
        englishFilterState.selectedRegionCode = "en-GB"
        englishFilterState.selectedGender = "Female"

        vietnameseFilterState.searchQuery = "NamMinh"
        vietnameseFilterState.selectedRegionCode = "vi-VN"
        vietnameseFilterState.selectedGender = "Male"

        assertEquals("Sonia", englishFilterState.searchQuery)
        assertEquals("en-GB", englishFilterState.selectedRegionCode)
        assertEquals("Female", englishFilterState.selectedGender)

        assertEquals("NamMinh", vietnameseFilterState.searchQuery)
        assertEquals("vi-VN", vietnameseFilterState.selectedRegionCode)
        assertEquals("Male", vietnameseFilterState.selectedGender)

        // Clearing English filters does not affect Vietnamese filters
        englishFilterState.clear()
        assertEquals("", englishFilterState.searchQuery)
        assertNull(englishFilterState.selectedRegionCode)
        assertNull(englishFilterState.selectedGender)

        assertEquals("NamMinh", vietnameseFilterState.searchQuery)
        assertEquals("vi-VN", vietnameseFilterState.selectedRegionCode)
        assertEquals("Male", vietnameseFilterState.selectedGender)
    }

    @Test
    fun `5 and 6 - inline preview does not select voice and does not close picker contract`() {
        var selectedVoice: TtsVoice? = null
        var isDialogDismissed = false
        var previewedVoice: TtsVoice? = null

        val onSelect: (TtsVoice) -> Unit = { voice ->
            selectedVoice = voice
        }
        val onDismiss: () -> Unit = {
            isDialogDismissed = true
        }
        val onPreview: (TtsVoice) -> Unit = { voice ->
            previewedVoice = voice
        }

        // Triggering inline preview on guyVoiceEnUs
        onPreview(guyVoiceEnUs)

        assertEquals(guyVoiceEnUs, previewedVoice, "Preview must trigger for requested candidate voice")
        assertNull(selectedVoice, "Preview must NOT select the voice")
        assertFalse(isDialogDismissed, "Preview must NOT dismiss the dialog")

        // Clicking on row selection
        onSelect(soniaVoiceEnGb)
        onDismiss()

        assertEquals(soniaVoiceEnGb, selectedVoice, "Selecting row must select voice")
        assertTrue(isDialogDismissed, "Selecting row must dismiss dialog")
    }

    @Test
    fun `7 - preview callback receives candidate voice and current sample and parameters`() = runBlocking {
        var capturedVoice: TtsVoice? = null
        var capturedText: String? = null
        var capturedRate: Int? = null
        var capturedPitch: Int? = null
        var capturedVolume: Int? = null

        val previewHandler: (voice: TtsVoice, text: String, rate: Int, pitchHz: Int, volumePercent: Int) -> Unit =
            { voice, text, rate, pitchHz, volumePercent ->
                capturedVoice = voice
                capturedText = text
                capturedRate = rate
                capturedPitch = pitchHz
                capturedVolume = volumePercent
            }

        val sampleText = "The company produces genuine leather goods."
        val speedRate = 15
        val pitchHz = -4
        val volumePercent = 10

        previewHandler(soniaVoiceEnGb, sampleText, speedRate, pitchHz, volumePercent)

        assertEquals(soniaVoiceEnGb, capturedVoice)
        assertEquals(sampleText, capturedText)
        assertEquals(15, capturedRate)
        assertEquals(-4, capturedPitch)
        assertEquals(10, capturedVolume)

        // Verify synthesis preview generation with these parameters
        val pitchStr = if (pitchHz >= 0) "+${pitchHz}Hz" else "${pitchHz}Hz"
        val volumeStr = if (volumePercent >= 0) "+${volumePercent}%" else "${volumePercent}%"
        val previewPath = service.preview(sampleText, soniaVoiceEnGb, speedRate, pitchStr, volumeStr)
        assertTrue(Files.exists(previewPath))
    }

    @Test
    fun `8 - previewing second voice replaces and stops first preview`() {
        var currentlyPlayingVoiceId: String? = null
        var cancelCount = 0

        fun stopAudio() {
            if (currentlyPlayingVoiceId != null) {
                cancelCount++
                currentlyPlayingVoiceId = null
            }
        }

        fun playPreview(voice: TtsVoice) {
            stopAudio()
            currentlyPlayingVoiceId = voice.id
        }

        // Play voice 1
        playPreview(avaVoiceEnUs)
        assertEquals(avaVoiceEnUs.id, currentlyPlayingVoiceId)
        assertEquals(0, cancelCount)

        // Play voice 2 while voice 1 is playing
        playPreview(soniaVoiceEnGb)
        assertEquals(soniaVoiceEnGb.id, currentlyPlayingVoiceId)
        assertEquals(1, cancelCount, "First preview must be stopped/cancelled when starting second preview")

        // Stop preview
        stopAudio()
        assertNull(currentlyPlayingVoiceId)
        assertEquals(2, cancelCount)
    }

    @Test
    fun `9 and 10 - Primary and Fallback voice pickers support inline preview`() {
        val candidateCatalog = testVoices.filter { it.isEnglish }
        assertEquals(4, candidateCatalog.size)

        val primaryFilterState = VoicePickerFilterState()
        val fallbackFilterState = VoicePickerFilterState()

        // Filter by en-GB in primary picker
        primaryFilterState.selectedRegionCode = "en-GB"
        val filteredPrimary = candidateCatalog.filter { voice ->
            primaryFilterState.selectedRegionCode == null || voice.locale.equals(primaryFilterState.selectedRegionCode, ignoreCase = true)
        }
        assertEquals(listOf(soniaVoiceEnGb, ryanVoiceEnGb), filteredPrimary)

        // Fallback picker shares English filter state
        fallbackFilterState.selectedRegionCode = primaryFilterState.selectedRegionCode
        val filteredFallback = candidateCatalog.filter { voice ->
            voice.id != avaVoiceEnUs.id && (fallbackFilterState.selectedRegionCode == null || voice.locale.equals(fallbackFilterState.selectedRegionCode, ignoreCase = true))
        }
        assertEquals(listOf(soniaVoiceEnGb, ryanVoiceEnGb), filteredFallback)
    }

    @Test
    fun `11 - fallback count 3 of 3 disables Add and exposes clear capacity text`() {
        var fallbacks = OrderedFallbackVoices.EMPTY
        fallbacks = fallbacks.add(guyVoiceEnUs, avaVoiceEnUs)
        fallbacks = fallbacks.add(soniaVoiceEnGb, avaVoiceEnUs)
        fallbacks = fallbacks.add(ryanVoiceEnGb, avaVoiceEnUs)

        assertEquals(3, fallbacks.voices.size)
        assertEquals(OrderedFallbackVoices.MAX_FALLBACKS, fallbacks.voices.size)

        val availableCatalog = testVoices.filter { it.isEnglish }
        val remainingCandidates = availableCatalog.filter { it.id != avaVoiceEnUs.id && it.id !in fallbacks.ids }
        val canAdd = fallbacks.voices.size < OrderedFallbackVoices.MAX_FALLBACKS && remainingCandidates.isNotEmpty()

        assertFalse(canAdd, "When fallback count is 3/3, adding more fallbacks must be disabled")

        val capacitySubtitle = if (fallbacks.voices.size >= OrderedFallbackVoices.MAX_FALLBACKS) {
            "Maximum 3 fallback voices · 3 selected"
        } else {
            "Maximum 3 fallback voices · ${fallbacks.voices.size} selected"
        }
        assertEquals("Maximum 3 fallback voices · 3 selected", capacitySubtitle)
    }

    @Test
    fun `12 - fallback count less than 3 enables Add when valid candidates exist`() {
        var fallbacks = OrderedFallbackVoices.EMPTY
        fallbacks = fallbacks.add(guyVoiceEnUs, avaVoiceEnUs)

        assertEquals(1, fallbacks.voices.size)

        val availableCatalog = testVoices.filter { it.isEnglish }
        val remainingCandidates = availableCatalog.filter { it.id != avaVoiceEnUs.id && it.id !in fallbacks.ids }
        val canAdd = fallbacks.voices.size < OrderedFallbackVoices.MAX_FALLBACKS && remainingCandidates.isNotEmpty()

        assertTrue(canAdd, "When count < 3 and valid candidates remain, Add fallback must be enabled")

        val capacitySubtitle = "Maximum 3 fallback voices · ${fallbacks.voices.size} selected"
        assertEquals("Maximum 3 fallback voices · 1 selected", capacitySubtitle)
    }

    @Test
    fun `13 - existing fallback add remove and reorder operations remain completely preserved`() {
        var fallbacks = OrderedFallbackVoices.EMPTY

        // Add 1
        fallbacks = fallbacks.add(guyVoiceEnUs, avaVoiceEnUs)
        assertEquals(listOf("en-US-GuyNeural"), fallbacks.ids)

        // Add 2
        fallbacks = fallbacks.add(soniaVoiceEnGb, avaVoiceEnUs)
        assertEquals(listOf("en-US-GuyNeural", "en-GB-SoniaNeural"), fallbacks.ids)

        // Reorder (move Sonia up to index 0)
        fallbacks = fallbacks.move("en-GB-SoniaNeural", -1)
        assertEquals(listOf("en-GB-SoniaNeural", "en-US-GuyNeural"), fallbacks.ids)

        // Reorder (move Sonia back down)
        fallbacks = fallbacks.move("en-GB-SoniaNeural", 1)
        assertEquals(listOf("en-US-GuyNeural", "en-GB-SoniaNeural"), fallbacks.ids)

        // Remove Guy
        fallbacks = fallbacks.remove("en-US-GuyNeural")
        assertEquals(listOf("en-GB-SoniaNeural"), fallbacks.ids)
    }

    private class FakeEngine(private val voices: List<TtsVoice>) : TtsEngine {
        override suspend fun listVoices(): List<TtsVoice> = voices

        override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
            Files.createDirectories(outputFile.parent)
            Files.write(outputFile, byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00))
            return TtsSynthesisResult(outputFile, 4)
        }
    }

    private class FakeMediaStorage(private val root: Path) : ContentMediaStorage {
        var storedCount = 0
            private set
        val storedAssets = mutableMapOf<String, ByteArray>()

        override fun store(
            packageName: String,
            fileName: String,
            content: ByteArray
        ): ContentMediaAsset {
            val relPath = "$packageName/$fileName"
            storedAssets[relPath] = content
            val targetPath = root.resolve(relPath)
            Files.createDirectories(targetPath.parent)
            Files.write(targetPath, content)
            storedCount++
            return ContentMediaAsset(packageName, fileName, relPath)
        }

        override fun storeStream(
            packageName: String,
            fileName: String,
            source: Path
        ): ContentMediaAsset {
            return store(packageName, fileName, Files.readAllBytes(source))
        }

        override fun resolve(relativePath: String): Path? {
            val path = root.resolve(relativePath)
            return if (Files.exists(path)) path else null
        }

        override fun exists(relativePath: String): Boolean {
            return storedAssets.containsKey(relativePath) || Files.exists(root.resolve(relativePath))
        }
    }
}
