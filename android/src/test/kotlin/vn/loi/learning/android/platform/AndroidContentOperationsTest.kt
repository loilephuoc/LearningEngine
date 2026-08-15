package vn.loi.learning.android.platform

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager

class AndroidContentOperationsTest {
    @Test fun `persisted importer and Android resolver share canonical data media root`() {
        fixture().use { fixture ->
            assertEquals(fixture.directories.dataDirectory.resolve("media"), fixture.directories.mediaDirectory)
            val reference="package/images/bed.png"
            val target=fixture.directories.mediaDirectory.resolve(reference)
            Files.createDirectories(target.parent);Files.write(target,byteArrayOf(1,2,3))
            assertEquals(target,fixture.graph.media.resolve(reference))
        }
    }
    @Test fun `duplicate operation request reuses active identity`() {
        fixture().use { fixture ->
            val operations = fixture.operations()
            assertEquals(operations.newOperation(AndroidOperationKind.IMPORT).id, operations.newOperation(AndroidOperationKind.IMPORT).id)
        }
    }

    @Test fun `process recreation exposes interrupted operation instead of replaying it`() {
        fixture().use { fixture ->
            val state = fixture.operations().recoverInterrupted("op-1", AndroidOperationKind.RESTORE)
            assertIs<AndroidContentOperationState.Failed>(state)
            assertIs<AndroidContentFailure.Interrupted>(state.failure)
        }
    }

    @Test fun `missing import URI is typed unavailable and staging is cleaned`() = runTest {
        fixture().use { fixture ->
            val operations = fixture.operations(StandardTestDispatcher(testScheduler))
            val operation = operations.newOperation(AndroidOperationKind.IMPORT)
            val state = operations.import(operation, "missing.opd3") { null }
            assertIs<AndroidContentOperationState.Failed>(state)
            assertIs<AndroidContentFailure.Unavailable>(state.failure)
            assertTrue(Files.list(fixture.directories.importDirectory).use { it.toList().isEmpty() })
        }
    }

    @Test fun `backup closes destination and excludes temporary staging`() = runTest {
        fixture().use { fixture ->
            Files.writeString(fixture.directories.dataDirectory.resolve("content.json"), "durable")
            val output = TrackingOutputStream()
            val operations = fixture.operations(StandardTestDispatcher(testScheduler))
            val state = operations.backup(operations.newOperation(AndroidOperationKind.BACKUP)) { output }
            assertIs<AndroidContentOperationState.Succeeded>(state)
            assertTrue(output.closed)
            assertTrue(output.size() > 0)
            assertFalse(Files.list(fixture.directories.rootDirectory).use { stream -> stream.anyMatch { it.fileName.toString().startsWith(".android-backup-") } })
        }
    }

    @Test fun `restore validates before mutation and corrupt input preserves data`() = runTest {
        fixture().use { fixture ->
            val durable = fixture.directories.dataDirectory.resolve("content.json")
            Files.writeString(durable, "current")
            val operations = fixture.operations(StandardTestDispatcher(testScheduler))
            val state = operations.restore(operations.newOperation(AndroidOperationKind.RESTORE)) {
                ByteArrayInputStream("not-a-backup".toByteArray())
            }
            assertIs<AndroidContentOperationState.Failed>(state)
            assertIs<AndroidContentFailure.Restore>(state.failure)
            assertEquals("current", Files.readString(durable))
        }
    }

    @Test fun `backup round trip restores persisted data`() = runTest {
        fixture().use { fixture ->
            val durable = fixture.directories.dataDirectory.resolve("content.json")
            Files.writeString(durable, "before")
            val backup = ByteArrayOutputStream()
            val operations = fixture.operations(StandardTestDispatcher(testScheduler))
            assertIs<AndroidContentOperationState.Succeeded>(operations.backup(operations.newOperation(AndroidOperationKind.BACKUP)) { backup })
            Files.writeString(durable, "after")
            val restore = fixture.operations(StandardTestDispatcher(testScheduler))
            assertIs<AndroidContentOperationState.Succeeded>(restore.restore(restore.newOperation(AndroidOperationKind.RESTORE)) { ByteArrayInputStream(backup.toByteArray()) })
            assertEquals("before", Files.readString(durable))
        }
    }

    @Test fun `Android recovery round trip prevalidates overlapping media aliases portably`() = runTest {
        fixture().use { fixture ->
            val media = fixture.directories.mediaDirectory.resolve("package/large.bin")
            Files.createDirectories(media.parent)
            val expected = ByteArray(64 * 1024 * 2 + 11) { index -> (index % 239).toByte() }
            Files.write(media, expected)
            val backup = ByteArrayOutputStream()
            val operations = fixture.operations(StandardTestDispatcher(testScheduler))
            assertIs<AndroidContentOperationState.Succeeded>(
                operations.backup(operations.newOperation(AndroidOperationKind.BACKUP)) { backup }
            )
            Files.write(media, byteArrayOf(9))

            val restore = fixture.operations(StandardTestDispatcher(testScheduler))
            assertIs<AndroidContentOperationState.Succeeded>(
                restore.restore(restore.newOperation(AndroidOperationKind.RESTORE)) { ByteArrayInputStream(backup.toByteArray()) }
            )
            assertContentEquals(expected, Files.readAllBytes(media))
        }
    }

    private fun fixture(): Fixture {
        val root = createTempDirectory("android-content-test")
        val directories = AndroidPlatformDirectories(root.resolve("data"), root.resolve("data/media"), root.resolve("imports")).also { it.create() }
        val graph = AndroidApplicationGraph(
            LearningApplicationFactory.createPersisted(directories.dataDirectory),
            JvmContentMediaStorage(directories.mediaDirectory),
            JvmLearningDataRecoveryManager(mapOf("data" to directories.dataDirectory, "media" to directories.mediaDirectory), directories.backupDirectory),
            directories
        )
        return Fixture(root, directories, graph)
    }

    private class Fixture(val root: java.nio.file.Path, val directories: AndroidPlatformDirectories, val graph: AndroidApplicationGraph) : AutoCloseable {
        fun operations(dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined) = AndroidContentOperations(graph, dispatcher) { "op-1" }
        override fun close() { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
    private class TrackingOutputStream : ByteArrayOutputStream() { var closed = false; override fun close() { closed = true; super.close() } }
}
