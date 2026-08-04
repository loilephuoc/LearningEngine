package vn.loi.learning.android.acceptance

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import vn.loi.learning.android.platform.AndroidContentFailure
import vn.loi.learning.android.platform.AndroidContentOperationState
import vn.loi.learning.android.platform.AndroidOperationKind
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AndroidStorageFaultAcceptanceTest {
    @Test
    fun `provider permission failure is typed and import staging is removed`() = runTest {
        AndroidAcceptanceFixture.create().use { fixture ->
            val operations = fixture.contentOperations(StandardTestDispatcher(testScheduler))
            val operation = operations.newOperation(AndroidOperationKind.IMPORT)

            val result = operations.import(operation, "unsafe.opd3") {
                throw SecurityException("private provider detail")
            }

            assertIs<AndroidContentFailure.Permission>(
                assertIs<AndroidContentOperationState.Failed>(result).failure
            )
            assertTrue(Files.list(fixture.directories.importDirectory).use { it.toList().isEmpty() })
        }
    }

    @Test
    fun `destination failure is typed and backup staging is removed`() = runTest {
        AndroidAcceptanceFixture.create().use { fixture ->
            Files.writeString(fixture.directories.dataDirectory.resolve("state.json"), "durable")
            val operations = fixture.contentOperations(StandardTestDispatcher(testScheduler))
            val operation = operations.newOperation(AndroidOperationKind.BACKUP)

            val result = operations.backup(operation) { ThrowingOutputStream() }

            assertIs<AndroidContentFailure.Backup>(
                assertIs<AndroidContentOperationState.Failed>(result).failure
            )
            assertTrue(Files.list(fixture.directories.rootDirectory).use { stream ->
                stream.noneMatch { it.fileName.toString().startsWith(".android-backup-") }
            })
        }
    }

    @Test
    fun `duplicate successful callback cannot write backup twice`() = runTest {
        AndroidAcceptanceFixture.create().use { fixture ->
            Files.writeString(fixture.directories.dataDirectory.resolve("state.json"), "durable")
            val operations = fixture.contentOperations(StandardTestDispatcher(testScheduler))
            val operation = operations.newOperation(AndroidOperationKind.BACKUP)
            var destinationsOpened = 0

            assertIs<AndroidContentOperationState.Succeeded>(operations.backup(operation) {
                destinationsOpened++
                ByteArrayOutputStream()
            })
            assertIs<AndroidContentOperationState.Idle>(operations.backup(operation) {
                destinationsOpened++
                ByteArrayOutputStream()
            })
            assertEquals(1, destinationsOpened)
        }
    }

    @Test
    fun `corrupt restore cannot mutate existing durable state`() = runTest {
        AndroidAcceptanceFixture.create().use { fixture ->
            val state = fixture.directories.dataDirectory.resolve("state.json")
            Files.writeString(state, "before")
            val operations = fixture.contentOperations(StandardTestDispatcher(testScheduler))

            val result = operations.restore(operations.newOperation(AndroidOperationKind.RESTORE)) {
                ByteArrayInputStream("corrupt-checksum".toByteArray())
            }

            assertIs<AndroidContentFailure.Restore>(
                assertIs<AndroidContentOperationState.Failed>(result).failure
            )
            assertEquals("before", Files.readString(state))
        }
    }

    @Test
    fun `operation identity state machine is deterministic across interruption and retry`() {
        AndroidAcceptanceFixture.create().use { fixture ->
            val operations = fixture.contentOperations()
            val first = operations.newOperation(AndroidOperationKind.IMPORT)
            val duplicate = operations.newOperation(AndroidOperationKind.IMPORT)
            assertEquals(first, duplicate)
            assertIs<AndroidContentFailure.Interrupted>(
                assertIs<AndroidContentOperationState.Failed>(
                    operations.recoverInterrupted(first.id, first.kind)
                ).failure
            )
        }
    }

    private class ThrowingOutputStream : OutputStream() {
        override fun write(value: Int) = throw IOException("simulated destination failure")
    }
}
