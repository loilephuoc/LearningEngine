package vn.loi.learning.application.sync

import vn.loi.learning.application.port.RemoteMediaBlobTransport
import vn.loi.learning.domain.sync.protocol.DeltaOperation
import vn.loi.learning.domain.sync.protocol.MediaDelta
import vn.loi.learning.domain.sync.protocol.SyncAccountId

sealed interface MediaBlobFetchResult {
    data object Ready : MediaBlobFetchResult
    data object Missing : MediaBlobFetchResult
}

/** Downloads into the existing media validator/staging boundary; it never applies Content itself. */
class MediaBlobTransferCoordinator(
    private val remote: RemoteMediaBlobTransport,
    private val media: MediaDeltaSyncService
) {
    fun fetch(accountId: SyncAccountId, delta: MediaDelta): MediaBlobFetchResult {
        require(delta.operation == DeltaOperation.SET) { "Only media SET deltas require a blob." }
        val bytes = remote.download(accountId, requireNotNull(delta.sha256)) ?: return MediaBlobFetchResult.Missing
        media.provideBlob(delta, bytes)
        return MediaBlobFetchResult.Ready
    }
}
