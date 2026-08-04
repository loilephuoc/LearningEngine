package vn.loi.learning.infrastructure.contentmedia

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.*
import kotlin.test.Test

class JvmContentMediaStorageTest {
    @Test fun `canonical and persisted-root media references resolve one stored asset`() {
        val root=createTempDirectory("media-contract")
        try {
            val storage=JvmContentMediaStorage(root)
            val asset=storage.store("Vocabulary_In_Use_Elementary","images/bed.png",byteArrayOf(1,2,3))
            assertEquals("Vocabulary_In_Use_Elementary/images/bed.png",asset.relativePath)
            val expected=root.resolve(asset.relativePath)
            assertEquals(expected,storage.resolve(asset.relativePath))
            assertEquals(expected,storage.resolve("media/${asset.relativePath}"))
            assertFalse(Files.exists(root.resolve("media/Vocabulary_In_Use_Elementary/images/bed.png")))
        } finally { Files.walk(root).use{paths->paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)} }
    }

    @Test fun `compatibility is deterministic and cannot escape media root`() {
        val root=createTempDirectory("media-contract-safe")
        try {
            val storage=JvmContentMediaStorage(root)
            assertNull(storage.resolve("media/../../outside.png"))
            assertNull(storage.resolve("missing.png"))
        } finally { Files.walk(root).use{paths->paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)} }
    }
}
