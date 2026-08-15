package vn.loi.learning.infrastructure.recovery

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmLearningDataRecoveryOverlappingRootsTest {
    @Test
    fun `portable file equality handles empty size content and multiple buffers`() = fixture().use { fixture ->
        val first = fixture.root.resolve("first.bin")
        val second = fixture.root.resolve("second.bin")
        Files.write(first, byteArrayOf())
        Files.write(second, byteArrayOf())
        assertTrue(filesEqual(first, second))

        val large = ByteArray(64 * 1024 * 3 + 17) { index -> (index % 251).toByte() }
        Files.write(first, large)
        Files.write(second, large)
        assertTrue(filesEqual(first, second))

        large[64 * 1024 + 9] = (large[64 * 1024 + 9] + 1).toByte()
        Files.write(second, large)
        assertFalse(filesEqual(first, second))

        Files.write(second, byteArrayOf(1))
        assertFalse(filesEqual(first, second))
    }

    @Test
    fun `authoritative Android shape maps 9807 logical entries to 4910 physical targets`() = fixture().use { fixture ->
        val names = buildList {
            repeat(13) { add("data/store-$it.json") }
            repeat(4_897) { index ->
                add("data/media/package/media-$index.bin")
                add("media/package/media-$index.bin")
            }
        }

        val groups = groupLogicalEntriesByPhysicalTarget(
            mapOf("data" to fixture.data, "media" to fixture.media), names
        )

        assertEquals(9_807, names.size)
        assertEquals(4_910, groups.size)
        assertEquals(4_897, groups.count { it.second.size == 2 })
    }

    @Test
    fun `identical first middle and final aliases restore once independent of manifest order`() = fixture().use { fixture ->
        val entries = linkedMapOf(
            "media/z.bin" to "final", "data/media/a.bin" to "first", "data/state.json" to "state",
            "media/m.bin" to "middle", "data/media/z.bin" to "final", "media/a.bin" to "first",
            "data/media/m.bin" to "middle"
        ).mapValues { it.value.toByteArray() }
        val archive = fixture.archive("aliases.lebak", entries)

        fixture.manager().restore(archive, false)

        assertContentEquals("first".toByteArray(), Files.readAllBytes(fixture.media.resolve("a.bin")))
        assertContentEquals("middle".toByteArray(), Files.readAllBytes(fixture.media.resolve("m.bin")))
        assertContentEquals("final".toByteArray(), Files.readAllBytes(fixture.media.resolve("z.bin")))
        assertEquals(4, Files.walk(fixture.data).use { paths -> paths.filter(Files::isRegularFile).count() })
    }

    @Test
    fun `conflicting aliases are typed and rejected before safety backup or live mutation`() = fixture().use { fixture ->
        val live = fixture.data.resolve("state.json")
        Files.writeString(live, "pre-restore")
        val archive = fixture.archive(
            "conflict.lebak",
            mapOf("data/media/foo.bin" to byteArrayOf(1), "media/foo.bin" to byteArrayOf(2))
        )

        val failure = assertFailsWith<ConflictingRestoreAliasException> { fixture.manager().restore(archive, false) }

        assertEquals(listOf("data/media/foo.bin", "media/foo.bin"), failure.logicalEntries)
        assertTrue(failure.message.orEmpty().contains("CONFLICTING_ALIAS"))
        assertTrue(failure.message.orEmpty().contains(fixture.media.resolve("foo.bin").toString()))
        assertEquals("pre-restore", Files.readString(live))
        assertFalse(Files.exists(fixture.media.resolve("foo.bin")))
        assertTrue(Files.list(fixture.safety).use { it.findAny().isEmpty })
    }

    @Test
    fun `later unique target failure rolls overlapping live state back exactly`() = fixture().use { fixture ->
        val liveMedia = fixture.media.resolve("old.bin")
        liveMedia.parent.createDirectories()
        Files.writeString(liveMedia, "old-media")
        Files.writeString(fixture.data.resolve("state.json"), "old-state")
        val before = fixture.snapshot()
        val archive = fixture.archive(
            "restored.lebak",
            mapOf(
                "data/media/new.bin" to "new-media".toByteArray(),
                "media/new.bin" to "new-media".toByteArray(),
                "data/z.json" to "new-state".toByteArray()
            )
        )
        val manager = fixture.manager { phase, detail ->
            if (phase == "restore-write" && detail?.startsWith("1:") == true) error("later failure")
        }

        assertFailsWith<LearningDataRecoveryException> { manager.restore(archive, false) }

        val after = fixture.snapshot()
        assertEquals(before.keys, after.keys)
        before.forEach { (name, bytes) -> assertContentEquals(bytes, after.getValue(name), name) }
    }

    private fun fixture() = Fixture(Files.createTempDirectory("overlapping-recovery-"))

    private class Fixture(val root: Path) : AutoCloseable {
        val data = root.resolve("data").createDirectories()
        val media = data.resolve("media").createDirectories()
        val safety = root.resolve("safety").createDirectories()

        fun manager(hook: (String, String?) -> Unit = { _, _ -> }) = JvmLearningDataRecoveryManager(
            mapOf("data" to data, "media" to media), safety, failureHook = hook
        )

        fun archive(name: String, files: Map<String, ByteArray>): Path = root.resolve(name).also { archive ->
            ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                val manifest = buildString {
                    appendLine("format=1")
                    appendLine("created=2026-08-15T00:00:00Z")
                    appendLine("files=${files.size}")
                    files.entries.forEachIndexed { index, (entry, bytes) ->
                        appendLine("file.$index=$entry\t${bytes.size}\t${sha256(bytes)}")
                    }
                }
                zip.putNextEntry(ZipEntry(JvmLearningDataRecoveryManager.MANIFEST_ENTRY))
                zip.write(manifest.toByteArray(StandardCharsets.UTF_8)); zip.closeEntry()
                files.forEach { (entry, bytes) -> zip.putNextEntry(ZipEntry(entry)); zip.write(bytes); zip.closeEntry() }
            }
            ZipFile(archive.toFile()).use { check(it.size() == files.size + 1) }
        }

        fun snapshot(): Map<String, ByteArray> = Files.walk(data).use { paths -> paths.filter(Files::isRegularFile)
            .map { data.relativize(it).toString() to Files.readAllBytes(it) }.toList().toMap() }

        override fun close() { root.toFile().deleteRecursively() }
    }

    companion object {
        private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
