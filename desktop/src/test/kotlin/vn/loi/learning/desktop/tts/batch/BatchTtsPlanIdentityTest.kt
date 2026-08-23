package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.*

class BatchTtsPlanIdentityTest {
    private val primary = voice("primary")
    private val fallback1 = voice("fallback-1")
    private val fallback2 = voice("fallback-2")

    @Test
    fun `same exact generation plan has stable identity`() {
        assertEquals(plan(listOf(job("one"))).id, plan(listOf(job("one"))).id)
    }

    @Test
    fun `same package and fields with different content scope do not collide`() {
        assertNotEquals(plan(listOf(job("one"))).id, plan(listOf(job("two"))).id)
    }

    @Test
    fun `overwrite mode voice order and synthesis settings affect identity`() {
        val baseline = plan(listOf(job("one")))
        assertNotEquals(baseline.id, plan(listOf(job("one")), overwrite = true).id)
        assertNotEquals(baseline.id, plan(listOf(job("one").copy(voice = fallback1, candidateVoices = listOf(fallback1)))).id)
        assertNotEquals(
            plan(listOf(job("one").copy(candidateVoices = listOf(primary, fallback1, fallback2)))).id,
            plan(listOf(job("one").copy(candidateVoices = listOf(primary, fallback2, fallback1)))).id
        )
        assertNotEquals(baseline.id, plan(listOf(job("one").copy(rate = 5))).id)
        assertNotEquals(baseline.id, plan(listOf(job("one").copy(pitch = "+2Hz"))).id)
        assertNotEquals(baseline.id, plan(listOf(job("one").copy(volume = "-3%"))).id)
    }

    @Test
    fun `different plans coexist and discarding one leaves the other intact`() {
        val root = Files.createTempDirectory("tts-plan-repo")
        try {
            val repository = BatchTtsCheckpointRepository(root)
            val first = plan(listOf(job("one")))
            val second = plan(listOf(job("two")))
            repository.storeFor(first).save(checkpoint(first))
            repository.storeFor(second).save(checkpoint(second))
            assertNotEquals(repository.checkpointPath(first), repository.checkpointPath(second))
            assertTrue(repository.discard(first))
            assertNull(repository.storeFor(first).load())
            assertEquals(second.id, repository.storeFor(second).load()?.batchId)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `same plan rejects concurrent writer while different plan can acquire`() {
        val root = Files.createTempDirectory("tts-plan-lock")
        try {
            val repository = BatchTtsCheckpointRepository(root)
            val first = plan(listOf(job("one")))
            val second = plan(listOf(job("two")))
            repository.acquire(first).use {
                assertFailsWith<IllegalStateException> { repository.acquire(first) }
                repository.acquire(second).close()
            }
            repository.acquire(first).close()
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `stale lease is recovered conservatively`() {
        val root = Files.createTempDirectory("tts-stale-lock")
        try {
            val repository = BatchTtsCheckpointRepository(root, Duration.ZERO)
            val target = plan(listOf(job("one")))
            val first = repository.acquire(target)
            // Simulate process death: close the channel but leave the ownership marker.
            val lock = repository.checkpointPath(target).resolveSibling("${target.id}.lock")
            first.close()
            Files.writeString(lock, "stale")
            Files.setLastModifiedTime(lock, java.nio.file.attribute.FileTime.fromMillis(0))
            repository.acquire(target).close()
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `corrupt checkpoint does not damage another plan`() {
        val root = Files.createTempDirectory("tts-plan-corrupt")
        try {
            val repository = BatchTtsCheckpointRepository(root)
            val corrupt = plan(listOf(job("one")))
            val valid = plan(listOf(job("two")))
            Files.createDirectories(repository.checkpointPath(corrupt).parent)
            Files.writeString(repository.checkpointPath(corrupt), "bad-json")
            repository.storeFor(valid).save(checkpoint(valid))
            assertNull(repository.storeFor(corrupt).load())
            assertEquals(valid.id, repository.storeFor(valid).load()?.batchId)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `matching success resumes only when asset exists and completion clears exact checkpoint`() = runTest {
        val root = Files.createTempDirectory("tts-plan-resume")
        try {
            val jobs = listOf(job("one"), job("two"))
            val plan = plan(jobs)
            val unrelated = plan(listOf(job("other")))
            val repository = BatchTtsCheckpointRepository(root.resolve("checkpoints"))
            repository.storeFor(plan).save(
                BatchTtsCheckpoint(plan.id, "pkg", false, listOf(
                    BatchTtsCheckpointRecord(jobs[0].id, jobs[0].textFingerprint(), "SUCCESS", "pkg/existing.mp3")
                ))
            )
            repository.storeFor(unrelated).save(checkpoint(unrelated))
            val engine = CountingEngine()
            var final: BatchTtsSummary? = null
            BatchTtsRunner(
                DesktopTtsAudioService(engine, Storage(root.resolve("media")), previewStore = TtsPreviewStore(root)),
                this,
                checkpointStore = repository.storeFor(plan),
                assetExists = { it == "pkg/existing.mp3" },
                checkpointOwnership = repository.acquire(plan)
            ).runBatch(jobs, "pkg", plan.id, false, onProgress = { final = it }).join()
            assertEquals(1, engine.calls)
            assertEquals(2, final?.successCount)
            assertNull(repository.storeFor(plan).load())
            assertEquals(unrelated.id, repository.storeFor(unrelated).load()?.batchId)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `missing asset invalidates only recorded success and regenerates target`() = runTest {
        val root = Files.createTempDirectory("tts-plan-missing-asset")
        try {
            val jobs = listOf(job("one"), job("two"))
            val plan = plan(jobs)
            val repository = BatchTtsCheckpointRepository(root.resolve("checkpoints"))
            repository.storeFor(plan).save(
                BatchTtsCheckpoint(plan.id, "pkg", false, listOf(
                    BatchTtsCheckpointRecord(jobs[0].id, jobs[0].textFingerprint(), "SUCCESS", "pkg/missing.mp3")
                ))
            )
            val engine = CountingEngine()
            BatchTtsRunner(
                DesktopTtsAudioService(engine, Storage(root.resolve("media")), previewStore = TtsPreviewStore(root)),
                this,
                checkpointStore = repository.storeFor(plan),
                assetExists = { false },
                checkpointOwnership = repository.acquire(plan)
            ).runBatch(jobs, "pkg", plan.id, false, onProgress = {}).join()
            assertEquals(2, engine.calls)
        } finally { root.toFile().deleteRecursively() }
    }

    private fun checkpoint(plan: BatchTtsGenerationPlan) = BatchTtsCheckpoint(plan.id, plan.packageName, false)
    private fun plan(jobs: List<BatchTtsJob>, overwrite: Boolean = false) = BatchTtsPlanIdentity.create("pkg", jobs, overwrite)
    private fun job(id: String) = BatchTtsJob(id, TtsField.QUESTION, "text-$id", TtsLanguage.ENGLISH, primary, candidateVoices = listOf(primary), id = "${id}_question")
    private fun voice(id: String) = TtsVoice(id, id, "en-US", "en")

    private class CountingEngine : TtsEngine {
        var calls = 0
        override suspend fun listVoices() = emptyList<TtsVoice>()
        override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
            calls++
            Files.write(outputFile, byteArrayOf(1))
            return TtsSynthesisResult(outputFile, 1)
        }
    }

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
