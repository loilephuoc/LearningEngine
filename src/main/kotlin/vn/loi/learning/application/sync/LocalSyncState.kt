package vn.loi.learning.application.sync

import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.sync.protocol.OutboundSyncChange
import vn.loi.learning.domain.sync.protocol.RemoteSyncChange
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.domain.sync.protocol.SyncCursor
import vn.loi.learning.domain.sync.protocol.SyncEventId

data class SyncQuarantineRecord(
    val accountId: SyncAccountId,
    val eventId: SyncEventId,
    val reviewEventId: String,
    val learningItemId: String,
    val remoteRevision: Long,
    val payloadVersion: Int,
    val code: String,
    val reason: String,
    val contentId: String? = null,
    val mediaSha256: String? = null
)

data class PendingMediaApply(
    val accountId: SyncAccountId,
    val eventId: SyncEventId,
    val contentId: String,
    val slot: String,
    val sha256: String,
    val sizeBytes: Long,
    val mimeType: String,
    val remoteRevision: Long,
    val payloadVersion: Int
)

data class MediaGcCandidate(val reference: String, val sha256: String)

interface LocalSyncStateRepository {
    fun enqueue(change: OutboundSyncChange)
    fun pendingOutbox(accountId: SyncAccountId): List<OutboundSyncChange>
    fun acknowledgeOutbox(accountId: SyncAccountId, eventIds: Set<SyncEventId>)
    fun hasApplied(accountId: SyncAccountId, eventId: SyncEventId): Boolean
    fun recordApplied(accountId: SyncAccountId, eventId: SyncEventId, cursor: SyncCursor)
    fun cursor(accountId: SyncAccountId): SyncCursor
    fun recordQuarantine(record: SyncQuarantineRecord)
    fun quarantines(accountId: SyncAccountId): List<SyncQuarantineRecord>
    fun recordPendingMedia(record: PendingMediaApply)
    fun removePendingMedia(accountId: SyncAccountId, eventId: SyncEventId)
    fun pendingMedia(accountId: SyncAccountId): List<PendingMediaApply>
    fun recordMediaGcCandidate(candidate: MediaGcCandidate)
    fun removeMediaGcCandidate(reference: String)
    fun mediaGcCandidates(): List<MediaGcCandidate>
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

    fun <T> mutateAndEnqueue(mutation: () -> Pair<T, OutboundSyncChange>): T =
        transactions.runInTransaction {
            val (result, change) = mutation()
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
