package vn.loi.learning.infrastructure.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.application.sync.SyncAcknowledgement
import vn.loi.learning.domain.sync.protocol.ContentField
import vn.loi.learning.domain.sync.protocol.ContentFieldDelta
import vn.loi.learning.domain.sync.protocol.DeltaOperation
import vn.loi.learning.domain.sync.protocol.IdempotencyKey
import vn.loi.learning.domain.sync.protocol.OutboundSyncChange
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.domain.sync.protocol.SyncCursor
import vn.loi.learning.domain.sync.protocol.SyncDeviceId
import vn.loi.learning.domain.sync.protocol.SyncEntityId
import vn.loi.learning.domain.sync.protocol.SyncEventId

class InMemorySyncTransportTest {
    private val account = SyncAccountId("learner-1")
    private val device = SyncDeviceId("desktop-1")

    private fun change(index: Int) = OutboundSyncChange(
        accountId = account,
        eventId = SyncEventId("event-$index"),
        idempotencyKey = IdempotencyKey("key-$index"),
        sourceDeviceId = device,
        entityId = SyncEntityId("content-$index"),
        delta = ContentFieldDelta(ContentField.QUESTION, DeltaOperation.SET, "value-$index")
    )

    @Test
    fun `pull is revision ordered and paginated from an exclusive cursor`() {
        val transport = InMemorySyncTransport()
        transport.push(listOf(change(1), change(2), change(3)))

        val first = transport.pull(account, SyncCursor.START, 2)
        val second = transport.pull(account, first.nextCursor, 2)

        assertEquals(listOf(1L, 2L), first.changes.map { it.revision.value })
        assertTrue(first.hasMore)
        assertEquals(listOf(3L), second.changes.map { it.revision.value })
        assertFalse(second.hasMore)
        assertEquals(SyncCursor(3), second.nextCursor)
    }

    @Test
    fun `retry returns the original revision without duplicating an event`() {
        val transport = InMemorySyncTransport()
        val original = change(1)

        val accepted = transport.push(listOf(original))
        val retry = transport.push(listOf(original))

        assertEquals(1L, accepted.accepted.getValue(original.eventId).value)
        assertTrue(retry.accepted.isEmpty())
        assertEquals(1L, retry.duplicates.getValue(original.eventId).value)
        assertEquals(1, transport.pull(account, SyncCursor.START, 10).changes.size)
    }

    @Test
    fun `reusing identity for a different change is rejected`() {
        val transport = InMemorySyncTransport()
        val original = change(1)
        transport.push(listOf(original))

        assertFailsWith<IllegalArgumentException> {
            transport.push(listOf(original.copy(entityId = SyncEntityId("different"))))
        }
    }

    @Test
    fun `empty second pull is a no-op and acknowledgement is monotonic`() {
        val transport = InMemorySyncTransport()
        transport.push(listOf(change(1)))
        val first = transport.pull(account, SyncCursor.START, 10)
        transport.acknowledge(SyncAcknowledgement(account, device, first.nextCursor))
        transport.acknowledge(SyncAcknowledgement(account, device, SyncCursor.START))

        val second = transport.pull(account, first.nextCursor, 10)
        assertTrue(second.changes.isEmpty())
        assertEquals(first.nextCursor, second.nextCursor)
        assertEquals(SyncCursor(1), transport.acknowledgedCursor(account, device))
    }

    @Test
    fun `account streams and revisions are isolated`() {
        val transport = InMemorySyncTransport()
        val otherAccount = SyncAccountId("learner-2")
        transport.push(listOf(change(1), change(2).copy(accountId = otherAccount)))

        assertEquals(listOf(1L), transport.pull(account, SyncCursor.START, 10).changes.map { it.revision.value })
        assertEquals(listOf(1L), transport.pull(otherAccount, SyncCursor.START, 10).changes.map { it.revision.value })
    }
}
