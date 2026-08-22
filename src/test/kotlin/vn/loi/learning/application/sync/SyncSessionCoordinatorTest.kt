package vn.loi.learning.application.sync

import java.nio.file.Files
import kotlin.test.*
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.persistence.json.JsonLocalSyncStateRepository
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner

class SyncSessionCoordinatorTest {
    private val account = SyncAccountId("account")
    private val device = SyncDeviceId("desktop")
    private val change = OutboundSyncChange(account, SyncEventId("event"), IdempotencyKey("key"), device, SyncEntityId("content"), delta = ContentFieldDelta(ContentField.QUESTION, DeltaOperation.SET, "new"))

    @Test fun `server ACK happens only after transactional local apply`() {
        val root = Files.createTempDirectory("sync-session-")
        try {
            val state = JsonLocalSyncStateRepository(root.resolve("sync.json"))
            val local = LocalSyncCoordinator(state, JsonFileTransactionRunner(listOf(root.resolve("sync.json"))))
            val transport = RecordingTransport(RemoteSyncChange(SyncRevision(1), change))
            SyncSessionCoordinator(state, transport).synchronize(account, device, 10) { remote ->
                assertTrue(transport.acks.isEmpty())
                local.applyOnce(remote) { }
            }
            assertEquals(listOf(1L), transport.acks.map { it.cursor.value })
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `failed apply leaves cursor and server ACK unchanged for restart retry`() {
        val root = Files.createTempDirectory("sync-session-fail-")
        try {
            val state = JsonLocalSyncStateRepository(root.resolve("sync.json"))
            val transport = RecordingTransport(RemoteSyncChange(SyncRevision(1), change))
            assertFailsWith<IllegalStateException> {
                SyncSessionCoordinator(state, transport).synchronize(account, device, 10) { error("apply failed") }
            }
            assertEquals(SyncCursor.START, state.cursor(account))
            assertTrue(transport.acks.isEmpty())
        } finally { root.toFile().deleteRecursively() }
    }

    private class RecordingTransport(private val remote: RemoteSyncChange) : SyncTransport {
        val acks = mutableListOf<SyncAcknowledgement>()
        override fun push(changes: List<OutboundSyncChange>) = PushResult(emptyMap(), emptyMap())
        override fun pull(accountId: SyncAccountId, after: SyncCursor, limit: Int) = if (after == SyncCursor.START) PullPage(listOf(remote), SyncCursor(1), false) else PullPage(emptyList(), after, false)
        override fun acknowledge(acknowledgement: SyncAcknowledgement) { acks += acknowledgement }
    }
}
