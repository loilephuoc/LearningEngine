package vn.loi.learning.infrastructure.sync

import vn.loi.learning.application.sync.PullPage
import vn.loi.learning.application.sync.PushResult
import vn.loi.learning.application.sync.SyncAcknowledgement
import vn.loi.learning.application.sync.SyncTransport
import vn.loi.learning.domain.sync.protocol.IdempotencyKey
import vn.loi.learning.domain.sync.protocol.OutboundSyncChange
import vn.loi.learning.domain.sync.protocol.RemoteSyncChange
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.domain.sync.protocol.SyncCursor
import vn.loi.learning.domain.sync.protocol.SyncDeviceId
import vn.loi.learning.domain.sync.protocol.SyncEventId
import vn.loi.learning.domain.sync.protocol.SyncRevision

class InMemorySyncTransport : SyncTransport {
    private val changesByAccount = linkedMapOf<SyncAccountId, MutableList<RemoteSyncChange>>()
    private val events = mutableMapOf<Pair<SyncAccountId, SyncEventId>, RemoteSyncChange>()
    private val idempotencyKeys = mutableMapOf<Pair<SyncAccountId, IdempotencyKey>, RemoteSyncChange>()
    private val acknowledgements = mutableMapOf<Pair<SyncAccountId, SyncDeviceId>, SyncCursor>()

    override fun push(changes: List<OutboundSyncChange>): PushResult {
        val accepted = linkedMapOf<SyncEventId, SyncRevision>()
        val duplicates = linkedMapOf<SyncEventId, SyncRevision>()
        changes.forEach { candidate ->
            val eventKey = candidate.accountId to candidate.eventId
            val idempotencyKey = candidate.accountId to candidate.idempotencyKey
            val existing = events[eventKey] ?: idempotencyKeys[idempotencyKey]
            if (existing != null) {
                require(existing.change == candidate) {
                    "An event or idempotency key cannot identify different sync changes."
                }
                duplicates[candidate.eventId] = existing.revision
            } else {
                val accountChanges = changesByAccount.getOrPut(candidate.accountId, ::mutableListOf)
                val remote = RemoteSyncChange(SyncRevision(accountChanges.size.toLong() + 1L), candidate)
                accountChanges += remote
                events[eventKey] = remote
                idempotencyKeys[idempotencyKey] = remote
                accepted[candidate.eventId] = remote.revision
            }
        }
        return PushResult(accepted, duplicates)
    }

    override fun pull(accountId: SyncAccountId, after: SyncCursor, limit: Int): PullPage {
        require(limit > 0) { "Pull limit must be positive." }
        val available = changesByAccount[accountId].orEmpty().filter { it.revision.value > after.value }
        val page = available.take(limit)
        return PullPage(
            changes = page,
            nextCursor = page.lastOrNull()?.let { SyncCursor(it.revision.value) } ?: after,
            hasMore = available.size > page.size
        )
    }

    override fun acknowledge(acknowledgement: SyncAcknowledgement) {
        val latestRevision = changesByAccount[acknowledgement.accountId]?.size?.toLong() ?: 0L
        require(acknowledgement.cursor.value <= latestRevision) {
            "Acknowledgement cursor cannot exceed the latest account revision."
        }
        val key = acknowledgement.accountId to acknowledgement.deviceId
        val current = acknowledgements[key] ?: SyncCursor.START
        if (acknowledgement.cursor > current) acknowledgements[key] = acknowledgement.cursor
    }

    fun acknowledgedCursor(accountId: SyncAccountId, deviceId: SyncDeviceId): SyncCursor =
        acknowledgements[accountId to deviceId] ?: SyncCursor.START
}
