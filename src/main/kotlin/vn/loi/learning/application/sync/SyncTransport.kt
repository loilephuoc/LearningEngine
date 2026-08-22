package vn.loi.learning.application.sync

import vn.loi.learning.domain.sync.protocol.OutboundSyncChange
import vn.loi.learning.domain.sync.protocol.RemoteSyncChange
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.domain.sync.protocol.SyncCursor
import vn.loi.learning.domain.sync.protocol.SyncDeviceId
import vn.loi.learning.domain.sync.protocol.SyncEventId
import vn.loi.learning.domain.sync.protocol.SyncRevision

data class PushResult(
    val accepted: Map<SyncEventId, SyncRevision>,
    val duplicates: Map<SyncEventId, SyncRevision>
)

data class PullPage(
    val changes: List<RemoteSyncChange>,
    val nextCursor: SyncCursor,
    val hasMore: Boolean
)

data class SyncAcknowledgement(
    val accountId: SyncAccountId,
    val deviceId: SyncDeviceId,
    val cursor: SyncCursor
)

interface SyncTransport {
    fun push(changes: List<OutboundSyncChange>): PushResult

    fun pull(accountId: SyncAccountId, after: SyncCursor, limit: Int): PullPage

    fun acknowledge(acknowledgement: SyncAcknowledgement)
}
