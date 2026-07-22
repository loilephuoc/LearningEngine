package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopRecoveryManagerTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `backup includes durable data and config but excludes temp and previous backups`() {
        fixture().use { fixture ->
            Files.writeString(fixture.data.resolve("learning-items.json"), "items")
            Files.writeString(fixture.data.resolve("stale.tmp"), "temporary")
            Files.writeString(fixture.config.resolve("runtime.properties"), "theme=dark")
            Files.createDirectories(fixture.config.resolve("backups"))
            Files.writeString(fixture.config.resolve("backups/old.lebak"), "old")
            val target = fixture.root.resolve("manual.lebak")

            fixture.manager.createBackup(target)
            val manifest = fixture.manager.validate(target)

            assertEquals(
                listOf("config/runtime.properties", "data/learning-items.json"),
                manifest.files.map { it.path }
            )
            assertEquals(clock.instant(), manifest.createdAt)
        }
    }

    @Test
    fun `restore validates first creates safety backup and replaces whole snapshot`() {
        fixture().use { fixture ->
            val dataFile = fixture.data.resolve("state.json")
            Files.writeString(dataFile, "snapshot")
            val backup = fixture.root.resolve("manual.lebak")
            fixture.manager.createBackup(backup)
            Files.writeString(dataFile, "current")
            Files.writeString(fixture.data.resolve("extra.json"), "remove")

            val safety = fixture.manager.restore(backup, operationActive = false)

            assertEquals("snapshot", Files.readString(dataFile))
            assertFalse(Files.exists(fixture.data.resolve("extra.json")))
            assertTrue(Files.exists(safety))
            assertEquals(
                listOf("data/extra.json", "data/state.json"),
                fixture.manager.validate(safety).files.map { it.path }
            )
        }
    }

    @Test
    fun `active operation blocks restore before safety backup or mutation`() {
        fixture().use { fixture ->
            val dataFile = fixture.data.resolve("state.json")
            Files.writeString(dataFile, "current")
            val backup = fixture.root.resolve("manual.lebak")
            fixture.manager.createBackup(backup)

            assertFailsWith<DesktopRecoveryException> {
                fixture.manager.restore(backup, operationActive = true)
            }

            assertEquals("current", Files.readString(dataFile))
            assertFalse(Files.exists(fixture.manager.safetyBackupDirectory))
        }
    }

    @Test
    fun `checksum failure preserves current data and creates no safety backup`() {
        fixture().use { fixture ->
            val dataFile = fixture.data.resolve("state.json")
            Files.writeString(dataFile, "original")
            val valid = fixture.root.resolve("valid.lebak")
            fixture.manager.createBackup(valid)
            val corrupt = fixture.root.resolve("corrupt.lebak")
            ZipFile(valid.toFile()).use { input ->
                ZipOutputStream(Files.newOutputStream(corrupt)).use { output ->
                    input.entries().asSequence().forEach { entry ->
                        output.putNextEntry(ZipEntry(entry.name))
                        if (entry.name == "data/state.json") output.write("tampered".toByteArray())
                        else input.getInputStream(entry).use { it.copyTo(output) }
                        output.closeEntry()
                    }
                }
            }

            assertFailsWith<DesktopRecoveryException> { fixture.manager.restore(corrupt, false) }

            assertEquals("original", Files.readString(dataFile))
            assertFalse(Files.exists(fixture.manager.safetyBackupDirectory))
        }
    }

    @Test
    fun `negative manifest file count is rejected before restore mutation`() {
        fixture().use { fixture ->
            val dataFile = fixture.data.resolve("state.json")
            Files.writeString(dataFile, "current")
            val malformed = fixture.root.resolve("negative-count.lebak")
            ZipOutputStream(Files.newOutputStream(malformed)).use { zip ->
                zip.putNextEntry(ZipEntry(DesktopRecoveryManager.MANIFEST_ENTRY))
                zip.write(
                    """
                    format=1
                    created=2026-07-22T12:00:00Z
                    files=-1
                    """.trimIndent().toByteArray()
                )
                zip.closeEntry()
            }

            assertFailsWith<DesktopRecoveryException> {
                fixture.manager.restore(malformed, operationActive = false)
            }

            assertEquals("current", Files.readString(dataFile))
            assertFalse(Files.exists(fixture.manager.safetyBackupDirectory))
        }
    }

    @Test
    fun `existing backup is never overwritten`() {
        fixture().use { fixture ->
            val target = fixture.root.resolve("manual.lebak")
            val original = "existing".toByteArray()
            Files.write(target, original)
            assertFailsWith<DesktopRecoveryException> { fixture.manager.createBackup(target) }
            assertContentEquals(original, Files.readAllBytes(target))
        }
    }

    @Test
    fun `restore write failure rolls back exact current durable bytes`() {
        val root = Files.createTempDirectory("desktop-recovery-rollback-test")
        try {
            val data = Files.createDirectories(root.resolve("data"))
            val config = Files.createDirectories(root.resolve("config"))
            Files.writeString(data.resolve("state.json"), "backup-state")
            Files.writeString(config.resolve("runtime.properties"), "backup-config")
            val backup = root.resolve("manual.lebak")
            DesktopRecoveryManager(data, config, clock).createBackup(backup)
            val currentData = "current-state".toByteArray()
            val currentConfig = "current-config".toByteArray()
            Files.write(data.resolve("state.json"), currentData)
            Files.write(config.resolve("runtime.properties"), currentConfig)
            var writes = 0
            val failing = DesktopRecoveryManager(data, config, clock) {
                writes++
                if (writes == 2) throw java.io.IOException("injected write failure")
            }

            assertFailsWith<DesktopRecoveryException> { failing.restore(backup, false) }

            assertContentEquals(currentData, Files.readAllBytes(data.resolve("state.json")))
            assertContentEquals(currentConfig, Files.readAllBytes(config.resolve("runtime.properties")))
            assertTrue(Files.list(failing.safetyBackupDirectory).use { it.findAny().isPresent })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun fixture(): Fixture {
        val root = Files.createTempDirectory("desktop-recovery-test")
        val data = Files.createDirectories(root.resolve("data"))
        val config = Files.createDirectories(root.resolve("config"))
        return Fixture(root, data, config, DesktopRecoveryManager(data, config, clock))
    }

    private data class Fixture(
        val root: java.nio.file.Path,
        val data: java.nio.file.Path,
        val config: java.nio.file.Path,
        val manager: DesktopRecoveryManager
    ) : AutoCloseable {
        override fun close() { root.toFile().deleteRecursively() }
    }
}
