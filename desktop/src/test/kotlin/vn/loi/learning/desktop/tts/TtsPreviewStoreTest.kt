package vn.loi.learning.desktop.tts

import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TtsPreviewStoreTest {

    private lateinit var tempDir: java.nio.file.Path
    private lateinit var store: TtsPreviewStore

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("tts-preview-test-")
        store = TtsPreviewStore(tempDir)
    }

    @AfterTest
    fun tearDown() {
        store.close()
        if (Files.exists(tempDir)) {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `createPreviewFile creates file path inside temp directory`() {
        val file1 = store.createPreviewFile()
        assertTrue(file1.startsWith(tempDir))
        assertTrue(file1.toString().endsWith(".mp3"))
    }

    @Test
    fun `allocating new preview cleans up previous preview file`() {
        val file1 = store.createPreviewFile()
        Files.write(file1, byteArrayOf(1, 2, 3, 4))
        assertTrue(Files.exists(file1))

        val file2 = store.createPreviewFile()
        assertFalse(Files.exists(file1), "Previous preview file must be deleted on new preview allocation")
        assertTrue(file2.startsWith(tempDir))
    }

    @Test
    fun `cleanPrevious removes last preview file`() {
        val file = store.createPreviewFile()
        Files.write(file, byteArrayOf(5, 6, 7))
        assertTrue(Files.exists(file))

        store.cleanPrevious()
        assertFalse(Files.exists(file))
    }

    @Test
    fun `close clears store and deletes temp directory`() {
        val file = store.createPreviewFile()
        Files.write(file, byteArrayOf(1, 2, 3))
        assertTrue(Files.exists(file))

        store.close()
        assertFalse(Files.exists(file))
        assertFalse(Files.exists(tempDir))
    }
}
