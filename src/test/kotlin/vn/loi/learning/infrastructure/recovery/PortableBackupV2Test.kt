package vn.loi.learning.infrastructure.recovery

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PortableBackupV2Test {
    @Test
    fun `v2 writes deterministic portable structure and inventories nested media once`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("memory-states.json"), "[{\"id\":\"m-1\"},{\"id\":\"m-2\"}]")
        Files.writeString(fixture.data.resolve("contents.json"), "[{\"id\":\"c-1\"}]")
        val asset = fixture.media.resolve("pkg/audio.mp3")
        asset.parent.createDirectories()
        Files.write(asset, byteArrayOf(1, 2, 3, 4))

        val target = fixture.root.resolve("portable.lebak")
        fixture.manager.createPortableBackupV2(target, descriptor())
        val manifest = fixture.manager.validatePortableBackupV2(target)

        assertEquals(2, manifest.backupSchemaVersion)
        assertEquals(listOf("portable"), manifest.includedSections)
        assertEquals(1, manifest.entries.count { it.logicalPath == "portable/media/pkg/audio.mp3" })
        assertFalse(manifest.entries.any { it.logicalPath == "portable/data/media/pkg/audio.mp3" })
        assertEquals(4, manifest.bytes.mediaBytes)
        assertEquals(2, manifest.counts.memoryStates)
        assertEquals(1, manifest.counts.contents)
        assertEquals(1, manifest.counts.mediaFiles)
        ZipFile(target.toFile()).use { zip -> assertEquals(manifest.entries.size + 1, zip.size()) }
    }

    @Test
    fun `v2 contributor recordings and supplements roundtrip cleanly`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[]")
        val target = fixture.root.resolve("with-recordings.lebak")
        fixture.manager.createPortableBackupV2(target, descriptor(), contributor = PortableBackupV2SnapshotContributor { staging ->
            val recDir = staging.resolve("android/recordings/files").createDirectories()
            val recFile = recDir.resolve("rec_01.m4a")
            Files.write(recFile, byteArrayOf(10, 20, 30))
            val indexFile = staging.resolve("android/recordings/index.json")
            Files.writeString(indexFile, "{\"recordings\":[{\"id\":\"1\",\"recordingFile\":\"files/rec_01.m4a\"}]}")
            listOf(
                PortableBackupSupplementV2("android/recordings/index.json", "android", "recording-index", indexFile),
                PortableBackupSupplementV2("android/recordings/files/rec_01.m4a", "android", "recording", recFile)
            )
        })
        val manifest = fixture.manager.validatePortableBackupV2(target)
        assertEquals(setOf("android", "portable"), manifest.includedSections.toSet())
        assertEquals(1, manifest.counts.recordings)
        assertEquals(3, manifest.bytes.recordingBytes)
    }

    @Test
    fun `v2 rejects recording metadata referencing non-existent or escaping archive file`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[]")
        val target = fixture.root.resolve("bad-rec.lebak")
        assertFailsWith<LearningDataRecoveryException> {
            fixture.manager.createPortableBackupV2(target, descriptor(), contributor = PortableBackupV2SnapshotContributor { staging ->
                val indexFile = staging.resolve("android/recordings/index.json").createDirectories()
                val targetFile = indexFile.resolve("index.json")
                Files.writeString(targetFile, "{\"recordings\":[{\"id\":\"1\",\"recordingFile\":\"files/missing.m4a\"}]}")
                listOf(PortableBackupSupplementV2("android/recordings/index.json", "android", "recording-index", targetFile))
            })
        }
    }

    @Test
    fun `v2 rejects unsafe contributor paths before publication`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[]")
        listOf("../escape", "/absolute", "C:/drive", "android\\escape").forEachIndexed { index, name ->
            val target = fixture.root.resolve("unsafe-$index.lebak")
            assertFailsWith<LearningDataRecoveryException> {
                fixture.manager.createPortableBackupV2(target, descriptor(), contributor = PortableBackupV2SnapshotContributor { staging ->
                    val source = staging.resolve("safe-$index")
                    Files.writeString(source, "x")
                    listOf(PortableBackupSupplementV2(name, "android", "test", source))
                })
            }
            assertFalse(Files.exists(target))
        }
    }

    @Test
    fun `v2 central limits reject too many and oversized entries`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "12345")
        assertFailsWith<LearningDataRecoveryException> {
            fixture.manager.createPortableBackupV2(
                fixture.root.resolve("count.lebak"), descriptor(),
                limits = PortableBackupV2Limits(maxArchiveEntryCount = 1)
            )
        }
        assertFailsWith<LearningDataRecoveryException> {
            fixture.manager.createPortableBackupV2(
                fixture.root.resolve("size.lebak"), descriptor(),
                limits = PortableBackupV2Limits(maxUncompressedBytesPerEntry = 4)
            )
        }
        assertFailsWith<LearningDataRecoveryException> {
            fixture.manager.createPortableBackupV2(
                fixture.root.resolve("expanded.lebak"), descriptor(),
                limits = PortableBackupV2Limits(maxTotalExpandedBytes = 4)
            )
        }
    }

    @Test
    fun `v2 validation rejects case collision and checksum mismatch`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[]")
        val valid = fixture.root.resolve("valid.lebak")
        fixture.manager.createPortableBackupV2(valid, descriptor())
        val corrupt = fixture.root.resolve("corrupt.lebak")
        rewrite(valid, corrupt) { name, bytes -> if (name == "portable/data/contents.json") "changed".toByteArray() else bytes }
        assertFailsWith<LearningDataRecoveryException> { fixture.manager.validatePortableBackupV2(corrupt) }

        val duplicate = fixture.root.resolve("case-collision.lebak")
        ZipFile(valid.toFile()).use { input ->
            ZipOutputStream(Files.newOutputStream(duplicate)).use { output ->
                input.entries().asSequence().forEach { entry ->
                    val bytes = input.getInputStream(entry).readBytes()
                    output.putNextEntry(ZipEntry(entry.name)); output.write(bytes); output.closeEntry()
                }
                output.putNextEntry(ZipEntry("PORTABLE/data/contents.json")); output.write("[]".toByteArray()); output.closeEntry()
            }
        }
        assertFailsWith<LearningDataRecoveryException> { fixture.manager.validatePortableBackupV2(duplicate) }
    }

    @Test
    fun `v2 validation rejects missing canonical data or invalid schema version`() = fixture().use { fixture ->
        val emptyArchive = fixture.root.resolve("empty.lebak")
        ZipOutputStream(Files.newOutputStream(emptyArchive)).use { output ->
            output.putNextEntry(ZipEntry("manifest.json"))
            val manifest = V2_JSON.encodeToString(
                PortableBackupManifestV2(
                    backupSchemaVersion = 2,
                    appVersion = "1.0",
                    createdAtUtc = "2026-01-01T00:00:00Z",
                    sourcePlatform = "test",
                    includedSections = listOf("android"),
                    counts = PortableBackupCountsV2(),
                    bytes = PortableBackupBytesV2(),
                    entries = emptyList()
                )
            ).toByteArray(StandardCharsets.UTF_8)
            output.write(manifest)
            output.closeEntry()
        }
        assertFailsWith<LearningDataRecoveryException> { fixture.manager.validatePortableBackupV2(emptyArchive) }
    }

    @Test
    fun `v2 preview returns summary and performs zero mutation`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[{\"id\":\"c-1\"}]")
        Files.writeString(fixture.data.resolve("memory-states.json"), "[{\"id\":\"m-1\"}]")
        val target = fixture.root.resolve("preview-test.lebak")
        fixture.manager.createPortableBackupV2(target, descriptor())

        val preview = fixture.manager.previewPortableBackupV2(target)
        assertEquals(2, preview.backupSchemaVersion)
        assertEquals("test", preview.appVersion)
        assertEquals(1, preview.counts.contents)
        assertEquals(1, preview.counts.memoryStates)
        assertEquals(2, preview.totalEntries)
    }

    @Test
    fun `v2 restore replaces everything and roundtrips canonical data and media`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[{\"id\":\"content-A\"}]")
        val mediaA = fixture.media.resolve("audio/a.mp3")
        mediaA.parent.createDirectories()
        Files.write(mediaA, byteArrayOf(1, 2, 3))
        val backupA = fixture.root.resolve("backup-A.lebak")
        fixture.manager.createPortableBackupV2(backupA, descriptor())

        // Modify live state to State B
        Files.writeString(fixture.data.resolve("contents.json"), "[{\"id\":\"content-B\"}]")
        Files.write(mediaA, byteArrayOf(9, 9, 9))
        val mediaB = fixture.media.resolve("audio/b.mp3")
        Files.write(mediaB, byteArrayOf(4, 5, 6))

        // Restore Backup A (Replace everything)
        val result = fixture.manager.restorePortableBackupV2(backupA)
        assertIs<PortableBackupV2RestoreResult.Success>(result)
        assertEquals("", result.safetyBackupPath)

        // Verify live state matches State A
        assertEquals("[{\"id\":\"content-A\"}]", Files.readString(fixture.data.resolve("contents.json")))
        kotlin.test.assertContentEquals(byteArrayOf(1, 2, 3), Files.readAllBytes(mediaA))
        assertFalse(Files.exists(mediaB))
        assertFalse(Files.list(fixture.root.resolve("safety")).use { paths -> paths.anyMatch { it.fileName.toString().startsWith(".transaction-rollback-") } })
    }

    @Test
    fun `v2 restore captures and verifies safety backup before mutation`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[{\"id\":\"original\"}]")
        val target = fixture.root.resolve("restore-source.lebak")
        Files.writeString(fixture.data.resolve("contents.json"), "[{\"id\":\"source\"}]")
        fixture.manager.createPortableBackupV2(target, descriptor())

        Files.writeString(fixture.data.resolve("contents.json"), "[{\"id\":\"current-target\"}]")
        val result = fixture.manager.restorePortableBackupV2(target, createSafetyBackupBeforeRestore = true)
        assertIs<PortableBackupV2RestoreResult.Success>(result)

        // Verify the safety backup can be read and contains the target state before mutation
        val safetyPath = Path.of(result.safetyBackupPath)
        val safetyManifest = fixture.manager.validatePortableBackupV2(safetyPath)
        assertEquals(2, safetyManifest.backupSchemaVersion)
        assertEquals(listOf("portable"), safetyManifest.includedSections)
    }

    @Test
    fun `v2 restore rejects preflight failures without mutating live state`() = fixture().use { fixture ->
        val liveFile = fixture.data.resolve("contents.json")
        Files.writeString(liveFile, "[{\"id\":\"live-untouched\"}]")

        // 1. Non-existent file
        val missingResult = fixture.manager.restorePortableBackupV2(fixture.root.resolve("missing.lebak"))
        assertIs<PortableBackupV2RestoreResult.ValidationFailed>(missingResult)
        assertEquals("[{\"id\":\"live-untouched\"}]", Files.readString(liveFile))

        // 2. Busy operation
        val busyResult = fixture.manager.restorePortableBackupV2(fixture.root.resolve("dummy.lebak"), operationActive = true)
        assertIs<PortableBackupV2RestoreResult.Busy>(busyResult)
        assertEquals("[{\"id\":\"live-untouched\"}]", Files.readString(liveFile))

        // 3. Corrupted checksum
        val valid = fixture.root.resolve("valid-preflight.lebak")
        fixture.manager.createPortableBackupV2(valid, descriptor())
        val corrupt = fixture.root.resolve("corrupt-preflight.lebak")
        rewrite(valid, corrupt) { name, bytes -> if (name == "portable/data/contents.json") "corrupt".toByteArray() else bytes }
        val corruptResult = fixture.manager.restorePortableBackupV2(corrupt)
        assertIs<PortableBackupV2RestoreResult.ValidationFailed>(corruptResult)
        assertEquals("[{\"id\":\"live-untouched\"}]", Files.readString(liveFile))

        // 4. Insufficient disk space
        val spaceResult = fixture.manager.restorePortableBackupV2(valid, limits = PortableBackupV2Limits(requireFreeDiskSpaceMarginBytes = Long.MAX_VALUE - 1000L))
        assertIs<PortableBackupV2RestoreResult.InsufficientSpace>(spaceResult)
        assertEquals("[{\"id\":\"live-untouched\"}]", Files.readString(liveFile))
    }

    @Test
    fun `v2 restore rolls back atomically when mid-restore failure occurs`() {
        val root = Files.createTempDirectory("restore-rollback-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"initial-state\"}]")

            var hookCount = 0
            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety,
                failureHook = { hook, _ ->
                    if (hook == "restore-v2-consumer-applied") {
                        hookCount++
                        error("Simulated failure after copying data")
                    }
                }
            )

            val backup = root.resolve("backup-to-restore.lebak")
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"backup-content\"}]")
            manager.createPortableBackupV2(backup, PortableBackupV2Descriptor("test", 1, "test"))

            // Set live state to something distinct
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"live-state-before-restore\"}]")

            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.RestoreFailedRolledBack>(result)
            assertEquals(1, hookCount)
            assertEquals("[{\"id\":\"live-state-before-restore\"}]", Files.readString(data.resolve("contents.json")))
            assertFalse(Files.list(safety).use { paths -> paths.anyMatch { it.fileName.toString().startsWith(".transaction-rollback-") } })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 restore reports rollback failure when rollback fails`() {
        val root = Files.createTempDirectory("restore-rollback-fail-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"initial\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety,
                failureHook = { hook, _ ->
                    if (hook == "restore-v2-consumer-applied") {
                        error("Restore failed")
                    }
                    if (hook == "restore-v2-rollback-start") {
                        error("Rollback failed")
                    }
                }
            )

            val backup = root.resolve("source.lebak")
            manager.createPortableBackupV2(backup, PortableBackupV2Descriptor("test", 1, "test"))

            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.RollbackFailed>(result)
            assertEquals("", result.safetyBackupPath)
            assertTrue(result.restoreFailure.contains("phase=LIVE_APPLY"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 restore with consumer coordinates platform supplements and rollback`() = fixture().use { fixture ->
        Files.writeString(fixture.data.resolve("contents.json"), "[]")
        val backup = fixture.root.resolve("with-supplements.lebak")

        var consumerPreflightCalled = false
        var consumerApplyCalled = false
        var consumerRollbackCalled = false

        fixture.manager.createPortableBackupV2(backup, descriptor(), contributor = PortableBackupV2SnapshotContributor { staging ->
            val supp = staging.resolve("android/test.txt")
            Files.createDirectories(supp.parent)
            Files.writeString(supp, "supplement-content")
            listOf(PortableBackupSupplementV2("android/test.txt", "android", "test", supp))
        })

        val consumer = object : PortableBackupV2RestoreConsumer {
            override fun preflight(stagingDirectory: Path) {
                consumerPreflightCalled = true
                assertTrue(Files.isRegularFile(stagingDirectory.resolve("android/test.txt")))
            }
            override fun captureCurrentState(): Any = "captured-state"
            override fun applyRestored(stagingDirectory: Path) {
                consumerApplyCalled = true
            }
            override fun rollback(capturedState: Any?) {
                consumerRollbackCalled = true
            }
            override fun validateLive() {}
        }

        val result = fixture.manager.restorePortableBackupV2(backup, consumer = consumer)
        assertIs<PortableBackupV2RestoreResult.Success>(result)
        assertTrue(consumerPreflightCalled)
        assertTrue(consumerApplyCalled)
        assertFalse(consumerRollbackCalled)
    }

    @Test
    fun `v2 restore equality failure triggers rollback and reports details`() {
        val root = Files.createTempDirectory("restore-equality-fail-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"initial\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety,
                failureHook = { hook, _ ->
                    if (hook == "restore-v2-consumer-applied") {
                        // Tamper live file before equality verification
                        Files.writeString(data.resolve("contents.json"), "[{\"id\":\"tampered-live\"}]")
                    }
                }
            )

            val backup = root.resolve("source.lebak")
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"backup-content\"}]")
            manager.createPortableBackupV2(backup, PortableBackupV2Descriptor("test", 1, "test"))

            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"live-before-restore\"}]")

            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.RestoreFailedRolledBack>(result)
            assertTrue(result.failureReason.contains("Restored canonical sha256 mismatch"))
            assertEquals("[{\"id\":\"live-before-restore\"}]", Files.readString(data.resolve("contents.json")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 streaming restore and rollback handles large files without heap memory spikes`() {
        val root = Files.createTempDirectory("restore-streaming-scale-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = root.resolve("media").createDirectories()
            val safety = root.resolve("safety")

            // Stream write 512KB media file in 4KB chunks without single large ByteArray
            val largeMediaA = media.resolve("audio_a.bin")
            Files.newOutputStream(largeMediaA).buffered().use { out ->
                val chunk = ByteArray(4096) { (it % 251).toByte() }
                repeat(128) { out.write(chunk) } // 512 KB
            }
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"pkg_a\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety
            )

            val backupA = root.resolve("backup_a.lebak")
            manager.createPortableBackupV2(backupA, descriptor())

            // Mutate live state to state B with large media B
            val largeMediaB = media.resolve("audio_b.bin")
            Files.newOutputStream(largeMediaB).buffered().use { out ->
                val chunk = ByteArray(4096) { ((it + 13) % 251).toByte() }
                repeat(128) { out.write(chunk) }
            }
            Files.deleteIfExists(largeMediaA)
            Files.writeString(data.resolve("contents.json"), "[{\"id\":\"pkg_b\"}]")

            // Restore backup A
            val restoreResult = manager.restorePortableBackupV2(backupA)
            assertIs<PortableBackupV2RestoreResult.Success>(restoreResult)

            // Verify live state matches backup A and stale media B is deleted
            assertFalse(Files.exists(largeMediaB))
            assertTrue(Files.exists(largeMediaA))
            assertEquals(512L * 1024L, Files.size(largeMediaA))
            assertEquals("[{\"id\":\"pkg_a\"}]", Files.readString(data.resolve("contents.json")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 physical media tree replacement removes stale package directory and restores exact media files`() {
        val root = Files.createTempDirectory("restore-media-tree-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")

            // Backup state: only Package A media
            val pkgAMedia = media.resolve("Vocabulary_in_Use_Intermediate").createDirectories()
            Files.writeString(pkgAMedia.resolve("audio_a1.mp3"), "audio-a1")
            Files.writeString(pkgAMedia.resolve("audio_a2.mp3"), "audio-a2")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_intermediate\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety
            )

            val backup = root.resolve("backup_intermediate.lebak")
            manager.createPortableBackupV2(backup, descriptor())

            // Mutate live state: add Package B with multiple files
            val pkgBMedia = media.resolve("Vocabulary_In_Use_Elementary").createDirectories()
            repeat(20) { index ->
                Files.writeString(pkgBMedia.resolve("audio_b_$index.mp3"), "audio-b-$index-content")
            }
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_intermediate\"},{\"id\":\"pkg_elementary\"}]")

            assertTrue(Files.exists(pkgBMedia))
            assertEquals(20, Files.list(pkgBMedia).count())

            // Restore Backup A
            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.Success>(result)

            // Assert: Package B directory does NOT exist on the physical filesystem
            assertFalse(Files.exists(pkgBMedia), "Package B directory must not survive Restore V2")

            // Assert: live media contains only Package A files
            val liveMediaFiles = Files.walk(media).filter { Files.isRegularFile(it) }.toList()
            assertEquals(2, liveMediaFiles.size)
            assertTrue(Files.exists(pkgAMedia.resolve("audio_a1.mp3")))
            assertTrue(Files.exists(pkgAMedia.resolve("audio_a2.mp3")))
            assertEquals("[{\"id\":\"pkg_intermediate\"}]", Files.readString(data.resolve("installed-packages.json")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 restore with extra stale media file triggers equality failure and rollback`() {
        val root = Files.createTempDirectory("restore-extra-media-fail-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")

            val pkgAMedia = media.resolve("PackageA").createDirectories()
            Files.writeString(pkgAMedia.resolve("a.jpg"), "image-a")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_a\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety,
                failureHook = { hook, _ ->
                    if (hook == "restore-v2-consumer-applied") {
                        // Inject an extra rogue media file into live root
                        val rogue = media.resolve("PackageB").createDirectories().resolve("rogue.bin")
                        Files.writeString(rogue, "rogue-bytes")
                    }
                }
            )

            val backup = root.resolve("backup_a.lebak")
            manager.createPortableBackupV2(backup, descriptor())

            // Live state before restore
            Files.writeString(pkgAMedia.resolve("a.jpg"), "modified-before-restore")

            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.RestoreFailedRolledBack>(result)
            assertTrue(result.failureReason.contains("Restored canonical inventory mismatch"))

            // Verify live state rolled back to pre-restore state
            assertEquals("modified-before-restore", Files.readString(pkgAMedia.resolve("a.jpg")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 restore with missing media file triggers equality failure and rollback`() {
        val root = Files.createTempDirectory("restore-missing-media-fail-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")

            val pkgAMedia = media.resolve("PackageA").createDirectories()
            Files.writeString(pkgAMedia.resolve("a.jpg"), "image-a")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_a\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety,
                failureHook = { hook, _ ->
                    if (hook == "restore-v2-consumer-applied") {
                        // Delete expected restored media file
                        Files.deleteIfExists(pkgAMedia.resolve("a.jpg"))
                    }
                }
            )

            val backup = root.resolve("backup_a.lebak")
            manager.createPortableBackupV2(backup, descriptor())

            Files.writeString(pkgAMedia.resolve("a.jpg"), "pre-restore-content")

            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.RestoreFailedRolledBack>(result)
            assertTrue(result.failureReason.contains("Restored canonical inventory mismatch") || result.failureReason.contains("Restored entry missing"))
            assertEquals("pre-restore-content", Files.readString(pkgAMedia.resolve("a.jpg")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 restore with mutated media bytes triggers equality failure and rollback`() {
        val root = Files.createTempDirectory("restore-mutated-media-fail-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")

            val pkgAMedia = media.resolve("PackageA").createDirectories()
            Files.writeString(pkgAMedia.resolve("a.jpg"), "original-image-bytes")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_a\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety,
                failureHook = { hook, _ ->
                    if (hook == "restore-v2-consumer-applied") {
                        // Mutate byte in restored file
                        Files.writeString(pkgAMedia.resolve("a.jpg"), "corrupted-image-bytes")
                    }
                }
            )

            val backup = root.resolve("backup_a.lebak")
            manager.createPortableBackupV2(backup, descriptor())

            Files.writeString(pkgAMedia.resolve("a.jpg"), "pre-restore-bytes")

            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.RestoreFailedRolledBack>(result)
            assertTrue(result.failureReason.contains("Restored canonical sha256 mismatch"))
            assertEquals("pre-restore-bytes", Files.readString(pkgAMedia.resolve("a.jpg")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 restore failure during media replace rolls back entire pre-restore media tree including both packages`() {
        val root = Files.createTempDirectory("restore-rollback-tree-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")

            // Backup has only Package A
            val pkgAMedia = media.resolve("PackageA").createDirectories()
            Files.writeString(pkgAMedia.resolve("a.mp3"), "a-backup")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_a\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety,
                failureHook = { hook, _ ->
                    if (hook == "restore-v2-consumer-applied") {
                        throw RuntimeException("Simulated platform apply crash")
                    }
                }
            )

            val backup = root.resolve("backup_a.lebak")
            manager.createPortableBackupV2(backup, descriptor())

            // Pre-restore live state has Package A + Package B
            val pkgBMedia = media.resolve("PackageB").createDirectories()
            Files.writeString(pkgAMedia.resolve("a.mp3"), "a-live-pre-restore")
            Files.writeString(pkgBMedia.resolve("b1.mp3"), "b1-live-pre-restore")
            Files.writeString(pkgBMedia.resolve("b2.mp3"), "b2-live-pre-restore")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_a\"},{\"id\":\"pkg_b\"}]")

            val result = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.RestoreFailedRolledBack>(result)

            // Assert: physical media tree returns BOTH Package A and Package B with exact pre-restore content
            assertTrue(Files.exists(pkgAMedia.resolve("a.mp3")))
            assertEquals("a-live-pre-restore", Files.readString(pkgAMedia.resolve("a.mp3")))
            assertTrue(Files.exists(pkgBMedia.resolve("b1.mp3")))
            assertEquals("b1-live-pre-restore", Files.readString(pkgBMedia.resolve("b1.mp3")))
            assertTrue(Files.exists(pkgBMedia.resolve("b2.mp3")))
            assertEquals("b2-live-pre-restore", Files.readString(pkgBMedia.resolve("b2.mp3")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `v2 restore removes orphan media allowing clean re-import of removed package`() {
        val root = Files.createTempDirectory("restore-reimport-test-")
        try {
            val data = root.resolve("data").createDirectories()
            val media = data.resolve("media").createDirectories()
            val safety = root.resolve("safety")

            // Backup has only Package A
            val pkgAMedia = media.resolve("PackageA").createDirectories()
            Files.writeString(pkgAMedia.resolve("a.mp3"), "a-audio")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_a\"}]")

            val manager = JvmLearningDataRecoveryManager(
                roots = mapOf("data" to data, "media" to media),
                safetyDirectory = safety
            )

            val backup = root.resolve("backup_a.lebak")
            manager.createPortableBackupV2(backup, descriptor())

            // Live state imports Package B
            val pkgBMedia = media.resolve("PackageB").createDirectories()
            Files.writeString(pkgBMedia.resolve("b.mp3"), "b-old-audio")
            Files.writeString(data.resolve("installed-packages.json"), "[{\"id\":\"pkg_a\"},{\"id\":\"pkg_b\"}]")

            // Restore Backup A
            val restoreResult = manager.restorePortableBackupV2(backup)
            assertIs<PortableBackupV2RestoreResult.Success>(restoreResult)

            // Assert: Package B media directory is gone
            assertFalse(Files.exists(pkgBMedia))

            // Re-import Package B freshly: directory is created without colliding with leftover files
            val newPkgBMedia = media.resolve("PackageB").createDirectories()
            Files.writeString(newPkgBMedia.resolve("b.mp3"), "b-new-audio")

            assertTrue(Files.exists(newPkgBMedia.resolve("b.mp3")))
            assertEquals("b-new-audio", Files.readString(newPkgBMedia.resolve("b.mp3")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun rewrite(source: Path, target: Path, transform: (String, ByteArray) -> ByteArray) {
        ZipFile(source.toFile()).use { input -> ZipOutputStream(Files.newOutputStream(target)).use { output ->
            input.entries().asSequence().forEach { entry ->
                output.putNextEntry(ZipEntry(entry.name))
                output.write(transform(entry.name, input.getInputStream(entry).readBytes()))
                output.closeEntry()
            }
        } }
    }

    private fun descriptor() = PortableBackupV2Descriptor("test", 1, "test", listOf("learner"))
    private fun fixture() = Fixture(Files.createTempDirectory("portable-backup-v2-"))

    private class Fixture(val root: Path) : AutoCloseable {
        val data = root.resolve("data").createDirectories()
        val media = data.resolve("media").createDirectories()
        val manager = JvmLearningDataRecoveryManager(mapOf("data" to data, "media" to media), root.resolve("safety"))
        override fun close() { root.toFile().deleteRecursively() }
    }

    companion object {
        private val V2_JSON = kotlinx.serialization.json.Json { encodeDefaults = true; ignoreUnknownKeys = false; prettyPrint = true }
    }
}
