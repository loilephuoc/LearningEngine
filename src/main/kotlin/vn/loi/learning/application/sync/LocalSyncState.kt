package vn.loi.learning.application.sync

import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.sync.protocol.OutboundSyncChange
import vn.loi.learning.domain.sync.protocol.RemoteSyncChange
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.domain.sync.protocol.SyncCursor
import vn.loi.learning.domain.sync.protocol.SyncEventId

interface LocalSyncStateRepository {
    fun enqueue(change: OutboundSyncChange)
    fun pendingOutbox(accountId: SyncAccountId): List<OutboundSyncChange>
    fun acknowledgeOutbox(accountId: SyncAccountId, eventIds: Set<SyncEventId>)
    fun hasApplied(accountId: SyncAccountId, eventId: SyncEventId): Boolean
    fun recordApplied(accountId: SyncAccountId, eventId: SyncEventId, cursor: SyncCursor)
    fun cursor(accountId: SyncAccountId): SyncCursor
}

class LocalSyncCoordinator(
    private val state: LocalSyncStateRepository,
    private val transactions: TransactionRunner
) {
    fun <T> mutateAndEnqueue(change: OutboundSyncChange, mutation: () -> T): T =
        transactions.runInTransaction {
            val result = mutation()
            state.enqueue(change)
            result
        }

    fun applyOnce(remote: RemoteSyncChange, apply: (OutboundSyncChange) -> Unit): Boolean =
        transactions.runInTransaction {
            val change = remote.change
            if (state.hasApplied(change.accountId, change.eventId)) return@runInTransaction false
            apply(change)
            state.recordApplied(change.accountId, change.eventId, SyncCursor(remote.revision.value))
            true
        }
}
