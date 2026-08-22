package vn.loi.learning.infrastructure.recovery

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SafetyBackupInventoryAndRetentionTest {
    @Test
    fun `retention grows conservatively from zero and one valid backup`() {
        listOf(0 to 1, 1 to 2).forEach { (existingCount, expectedCount) ->
            fixture().use { fixture ->
                repeat(existingCount) { index ->
                    fixture.createValidSafety("safety-v2-${index + 1}.lebak", "2026-08-22T0${index + 1}:00:00Z")
                }
                val external = fixture.root.resolve("external.lebak")
                fixture.managerAt("2026-08-22T03:00:00Z").createPortableBackupV2(external, fixture.descriptor())

                val result = assertIs<PortableBackupV2RestoreResult.Success>(
                    fixture.managerAt("2026-08-22T04:00:00Z").restorePortableBackupV2(external)
                )

                assertEquals(expectedCount, result.cleanupResult.retainedValidV2Count)
                assertEquals(0, result.cleanupResult.deletedValidV2Count)
            }
        }
    }

    @Test
    fun `discovery validates v2 metadata and separates legacy invalid and unrelated archives`() = fixture().use { fixture ->
        fixture.createValidSafety("safety-v2-1000.lebak", "2026-08-22T06:10:00Z")
        Files.writeString(fixture.safety.resolve("safety-v2-2000.lebak"), "corrupt")
        Files.writeString(fixture.safety.resolve("safety-999.lebak"), "legacy")
        Files.writeString(fixture.safety.resolve("LearningEngine_Backup_user.lebak"), "user")
        Files.writeString(fixture.safety.resolve("random.lebak"), "random")

        val inventory = fixture.manager().discoverSafetyBackups()

        assertEquals(1, inventory.validV2.size)
        val candidate = inventory.validV2.single()
        assertEquals("safety-v2-1000.lebak", candidate.fileName)
        assertEquals("2026-08-22T06:10:00Z", candidate.preview.createdAtUtc)
        assertEquals(candidate.fileSizeBytes, inventory.totalValidV2Bytes)
        assertEquals(1, inventory.invalidV2Count)
        assertEquals(listOf("safety-999.lebak"), inventory.legacy.map { it.fileName })
    }

    @Test
    fun `successful restore retains newest two valid v2 and never touches user legacy invalid or external source`() = fixture().use { fixture ->
        fixture.createValidSafety("safety-v2-1000.lebak", "2026-08-22T01:00:00Z")
        fixture.createValidSafety("safety-v2-2000.lebak", "2026-08-22T02:00:00Z")
        fixture.createValidSafety("safety-v2-3000.lebak", "2026-08-22T03:00:00Z")
        val legacy = fixture.safety.resolve("safety-legacy.lebak").also { Files.writeString(it, "legacy") }
        val user = fixture.safety.resolve("LearningEngine_Backup_user.lebak").also { Files.writeString(it, "user") }
        val invalid = fixture.safety.resolve("safety-v2-2500.lebak").also { Files.writeString(it, "invalid") }
        val external = fixture.root.resolve("external.lebak")
        fixture.managerAt("2026-08-22T04:00:00Z").createPortableBackupV2(external, fixture.descriptor())

        val result = assertIs<PortableBackupV2RestoreResult.Success>(
            fixture.managerAt("2026-08-22T05:00:00Z").restorePortableBackupV2(external)
        )

        assertEquals(2, result.cleanupResult.retainedValidV2Count)
        assertEquals(2, result.cleanupResult.deletedValidV2Count)
        assertEquals(null, result.cleanupResult.failureMessage)
        assertEquals(2, fixture.manager().discoverSafetyBackups().validV2.size)
        assertTrue(Files.exists(external))
        assertTrue(Files.exists(legacy))
        assertTrue(Files.exists(user))
        assertTrue(Files.exists(invalid))
    }

    @Test
    fun `cleanup failure is reported without failing restore or compensating deletion`() = fixture().use { fixture ->
        fixture.createValidSafety("safety-v2-1000.lebak", "2026-08-22T01:00:00Z")
        fixture.createValidSafety("safety-v2-2000.lebak", "2026-08-22T02:00:00Z")
        val external = fixture.root.resolve("external.lebak")
        fixture.managerAt("2026-08-22T03:00:00Z").createPortableBackupV2(external, fixture.descriptor())
        val before = Files.list(fixture.safety).use { it.count() }
        val manager = fixture.managerAt("2026-08-22T04:00:00Z") { throw IllegalStateException("delete denied") }

        val result = assertIs<PortableBackupV2RestoreResult.Success>(manager.restorePortableBackupV2(external))

        assertNotNull(result.cleanupResult.failureMessage)
        assertEquals(before + 1, Files.list(fixture.safety).use { it.count() })
        assertTrue(Files.exists(external))
    }

    @Test
    fun `active internal restore source is protected even when older than newest two`() = fixture().use { fixture ->
        val active = fixture.safety.resolve("safety-v2-9000.lebak")
        fixture.managerAt("2026-08-22T01:00:00Z").createPortableBackupV2(active, fixture.descriptor())
        fixture.createValidSafety("safety-v2-1000.lebak", "2026-08-22T02:00:00Z")
        fixture.createValidSafety("safety-v2-2000.lebak", "2026-08-22T03:00:00Z")

        val result = assertIs<PortableBackupV2RestoreResult.Success>(
            fixture.managerAt("2026-08-22T04:00:00Z").restorePortableBackupV2(active)
        )

        assertTrue(Files.exists(active))
        assertEquals(3, result.cleanupResult.retainedValidV2Count)
    }

    @Test
    fun `failed new safety creation deletes nothing`() = fixture().use { fixture ->
        fixture.createValidSafety("safety-v2-1000.lebak", "2026-08-22T01:00:00Z")
        val external = fixture.root.resolve("external.lebak")
        fixture.managerAt("2026-08-22T02:00:00Z").createPortableBackupV2(external, fixture.descriptor())
        val existing = fixture.safety.resolve("safety-v2-1000.lebak")
        val manager = JvmLearningDataRecoveryManager(
            roots = mapOf("data" to fixture.data, "media" to fixture.media),
            safetyDirectory = fixture.safety,
            clock = Clock.fixed(Instant.parse("2026-08-22T03:00:00Z"), ZoneOffset.UTC),
            failureHook = { hook, _ -> if (hook == "restore-v2-safety-backup-start") error("cannot create") }
        )

        assertIs<PortableBackupV2RestoreResult.SafetyBackupFailed>(manager.restorePortableBackupV2(external))
        assertTrue(Files.exists(existing))
        assertFalse(Files.exists(fixture.safety.resolve("safety-v2-1787367600000.lebak")))
    }

    private class Fixture(val root: Path) : AutoCloseable {
        val data = root.resolve("data").createDirectories()
        val media = data.resolve("media").createDirectories()
        val safety = root.resolve("backups").createDirectories()

        init { Files.writeString(data.resolve("installed-packages.json"), "[]") }

        fun descriptor() = PortableBackupV2Descriptor("test", sourcePlatform = "test")
        fun manager() = managerAt("2026-08-22T00:00:00Z")
        fun managerAt(iso: String, delete: (Path) -> Unit = Files::delete) = JvmLearningDataRecoveryManager(
            roots = mapOf("data" to data, "media" to media),
            safetyDirectory = safety,
            clock = Clock.fixed(Instant.parse(iso), ZoneOffset.UTC),
            safetyBackupDelete = delete
        )
        fun createValidSafety(name: String, iso: String) {
            managerAt(iso).createPortableBackupV2(safety.resolve(name), descriptor())
        }
        override fun close() { root.toFile().deleteRecursively() }
    }

    private fun fixture() = Fixture(Files.createTempDirectory("safety-inventory-test-"))
}
