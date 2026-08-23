package vn.loi.learning.infrastructure.sync.supabase

import java.net.URI
import java.io.IOException
import java.security.MessageDigest
import kotlin.math.min
import vn.loi.learning.application.port.MediaBlobUploadResult
import vn.loi.learning.application.port.RemoteMediaBlobTransport
import vn.loi.learning.domain.sync.protocol.SyncAccountId

class SupabaseMediaBlobTransport(
    private val configuration: SupabaseConfiguration,
    private val sessions: SupabaseSessionProvider,
    private val http: SupabaseHttpClient = UrlConnectionSupabaseHttpClient(),
    private val delay: (Long) -> Unit = { Thread.sleep(it) }
) : RemoteMediaBlobTransport {
    override fun upload(accountId: SyncAccountId, sha256: String, mimeType: String, bytes: ByteArray): MediaBlobUploadResult {
        validate(accountId, sha256)
        require(bytes.isNotEmpty()) { "Media blob must not be empty." }
        require(digest(bytes) == sha256) { "Media blob checksum mismatch." }
        val path = objectPath(accountId, sha256)
        when (request("HEAD", path).status) {
            in 200..299 -> return MediaBlobUploadResult.ALREADY_PRESENT
            404 -> Unit
            else -> throw SupabaseTransportException("SYNC_SUPABASE_STORAGE_PROBE_FAILED", false)
        }
        val response = request("POST", path, bytes, mimeType)
        return when (response.status) {
            in 200..299 -> MediaBlobUploadResult.UPLOADED
            409 -> MediaBlobUploadResult.ALREADY_PRESENT
            else -> throw storageError(response.status)
        }
    }

    override fun download(accountId: SyncAccountId, sha256: String): ByteArray? {
        validate(accountId, sha256)
        val response = request("GET", objectPath(accountId, sha256))
        return when (response.status) {
            in 200..299 -> response.body.also {
                if (digest(it) != sha256) throw SupabaseTransportException("SYNC_SUPABASE_STORAGE_CHECKSUM_MISMATCH", false)
            }
            404 -> null
            else -> throw storageError(response.status)
        }
    }

    private fun validate(accountId: SyncAccountId, sha256: String) {
        require(sessions.currentSession()?.accountId == accountId) { "Blob account does not match authenticated session." }
        require(accountId.value.matches(UUID)) { "Supabase account ID must be a UUID." }
        require(sha256.matches(SHA)) { "Media identity must be a lowercase SHA-256 checksum." }
    }

    private fun objectPath(accountId: SyncAccountId, sha256: String) =
        "/storage/v1/object/${configuration.mediaBucket}/${accountId.value}/$sha256"

    private fun request(method: String, path: String, body: ByteArray? = null, mimeType: String? = null): SupabaseHttpResponse {
        var attempt = 0
        var authenticationRetried = false
        while (true) {
            check(!Thread.currentThread().isInterrupted) { "Sync request cancelled." }
            attempt++
            val session = sessions.currentSession() ?: throw SupabaseTransportException("SYNC_SUPABASE_AUTHENTICATION_REQUIRED", false, 401)
            val headers = mutableMapOf("apikey" to configuration.publishableKey, "Authorization" to "Bearer ${session.accessToken}")
            mimeType?.let { headers["Content-Type"] = it }
            val response = try {
                http.execute(SupabaseHttpRequest(method, URI(configuration.baseUri.toString() + path), headers, body, configuration.requestTimeoutMillis))
            } catch (error: IOException) {
                if (attempt >= configuration.retry.maxAttempts) {
                    throw SupabaseTransportException("SYNC_SUPABASE_NETWORK_ERROR", true, cause = error)
                }
                val exponential = configuration.retry.initialDelayMillis * (1L shl (attempt - 1).coerceAtMost(20))
                delay(min(exponential, configuration.retry.maxDelayMillis))
                continue
            }
            if (response.status == 401 && !authenticationRetried && sessions is RefreshableSupabaseSessionProvider) {
                authenticationRetried = true
                sessions.refreshAfterUnauthorized(session.accessToken)
                    ?: throw SupabaseTransportException("SYNC_SUPABASE_AUTHENTICATION_REQUIRED", false, 401)
                attempt--
                continue
            }
            val retryable = response.status == 408 || response.status == 429 || response.status >= 500
            if (!retryable || attempt >= configuration.retry.maxAttempts) return response
            val exponential = configuration.retry.initialDelayMillis * (1L shl (attempt - 1).coerceAtMost(20))
            delay(min(exponential, configuration.retry.maxDelayMillis))
        }
    }

    private fun storageError(status: Int) = SupabaseTransportException(
        when (status) { 401 -> "SYNC_SUPABASE_AUTHENTICATION_FAILED"; 403 -> "SYNC_SUPABASE_AUTHORIZATION_FAILED"; else -> "SYNC_SUPABASE_STORAGE_ERROR" },
        status == 408 || status == 429 || status >= 500,
        status
    )
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        val SHA = Regex("[0-9a-f]{64}")
        val UUID = Regex("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    }
}
