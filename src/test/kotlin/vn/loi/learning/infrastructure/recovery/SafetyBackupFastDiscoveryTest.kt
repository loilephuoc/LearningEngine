package vn.loi.learning.infrastructure.recovery

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SafetyBackupFastDiscoveryTest {
    @Test
    fun `empty directory returns immediately usable empty inventory`() = fixture().use {
        assertTrue(it.manager().discoverSafetyBackupsFast().entries.isEmpty())
    }

    @Test
    fun `existing archive without index is manifest-discovered unknown then fully validated`() = fixture().use { fixture ->
        val archive = fixture.createSafety("safety-v2-1.lebak")
        Files.deleteIfExists(fixture.safety.resolve("safety-backup-index.json"))

        val fast = fixture.manager().discoverSafetyBackupsFast().entries.single()

        assertEquals(SafetyBackupValidationStatus.UNKNOWN, fast.validationStatus)
        assertNotNull(fast.preview)
        assertTrue(Files.exists(fixture.safety.resolve("safety-backup-index.json")))
        val validated = fixture.manager().validateSafetyBackupForIndex(archive).entries.single()
        assertEquals(SafetyBackupValidationStatus.VALIDATED, validated.validationStatus)
    }

    @Test
    fun `matching validated identity loads from index and changed archive invalidates cache`() = fixture().use { fixture ->
        val archive = fixture.createSafety("safety-v2-2.lebak")
        val cached = fixture.manager().discoverSafetyBackupsFast().entries.single()
        assertEquals(SafetyBackupValidationStatus.VALIDATED, cached.validationStatus)

        Files.write(archive, byteArrayOf(0), StandardOpenOption.APPEND)
        val changed = fixture.manager().discoverSafetyBackupsFast().entries.single()

        assertEquals(SafetyBackupValidationStatus.UNKNOWN, changed.validationStatus)
        assertEquals(null, changed.lastValidatedAtUtc)
    }

    @Test
    fun `missing archive and corrupt index are rebuilt from filesystem without crashing`() = fixture().use { fixture ->
        val archive = fixture.createSafety("safety-v2-3.lebak")
        fixture.manager().discoverSafetyBackupsFast()
        Files.delete(archive)
        assertTrue(fixture.manager().discoverSafetyBackupsFast().entries.isEmpty())

        Files.writeString(fixture.safety.resolve("safety-backup-index.json"), "not-json")
        val rebuilt = fixture.manager().discoverSafetyBackupsFast()
        assertTrue(rebuilt.entries.isEmpty())
        assertTrue(Files.readString(fixture.safety.resolve("safety-backup-index.json")).contains("schemaVersion"))
    }

    @Test
    fun `corrupt archive becomes invalid and cannot masquerade as validated`() = fixture().use { fixture ->
        val corrupt = fixture.safety.resolve("safety-v2-4.lebak")
        Files.writeString(corrupt, "corrupt")

        assertEquals(SafetyBackupValidationStatus.UNKNOWN, fixture.manager().discoverSafetyBackupsFast().entries.single().validationStatus)
        val validated = fixture.manager().validateSafetyBackupForIndex(corrupt).entries.single()
        assertEquals(SafetyBackupValidationStatus.INVALID, validated.validationStatus)
        assertFalse(validated.validationStatus == SafetyBackupValidationStatus.VALIDATED)
    }

    @Test
    fun `verified safety creation publishes validated index and retention removes deleted entries`() = fixture().use { fixture ->
        repeat(3) { fixture.createSafety("safety-v2-${it + 10}.lebak", "2026-08-22T0${it + 1}:00:00Z") }
        assertEquals(3, fixture.manager().discoverSafetyBackupsFast().entries.count { it.validationStatus == SafetyBackupValidationStatus.VALIDATED })

        fixture.manager().reconcileSafetyBackupRetention()

        val final = fixture.manager().discoverSafetyBackupsFast()
        assertEquals(2, final.entries.size)
        assertTrue(final.entries.all { Files.exists(it.path) })
    }

    private class Fixture(val root: Path) : AutoCloseable {
        val data = root.resolve("data").createDirectories()
        val media = data.resolve("media").createDirectories()
        val safety = root.resolve("backups").createDirectories()
        init { Files.writeString(data.resolve("installed-packages.json"), "[]") }
        fun manager(iso: String = "2026-08-22T00:00:00Z") = JvmLearningDataRecoveryManager(
            mapOf("data" to data, "media" to media), safety,
            Clock.fixed(Instant.parse(iso), ZoneOffset.UTC)
        )
        fun createSafety(name: String, iso: String = "2026-08-22T00:00:00Z"): Path =
            safety.resolve(name).also { manager(iso).createPortableBackupV2(
                it, PortableBackupV2Descriptor("test", sourcePlatform = "safety")
            ) }
        override fun close() { root.toFile().deleteRecursively() }
    }

    private fun fixture() = Fixture(Files.createTempDirectory("safety-fast-discovery-test-"))
}
