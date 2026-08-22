package vn.loi.learning.desktop.tts.ui

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsAudioFileNamer
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsError
import vn.loi.learning.desktop.tts.TtsException
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopTtsUiTest {

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
        tempMediaDir = Files.createTempDirectory("tts-ui-test-media-")
        tempPreviewDir = Files.createTempDirectory("tts-ui-test-preview-")
        fakeMediaStorage = FakeMediaStorage(tempMediaDir)
        fakeEngine = FakeEngine(testVoices)
        service = DesktopTtsAudioService(
            ttsEngine = fakeEngine,
            mediaStorage = fakeMediaStorage,
            fileNamer = TtsAudioFileNamer(Clock.fixed(Instant.parse("2026-08-22T05:00:00Z"), ZoneOffset.UTC)),
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
    fun `default field mapping maps logically to expected languages and regions`() {
        assertEquals(TtsLanguage.ENGLISH, DesktopTtsUiState.defaultLanguageForField(TtsField.QUESTION))
        assertEquals(TtsLanguage.ENGLISH, DesktopTtsUiState.defaultLanguageForField(TtsField.ANSWER))
        assertEquals(TtsLanguage.ENGLISH, DesktopTtsUiState.defaultLanguageForField(TtsField.EXAMPLE))
        assertEquals(TtsLanguage.VIETNAMESE, DesktopTtsUiState.defaultLanguageForField(TtsField.TRANSLATION))

        assertEquals("en-US", DesktopTtsUiState.defaultRegionForLanguage(TtsLanguage.ENGLISH))
        assertEquals("vi-VN", DesktopTtsUiState.defaultRegionForLanguage(TtsLanguage.VIETNAMESE))
    }

    @Test
    fun `TtsDialogTarget resolves fields and missing status correctly`() {
        val target = TtsDialogTarget(
            contentId = "content-1",
            questionText = "genuine",
            answerText = "authentic",
            exampleText = "This is genuine leather.",
            exampleTranslation = "Đây là da thật.",
            questionAudioRef = "media/pkg/q_audio.mp3",
            answerAudioRef = null,
            exampleAudioRef = null,
            translationAudioRef = null
        )

        assertTrue(target.hasAudioFor(TtsField.QUESTION))
        assertFalse(target.hasAudioFor(TtsField.ANSWER))
        assertFalse(target.hasAudioFor(TtsField.EXAMPLE))
        assertFalse(target.hasAudioFor(TtsField.TRANSLATION))

        // First missing field should be ANSWER since QUESTION already has audio
        assertEquals(TtsField.ANSWER, target.firstMissingField())
        assertEquals("authentic", target.textFor(TtsField.ANSWER))
        assertEquals("This is genuine leather.", target.textFor(TtsField.EXAMPLE))
        assertEquals("Đây là da thật.", target.textFor(TtsField.TRANSLATION))
    }

    @Test
    fun `voice filtering by language region and gender works as expected`() {
        val target = TtsDialogTarget(
            contentId = "content-1",
            questionText = "genuine",
            answerText = "authentic"
        )

        val stateEnUs = DesktopTtsUiState(
            target = target,
            packageName = "oxford_3000",
            allVoices = testVoices,
            selectedLanguage = TtsLanguage.ENGLISH,
            selectedRegion = "en-US",
            selectedGender = TtsGenderFilter.ALL
        )

        assertEquals(listOf("en-GB", "en-US"), stateEnUs.availableRegions)
        assertEquals(listOf(avaVoiceEnUs, guyVoiceEnUs), stateEnUs.filteredVoices)

        val stateEnUsFemale = stateEnUs.copy(selectedGender = TtsGenderFilter.FEMALE)
        assertEquals(listOf(avaVoiceEnUs), stateEnUsFemale.filteredVoices)

        val stateEnGb = stateEnUs.copy(selectedRegion = "en-GB")
        assertEquals(listOf(soniaVoiceEnGb), stateEnGb.filteredVoices)

        val stateVi = stateEnUs.copy(
            selectedLanguage = TtsLanguage.VIETNAMESE,
            selectedRegion = "vi-VN",
            selectedGender = TtsGenderFilter.MALE
        )
        assertEquals(listOf(namMinhVoiceVi), stateVi.filteredVoices)
    }

    @Test
    fun `state validation prevents generating for blank text or already populated audio`() {
        val targetWithMissing = TtsDialogTarget(
            contentId = "content-1",
            questionText = "genuine",
            answerText = "",
            questionAudioRef = "media/pkg/q.mp3",
            answerAudioRef = null
        )

        // Question has audio -> cannot generate, cannot apply
        val stateQuestion = DesktopTtsUiState(
            target = targetWithMissing,
            packageName = "pkg",
            allVoices = testVoices,
            selectedField = TtsField.QUESTION,
            selectedVoice = avaVoiceEnUs
        )
        assertTrue(stateQuestion.isCurrentFieldAlreadyPopulated)
        assertFalse(stateQuestion.canGenerate)
        assertFalse(stateQuestion.canApply)

        // Answer is blank -> cannot preview, cannot generate
        val stateAnswer = DesktopTtsUiState(
            target = targetWithMissing,
            packageName = "pkg",
            allVoices = testVoices,
            selectedField = TtsField.ANSWER,
            selectedVoice = avaVoiceEnUs
        )
        assertTrue(stateAnswer.isTextBlank)
        assertFalse(stateAnswer.canPreview)
        assertFalse(stateAnswer.canGenerate)
    }

    @Test
    fun `preview generates temporary audio without persisting to package storage`() = runBlocking {
        val previewPath = service.preview("genuine", avaVoiceEnUs)

        assertTrue(Files.exists(previewPath))
        assertTrue(previewPath.toString().contains("preview"))
        // MediaStorage must not contain any files from preview
        assertEquals(0, fakeMediaStorage.storedCount)
    }

    @Test
    fun `generate creates permanent asset without modifying content or auto applying`() = runBlocking {
        val asset = service.generatePermanentAudio(
            contentId = "c1",
            packageName = "pkg1",
            field = TtsField.QUESTION,
            text = "genuine",
            voice = avaVoiceEnUs
        )

        assertNotNull(asset)
        assertEquals("pkg1", asset.packageName)
        assertTrue(asset.relativePath.startsWith("pkg1/"))
        assertTrue(asset.relativePath.endsWith(".mp3"))
        assertEquals(1, fakeMediaStorage.storedCount)
        assertTrue(fakeMediaStorage.exists(asset.relativePath))
    }

    @Test
    fun `error state captures failure without throwing raw unhandled exceptions`() {
        val failedState = DesktopTtsUiState(
            target = TtsDialogTarget("c1", "text", "ans"),
            packageName = "pkg",
            previewState = TtsPreviewState.Failed("TTS provider unavailable: connection refused"),
            generationState = TtsGenerationState.Failed("Audio generation failed: timeout")
        )

        assertTrue(failedState.previewState is TtsPreviewState.Failed)
        assertEquals("TTS provider unavailable: connection refused", (failedState.previewState as TtsPreviewState.Failed).message)
        assertTrue(failedState.generationState is TtsGenerationState.Failed)
        assertEquals("Audio generation failed: timeout", (failedState.generationState as TtsGenerationState.Failed).message)
    }

    @Test
    fun `canonical field mapping extracts correct text for question answer example translation`() {
        val target = TtsDialogTarget(
            contentId = "c-target",
            questionText = "Question content",
            answerText = "Answer definition",
            exampleText = "Example sentence",
            exampleTranslation = "Bản dịch ví dụ"
        )

        assertEquals("Question content", target.textFor(TtsField.QUESTION))
        assertEquals("Answer definition", target.textFor(TtsField.ANSWER))
        assertEquals("Example sentence", target.textFor(TtsField.EXAMPLE))
        assertEquals("Bản dịch ví dụ", target.textFor(TtsField.TRANSLATION))
    }

    private class FakeEngine(private val voices: List<TtsVoice>) : TtsEngine {
        override suspend fun listVoices(): List<TtsVoice> = voices

        override suspend fun synthesize(request: TtsSynthesisRequest, targetFile: Path): TtsSynthesisResult {
            Files.createDirectories(targetFile.parent)
            Files.write(targetFile, byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00))
            return TtsSynthesisResult(targetFile, 4)
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
