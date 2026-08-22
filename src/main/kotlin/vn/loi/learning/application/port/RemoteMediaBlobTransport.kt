package vn.loi.learning.application.port

import vn.loi.learning.domain.sync.protocol.SyncAccountId

enum class MediaBlobUploadResult { UPLOADED, ALREADY_PRESENT }

interface RemoteMediaBlobTransport {
    fun upload(accountId: SyncAccountId, sha256: String, mimeType: String, bytes: ByteArray): MediaBlobUploadResult
    fun download(accountId: SyncAccountId, sha256: String): ByteArray?
}
