package vn.loi.learning.application.sync

import vn.loi.learning.domain.sync.protocol.RemoteSyncChange
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.domain.sync.protocol.SyncDeviceId

data class SyncSessionResult(val pushed: Int, val applied: Int, val cursor: Long)

/** Coordinates explicit sync without making the transport or Supabase an application authority. */
class SyncSessionCoordinator(
    private val state: LocalSyncStateRepository,
    private val transport: SyncTransport
) {
    fun synchronize(
        accountId: SyncAccountId,
        deviceId: SyncDeviceId,
        pageSize: Int,
        apply: (RemoteSyncChange) -> Unit
    ): SyncSessionResult {
        require(pageSize > 0) { "Sync page size must be positive." }
        val pending = state.pendingOutbox(accountId)
        val pushed = if (pending.isEmpty()) 0 else {
            val result = transport.push(pending)
            val confirmed = result.accepted.keys + result.duplicates.keys
            state.acknowledgeOutbox(accountId, confirmed)
            confirmed.size
        }

        var applied = 0
        while (true) {
            val before = state.cursor(accountId)
            val page = transport.pull(accountId, before, pageSize)
            if (page.changes.isEmpty()) break
            for (remote in page.changes) {
                apply(remote)
                val after = state.cursor(accountId)
                check(after.value >= remote.revision.value) {
                    "Sync apply did not commit the remote cursor."
                }
                transport.acknowledge(SyncAcknowledgement(accountId, deviceId, after))
                applied++
            }
            if (!page.hasMore) break
        }
        return SyncSessionResult(pushed, applied, state.cursor(accountId).value)
    }
}
