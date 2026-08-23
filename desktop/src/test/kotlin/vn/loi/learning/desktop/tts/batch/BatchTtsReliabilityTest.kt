package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.*

class BatchTtsReliabilityTest {
    private val en = TtsVoice("en-1", "English", "en-US", "en")
    private val enFallback = TtsVoice("en-2", "English fallback", "en-GB", "en")
    private val vi = TtsVoice("vi-1", "Vietnamese", "vi-VN", "vi")

    @Test
    fun `timeout retries are bounded and next target still completes`() = runTest {
        val root = Files.createTempDirectory("tts-reliability")
        try {
            val engine = object : TtsEngine {
                var calls = 0
                override suspend fun listVoices() = listOf(en)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    calls++
                    if (request.text == "hang") awaitCancellation()
                    Files.write(outputFile, byteArrayOf(1, 2, 3))
                    return TtsSynthesisResult(outputFile, 3)
                }
            }
            val runner = runner(root, engine, BatchTtsExecutionPolicy(500, 2, 0, 0, 2))
            var final: BatchTtsSummary? = null
            runner.runBatch(
                listOf(job("one", "hang", en), job("two", "works", en)), "pkg",
                onProgress = { final = it }
            ).join()

            assertEquals(3, engine.calls)
            assertEquals(2, final?.completedJobs)
            assertEquals(1, final?.failedCount)
            assertEquals(1, final?.successCount)
            assertTrue(final?.isFinished == true)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `transient failure retries then succeeds without fallback`() = runTest {
        val root = Files.createTempDirectory("tts-retry")
        try {
            val engine = object : TtsEngine {
                var calls = 0
                override suspend fun listVoices() = listOf(en)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    calls++
                    if (calls == 1) throw TtsException(TtsError.ProviderUnavailable("temporary"))
                    Files.write(outputFile, byteArrayOf(1))
                    return TtsSynthesisResult(outputFile, 1)
                }
            }
            var final: BatchTtsSummary? = null
            runner(root, engine, BatchTtsExecutionPolicy(100, 2, 0, 0, 2))
                .runBatch(listOf(job("one", "works", en)), "pkg", onProgress = { final = it }).join()
            assertEquals(2, engine.calls)
            assertEquals(1, final?.successCount)
            assertFalse(final!!.jobResults.single().recoveredViaFallback)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `cross language voice is excluded from fallback`() = runTest {
        val root = Files.createTempDirectory("tts-language")
        try {
            val attempted = mutableListOf<String>()
            val engine = object : TtsEngine {
                override suspend fun listVoices() = listOf(en, vi)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    attempted += request.voice.id
                    throw TtsException(TtsError.VoiceUnavailable(request.voice.id))
                }
            }
            val configured = job("one", "text", en).copy(candidateVoices = listOf(en, vi, enFallback))
            runner(root, engine, BatchTtsExecutionPolicy(100, 1, 0, 0, 4))
                .runBatch(listOf(configured), "pkg", onProgress = {}).join()
            assertEquals(listOf(en.id, enFallback.id), attempted)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `hundreds of targets with distributed failures reach terminal total`() = runTest {
        val root = Files.createTempDirectory("tts-stress")
        try {
            val engine = object : TtsEngine {
                override suspend fun listVoices() = listOf(en)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    if (request.text.endsWith("0")) throw TtsException(TtsError.InvalidText("fixture"))
                    Files.write(outputFile, byteArrayOf(1))
                    return TtsSynthesisResult(outputFile, 1)
                }
            }
            val jobs = (1..300).map { job("id-$it", "text-$it", en) }
            var final: BatchTtsSummary? = null
            runner(root, engine, BatchTtsExecutionPolicy(1_000, 1, 0, 0, 1))
                .runBatch(jobs, "pkg", onProgress = { final = it }).join()
            assertEquals(300, final?.completedJobs)
            assertEquals(30, final?.failedCount)
            assertEquals(270, final?.successCount)
        } finally { root.toFile().deleteRecursively() }
    }

    private fun job(id: String, text: String, voice: TtsVoice) = BatchTtsJob(
        id, TtsField.QUESTION, text, TtsLanguage.ENGLISH, voice, id = id
    )

    private fun runner(root: Path, engine: TtsEngine, policy: BatchTtsExecutionPolicy): BatchTtsRunner =
        BatchTtsRunner(
            DesktopTtsAudioService(engine, Storage(root), previewStore = TtsPreviewStore(root)),
            policy = policy
        )

    private class Storage(private val root: Path) : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset {
            val relative = "$packageName/$fileName"
            val target = root.resolve(relative)
            Files.createDirectories(target.parent)
            Files.write(target, content)
            return ContentMediaAsset(packageName, fileName, relative)
        }
        override fun storeStream(packageName: String, fileName: String, source: Path) = store(packageName, fileName, Files.readAllBytes(source))
        override fun resolve(relativePath: String): Path? = root.resolve(relativePath).takeIf(Files::exists)
        override fun exists(relativePath: String) = Files.exists(root.resolve(relativePath))
    }
}
