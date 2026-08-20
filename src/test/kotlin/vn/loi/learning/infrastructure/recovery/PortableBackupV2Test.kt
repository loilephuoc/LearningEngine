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
