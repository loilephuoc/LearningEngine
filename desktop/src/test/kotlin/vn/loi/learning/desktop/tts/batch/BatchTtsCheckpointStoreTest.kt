package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BatchTtsCheckpointStoreTest {
    @Test
    fun `checkpoint is atomically persisted loaded and cleared`() {
        val root = Files.createTempDirectory("tts-checkpoint")
        try {
            val store = BatchTtsCheckpointStore(root.resolve("batch.json"))
            val expected = BatchTtsCheckpoint(
                "batch", "pkg", false,
                listOf(BatchTtsCheckpointRecord("id_question", "fingerprint", "SUCCESS", "pkg/audio.mp3"))
            )
            store.save(expected)
            assertEquals(expected, store.load())
            store.clear()
            assertNull(store.load())
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `corrupted checkpoint fails closed without crashing`() {
        val root = Files.createTempDirectory("tts-bad-checkpoint")
        try {
            val path = root.resolve("batch.json")
            Files.writeString(path, "not-json")
            assertNull(BatchTtsCheckpointStore(path).load())
        } finally { root.toFile().deleteRecursively() }
    }
}
