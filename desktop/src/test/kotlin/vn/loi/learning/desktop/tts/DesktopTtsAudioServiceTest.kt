package vn.loi.learning.desktop.tts

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopTtsAudioServiceTest {

    private lateinit var tempMediaDir: Path
    private lateinit var tempPreviewDir: Path
    private lateinit var fakeMediaStorage: FakeContentMediaStorage
    private lateinit var fakeEngine: FakeTtsEngine
    private lateinit var previewStore: TtsPreviewStore
    private lateinit var service: DesktopTtsAudioService

    private val fixedClock = Clock.fixed(Instant.parse("2026-08-22T04:44:23.184Z"), ZoneOffset.UTC)
    private val namer = TtsAudioFileNamer(fixedClock)

    private val enVoice = TtsVoice(
        id = "en-US-AvaMultilingualNeural",
        displayName = "Microsoft Ava",
        locale = "en-US",
        language = "en",
        gender = "Female"
    )

    private val viVoice = TtsVoice(
        id = "vi-VN-HoaiMyNeural",
        displayName = "Microsoft HoaiMy",
        locale = "vi-VN",
        language = "vi",
        gender = "Female"
    )

    @BeforeTest
    fun setup() {
        tempMediaDir = Files.createTempDirectory("tts-service-media-test-")
        tempPreviewDir = Files.createTempDirectory("tts-service-preview-test-")
        fakeMediaStorage = FakeContentMediaStorage(tempMediaDir)
        fakeEngine = FakeTtsEngine(listOf(enVoice, viVoice))
        previewStore = TtsPreviewStore(tempPreviewDir)
        service = DesktopTtsAudioService(
            ttsEngine = fakeEngine,
            mediaStorage = fakeMediaStorage,
            fileNamer = namer,
            previewStore = previewStore
        )
    }

    @AfterTest
    fun tearDown() {
        service.close()
        tempMediaDir.toFile().deleteRecursively()
        tempPreviewDir.toFile().deleteRecursively()
    }

    @Test
    fun `listVoices and language filter tests`() = runBlocking {
        val allVoices = service.listVoices()
        assertEquals(2, allVoices.size)

        val enVoices = service.listEnglishVoices()
        assertEquals(1, enVoices.size)
        assertEquals("en-US-AvaMultilingualNeural", enVoices.first().id)

        val viVoices = service.listVietnameseVoices()
        assertEquals(1, viVoices.size)
        assertEquals("vi-VN-HoaiMyNeural", viVoices.first().id)
    }

    @Test
    fun `defaultLanguageFor maps logical fields correctly`() {
        assertEquals("en", service.defaultLanguageFor(TtsField.QUESTION))
        assertEquals("vi", service.defaultLanguageFor(TtsField.ANSWER))
        assertEquals("en", service.defaultLanguageFor(TtsField.EXAMPLE))
        assertEquals("vi", service.defaultLanguageFor(TtsField.TRANSLATION))
    }

    @Test
    fun `defaultVoiceFor selects appropriate voice`() {
        val voices = listOf(enVoice, viVoice)
        val selectedEn = service.defaultVoiceFor("en", voices)
        assertEquals("en-US-AvaMultilingualNeural", selectedEn?.id)

        val selectedVi = service.defaultVoiceFor("vi", voices)
        assertEquals("vi-VN-HoaiMyNeural", selectedVi?.id)
    }

    @Test
    fun `preview generates temporary file without calling permanent media storage`() = runBlocking {
        val previewPath = service.preview(
            text = "Hello world",
            voice = enVoice,
            rate = 0
        )

        assertTrue(Files.exists(previewPath))
        assertTrue(Files.size(previewPath) > 0)
        assertTrue(previewPath.startsWith(tempPreviewDir))
        assertEquals(0, fakeMediaStorage.storedAssets.size, "Preview must not write to permanent storage")
    }

    @Test
    fun `preview and permanent generation preserve rate pitch and volume`() = runBlocking {
        service.preview("Exact selected text", enVoice, 25, "+8Hz", "-5%")
        assertEquals(TtsSynthesisRequest("Exact selected text", enVoice, 25, "+8Hz", "-5%"), fakeEngine.lastRequest)

        service.generatePermanentAudio(
            contentId = "c1", packageName = "pkg", field = TtsField.QUESTION,
            text = "Generated text", voice = enVoice, rate = -10, pitch = "-4Hz", volume = "+10%"
        )
        assertEquals(TtsSynthesisRequest("Generated text", enVoice, -10, "-4Hz", "+10%"), fakeEngine.lastRequest)
    }

    @Test
    fun `preview rejects blank text`() = runBlocking {
        val ex = assertFailsWith<TtsException> {
            service.preview("   ", enVoice)
        }
        assertTrue(ex.error is TtsError.InvalidText)
    }

    @Test
    fun `generatePermanentAudio stores in ContentMediaStorage with deterministic name`() = runBlocking {
        val asset = service.generatePermanentAudio(
            contentId = "1842",
            packageName = "Vocabulary_in_Use",
            field = TtsField.QUESTION,
            text = "She's learning \"English\" & practicing every day.",
            voice = enVoice,
            rate = 0
        )

        assertEquals("Vocabulary_in_Use", asset.packageName)
        assertEquals("1842_question_en_20260822_04-44-23-184.mp3", asset.fileName)
        assertEquals("Vocabulary_in_Use/1842_question_en_20260822_04-44-23-184.mp3", asset.relativePath)

        assertTrue(fakeMediaStorage.exists(asset.relativePath))
        val resolved = fakeMediaStorage.resolve(asset.relativePath)
        assertTrue(resolved != null && Files.exists(resolved))
        assertTrue(Files.size(resolved) > 0)
    }

    @Test
    fun `generatePermanentAudio with Vietnamese text and translation field`() = runBlocking {
        val asset = service.generatePermanentAudio(
            contentId = "1842",
            packageName = "Vocabulary_in_Use",
            field = TtsField.TRANSLATION,
            text = "Cô ấy đang học tiếng Anh và luyện tập mỗi ngày.",
            voice = viVoice,
            rate = 0
        )

        assertEquals("Vocabulary_in_Use", asset.packageName)
        assertEquals("1842_translation_vi_20260822_04-44-23-184.mp3", asset.fileName)
        assertEquals("Vocabulary_in_Use/1842_translation_vi_20260822_04-44-23-184.mp3", asset.relativePath)
        assertTrue(fakeMediaStorage.exists(asset.relativePath))
    }

    @Test
    fun `generatePermanentAudio handles collision safely`() = runBlocking {
        // Pre-store an existing file
        fakeMediaStorage.store(
            packageName = "Vocabulary_in_Use",
            fileName = "1842_question_en_20260822_04-44-23-184.mp3",
            content = byteArrayOf(1, 2, 3)
        )

        val asset = service.generatePermanentAudio(
            contentId = "1842",
            packageName = "Vocabulary_in_Use",
            field = TtsField.QUESTION,
            text = "Question text",
            voice = enVoice
        )

        assertEquals("1842_question_en_20260822_04-44-23-184_2.mp3", asset.fileName)
        assertTrue(fakeMediaStorage.exists(asset.relativePath))
    }

    @Test
    fun `generatePermanentAudio fails cleanly on engine error and cleans temp files`() = runBlocking {
        fakeEngine.failNextSynthesis = true

        val ex = assertFailsWith<TtsException> {
            service.generatePermanentAudio(
                contentId = "1842",
                packageName = "Vocabulary_in_Use",
                field = TtsField.QUESTION,
                text = "Fail test",
                voice = enVoice
            )
        }

        assertTrue(ex.error is TtsError.GenerationFailed)
        assertFalse(fakeMediaStorage.exists("Vocabulary_in_Use/1842_question_en_20260822_04-44-23-184.mp3"))
    }
}

