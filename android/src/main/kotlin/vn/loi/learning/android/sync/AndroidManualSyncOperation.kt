package vn.loi.learning.android.sync

import java.nio.file.Files
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.port.RemoteMediaBlobTransport
import vn.loi.learning.application.sync.*
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.LearningApplicationContext

data class AndroidSyncSummary(
    val pushed: Int,
    val applied: Int,
    val uploadedMedia: Int,
    val downloadedMedia: Int,
    val conflicts: Int,
    val pendingMedia: Int,
    val cursor: Long
)

fun interface AndroidManualSyncOperation {
    fun run(accountId: SyncAccountId, deviceId: SyncDeviceId): AndroidSyncSummary
}

class DefaultAndroidManualSyncOperation(
    private val context: LearningApplicationContext,
    private val mediaStorage: ContentMediaStorage,
    private val transport: SyncTransport,
    private val blobs: RemoteMediaBlobTransport
) : AndroidManualSyncOperation {
    override fun run(accountId: SyncAccountId, deviceId: SyncDeviceId): AndroidSyncSummary {
        val state = requireNotNull(context.localSyncStateRepository)
        var uploaded = 0
        state.pendingOutbox(accountId).forEach { change ->
            ensureActive()
            val delta = change.delta as? MediaDelta ?: return@forEach
            if (delta.operation != DeltaOperation.SET) return@forEach
            val path = requireNotNull(mediaStorage.resolve(requireNotNull(delta.mediaReference))) { "SYNC_MEDIA_LOCAL_BLOB_MISSING" }
            blobs.upload(accountId, requireNotNull(delta.sha256), requireNotNull(delta.mimeType), Files.readAllBytes(path))
            uploaded++
        }
        var downloaded = 0
        var conflicts = 0
        var pending = 0
        val result = SyncSessionCoordinator(state, transport).synchronize(accountId, deviceId, 100) { remote ->
            ensureActive()
            when (remote.change.delta) {
                is ContentFieldDelta -> if (context.contentFieldSyncService!!.applyRemote(remote) is ContentDeltaApplyResult.Conflict) conflicts++
                is ReviewEventDelta -> if (context.reviewDeltaSyncService!!.applyRemote(remote) is ReviewDeltaApplyResult.Quarantined) conflicts++
                is MediaDelta -> {
                    val delta = remote.change.delta as MediaDelta
                    if (delta.operation == DeltaOperation.SET) {
                        val bytes = blobs.download(accountId, requireNotNull(delta.sha256))
                        if (bytes == null) {
                            context.mediaDeltaSyncService!!.applyRemote(remote)
                            pending++
                            throw AndroidPendingRemoteMediaException()
                        }
                        context.mediaDeltaSyncService!!.provideBlob(delta, bytes)
                        downloaded++
                    }
                    if (context.mediaDeltaSyncService!!.applyRemote(remote) is MediaDeltaApplyResult.Quarantined) conflicts++
                }
            }
        }
        return AndroidSyncSummary(result.pushed, result.applied, uploaded, downloaded, conflicts, pending, result.cursor)
    }

    private fun ensureActive() {
        if (Thread.currentThread().isInterrupted) throw java.util.concurrent.CancellationException("Android manual sync cancelled.")
    }
}

class AndroidPendingRemoteMediaException : IllegalStateException("SYNC_MEDIA_REMOTE_BLOB_PENDING")
