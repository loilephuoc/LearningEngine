package vn.loi.learning.desktop.tts

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EdgeTtsEngineTest {

    private lateinit var tempDir: java.nio.file.Path
    private lateinit var engine: EdgeTtsEngine

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("edge-tts-engine-test-")
        engine = EdgeTtsEngine()
    }

    @AfterTest
    fun tearDown() {
        if (Files.exists(tempDir)) {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `synthesize rejects blank or whitespace text`() = runBlocking {
        val targetPath = tempDir.resolve("out.mp3")
        val voice = TtsVoice("vi-VN-HoaiMyNeural", "HoaiMy", "vi-VN", "vi")

        val ex1 = assertFailsWith<TtsException> {
            engine.synthesize(
                TtsSynthesisRequest(
                    text = "",
                    voice = voice
                ),
                targetPath
            )
        }
        assertTrue(ex1.error is TtsError.InvalidText)

        val ex2 = assertFailsWith<TtsException> {
            engine.synthesize(
                TtsSynthesisRequest(
                    text = "   \n\t  ",
                    voice = voice
                ),
                targetPath
            )
        }
        assertTrue(ex2.error is TtsError.InvalidText)
    }

    @Test
    fun `synthesize rejects blank voice id`() = runBlocking {
        val targetPath = tempDir.resolve("out.mp3")
        val blankVoice = TtsVoice("", "", "", "")

        val ex = assertFailsWith<TtsException> {
            engine.synthesize(
                TtsSynthesisRequest(
                    text = "Valid text",
                    voice = blankVoice
                ),
                targetPath
            )
        }
        assertTrue(ex.error is TtsError.VoiceUnavailable)
    }

    @Test
    fun `optional live smoke test for English and Vietnamese`() = runBlocking {
        val viVoice = TtsVoice("vi-VN-HoaiMyNeural", "Microsoft HoaiMy", "vi-VN", "vi")
        val enVoice = TtsVoice("en-US-AvaMultilingualNeural", "Microsoft Ava", "en-US", "en")

        val viOut = tempDir.resolve("live_vi.mp3")
        val enOut = tempDir.resolve("live_en.mp3")

        val viResult = runCatching {
            engine.synthesize(
                TtsSynthesisRequest(
                    text = "Cô ấy làm việc cho một công ty phần mềm.",
                    voice = viVoice
                ),
                viOut
            )
        }

        if (viResult.isSuccess) {
            val res = viResult.getOrThrow()
            assertTrue(Files.exists(res.outputFile))
            assertTrue(res.byteCount > 0L)
            println("[Live Smoke] Vietnamese synthesis PASSED: ${res.byteCount} bytes")
        } else {
            println("[Live Smoke] Vietnamese synthesis skipped/unavailable: ${viResult.exceptionOrNull()?.message}")
        }

        val enResult = runCatching {
            engine.synthesize(
                TtsSynthesisRequest(
                    text = "genuine",
                    voice = enVoice
                ),
                enOut
            )
        }

        if (enResult.isSuccess) {
            val res = enResult.getOrThrow()
            assertTrue(Files.exists(res.outputFile))
            assertTrue(res.byteCount > 0L)
            println("[Live Smoke] English synthesis PASSED: ${res.byteCount} bytes")
        } else {
            println("[Live Smoke] English synthesis skipped/unavailable: ${enResult.exceptionOrNull()?.message}")
        }
    }
}