class FakeTtsEngine(
    private val voices: List<TtsVoice> = emptyList()
) : TtsEngine {
    var failNextSynthesis = false
    var lastRequest: TtsSynthesisRequest? = null

    override suspend fun listVoices(): List<TtsVoice> = voices

    override suspend fun synthesize(
        request: TtsSynthesisRequest,
        outputFile: Path
    ): TtsSynthesisResult {
        lastRequest = request
        if (request.text.isBlank()) throw TtsException(TtsError.InvalidText("Blank text"))
        if (failNextSynthesis) {
            throw TtsException(TtsError.GenerationFailed("Simulated provider failure"))
        }

        Files.createDirectories(outputFile.parent)
        val dummyMp3Bytes = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x44.toByte()) +
            request.text.toByteArray()
        Files.write(outputFile, dummyMp3Bytes)

        return TtsSynthesisResult(outputFile, dummyMp3Bytes.size.toLong())
    }
}

class FakeContentMediaStorage(
    private val rootDir: Path
) : ContentMediaStorage {
    val storedAssets = mutableMapOf<String, ByteArray>()

    override fun store(
        packageName: String,
        fileName: String,
        content: ByteArray
    ): ContentMediaAsset {
        val relPath = "$packageName/$fileName"
        storedAssets[relPath] = content
        val targetPath = rootDir.resolve(relPath)
        Files.createDirectories(targetPath.parent)
        Files.write(targetPath, content)
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
        val path = rootDir.resolve(relativePath)
        return if (Files.exists(path)) path else null
    }

    override fun exists(relativePath: String): Boolean {
        return storedAssets.containsKey(relativePath) || Files.exists(rootDir.resolve(relativePath))
    }
}
