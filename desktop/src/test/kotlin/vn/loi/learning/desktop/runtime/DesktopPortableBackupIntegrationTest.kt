package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Descriptor
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult

class DesktopPortableBackupIntegrationTest {
    private val clock = Clock.fixed(Instant.parse("2026-08-22T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `desktop creates and previews portable backup v2 with full metadata`() {
        val root = Files.createTempDirectory("desktop-v2-test-")
        try {
            val data = Files.createDirectories(root.resolve("data"))
            val media = Files.createDirectories(data.resolve("media"))
            val config = Files.createDirectories(root.resolve("config"))
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"c-1\"},{\"id\":\"c-2\"}]")
            val audio = media.resolve("pkg1/audio.mp3")
            Files.createDirectories(audio.parent)
            Files.write(audio, byteArrayOf(1, 2, 3, 4))

            val manager = DesktopRecoveryManager(data, config, clock)
            val backup = root.resolve("portable.lebak")

            manager.createPortableBackupV2(
                backup,
                PortableBackupV2Descriptor("desktop-app", 10, "desktop", listOf("user-1"))
            )

            assertTrue(Files.isRegularFile(backup))

            val preview = manager.previewPortableBackupV2(backup)
            assertEquals(2, preview.backupSchemaVersion)
            assertEquals("desktop", preview.sourcePlatform)
            assertEquals(2, preview.counts.contents)
            assertEquals(1, preview.counts.mediaFiles)
            assertEquals(4, preview.bytes.mediaBytes)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `desktop restores portable backup v2 cleanly and replaces live state with rollback safety`() {
        val root = Files.createTempDirectory("desktop-v2-restore-")
        try {
            val data = Files.createDirectories(root.resolve("data"))
            val media = Files.createDirectories(data.resolve("media"))
            val config = Files.createDirectories(root.resolve("config"))

            val manager = DesktopRecoveryManager(data, config, clock)

            // State 1: create backup
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"content-A\"}]")
            val mediaA = media.resolve("pkg/a.mp3")
            Files.createDirectories(mediaA.parent)
            Files.write(mediaA, byteArrayOf(10, 20, 30))

            val backup = root.resolve("stateA.lebak")
            manager.createPortableBackupV2(backup)

            // State 2: mutate live state
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"content-B\"}]")
            Files.write(mediaA, byteArrayOf(99, 99, 99))
            val mediaB = media.resolve("pkg/b.mp3")
            Files.write(mediaB, byteArrayOf(40, 50))

            // Restore State A
            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.Success>(result)

            // Assert live state restored to State A
            assertEquals("[{\"id\":\"content-A\"}]", Files.readString(data.resolve("contents.json")))
            kotlin.test.assertContentEquals(byteArrayOf(10, 20, 30), Files.readAllBytes(mediaA))
            assertFalse(Files.exists(mediaB))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
