package vn.loi.learning.infrastructure.sync.supabase

import java.io.IOException
import java.net.URI
import kotlin.math.min
import kotlinx.serialization.json.*
import vn.loi.learning.application.sync.*
import vn.loi.learning.domain.sync.protocol.*

class SupabaseSyncTransport(
    private val configuration: SupabaseConfiguration,
    private val sessions: SupabaseSessionProvider,
    private val http: SupabaseHttpClient = UrlConnectionSupabaseHttpClient(),
    private val codec: SyncWireCodec = SyncWireCodec(),
    private val delay: (Long) -> Unit = { Thread.sleep(it) }
) : SyncTransport {
    override fun push(changes: List<OutboundSyncChange>): PushResult {
        if (changes.isEmpty()) return PushResult(emptyMap(), emptyMap())
        require(changes.size <= configuration.maxPushBatchSize) { "Supabase push batch exceeds configured limit." }
        val session = session()
        require(changes.all { it.accountId == session.accountId }) { "Sync event account does not match authenticated session." }
        val requestBody = buildJsonObject { put("p_events", JsonArray(changes.map(codec::encode))) }.toString()
        val response = request("POST", "/rest/v1/rpc/push_sync_events", requestBody.toByteArray())
        val requested = changes.associateBy { it.eventId }
        val accepted = linkedMapOf<SyncEventId, SyncRevision>()
        val duplicates = linkedMapOf<SyncEventId, SyncRevision>()
        codec.parseArray(response.body.decodeToString()).forEach { item ->
            val row = item.jsonObject
            val id = SyncEventId(row.requiredString("event_id"))
            if (id !in requested) throw SupabaseTransportException("SYNC_SUPABASE_UNEXPECTED_ACK", false)
            val revision = SyncRevision(row.requiredLong("revision"))
            if (row.requiredBoolean("duplicate")) duplicates[id] = revision else accepted[id] = revision
        }
        return PushResult(accepted, duplicates)
    }

    override fun pull(accountId: SyncAccountId, after: SyncCursor, limit: Int): PullPage {
        require(limit > 0)
        val session = session()
        require(accountId == session.accountId) { "Pull account does not match authenticated session." }
        val actualLimit = min(limit, configuration.pageSize)
        val path = "/rest/v1/sync_events?select=revision,event_id,idempotency_key,origin_device_id,namespace,kind,entity_id,payload_version,payload&revision=gt.${after.value}&order=revision.asc&limit=$actualLimit"
        val rows = codec.parseArray(request("GET", path).body.decodeToString())
        val changes = rows.map { row ->
            val obj = row.jsonObject
            codec.decode(obj.requiredLong("revision"), obj, accountId)
        }
        if (changes.zipWithNext().any { (a, b) -> a.revision >= b.revision }) {
            throw SupabaseTransportException("SYNC_SUPABASE_REVISION_ORDER_INVALID", false)
        }
        val next = changes.lastOrNull()?.revision?.value ?: after.value
        return PullPage(changes, SyncCursor(next), changes.size == actualLimit)
    }

    override fun acknowledge(acknowledgement: SyncAcknowledgement) {
        val session = session()
        require(acknowledgement.accountId == session.accountId) { "ACK account does not match authenticated session." }
        val body = buildJsonObject {
            put("p_device_id", acknowledgement.deviceId.value)
            put("p_acknowledged_revision", acknowledgement.cursor.value)
        }.toString().toByteArray()
        request("POST", "/rest/v1/rpc/ack_sync_cursor", body)
    }

    private fun session(): SupabaseSession = sessions.currentSession()
        ?: throw SupabaseTransportException("SYNC_SUPABASE_AUTHENTICATION_REQUIRED", false, 401)

    private fun request(method: String, path: String, body: ByteArray? = null): SupabaseHttpResponse {
        var attempt = 0
        var last: Throwable? = null
        while (attempt < configuration.retry.maxAttempts) {
            check(!Thread.currentThread().isInterrupted) { "Sync request cancelled." }
            attempt++
            val session = session()
            try {
                val response = http.execute(
                    SupabaseHttpRequest(method, URI(configuration.baseUri.toString() + path), mapOf(
                        "apikey" to configuration.publishableKey,
                        "Authorization" to "Bearer ${session.accessToken}",
                        "Content-Type" to "application/json"
                    ), body, configuration.requestTimeoutMillis)
                )
                if (response.status in 200..299) return response
                val retryable = response.status == 408 || response.status == 429 || response.status >= 500
                val error = SupabaseTransportException(statusCode(response.status), retryable, response.status)
                if (!retryable || attempt == configuration.retry.maxAttempts) throw error
                last = error
            } catch (error: IOException) {
                last = SupabaseTransportException("SYNC_SUPABASE_NETWORK_ERROR", true, cause = error)
                if (attempt == configuration.retry.maxAttempts) throw last
            }
            val exponential = configuration.retry.initialDelayMillis * (1L shl (attempt - 1).coerceAtMost(20))
            delay(min(exponential, configuration.retry.maxDelayMillis))
        }
        throw last ?: SupabaseTransportException("SYNC_SUPABASE_REQUEST_FAILED", false)
    }

    private fun statusCode(status: Int) = when (status) {
        401 -> "SYNC_SUPABASE_AUTHENTICATION_FAILED"
        403 -> "SYNC_SUPABASE_AUTHORIZATION_FAILED"
        400, 404 -> "SYNC_SUPABASE_PROTOCOL_REJECTED"
        409 -> "SYNC_SUPABASE_IDEMPOTENCY_CONFLICT"
        408, 429 -> "SYNC_SUPABASE_RETRYABLE_HTTP"
        else -> if (status >= 500) "SYNC_SUPABASE_SERVER_ERROR" else "SYNC_SUPABASE_HTTP_ERROR"
    }
}

private fun JsonObject.required(name: String) = this[name] ?: throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MISSING_FIELD", false)
private fun JsonObject.requiredString(name: String) = runCatching { required(name).jsonPrimitive.content }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
private fun JsonObject.requiredLong(name: String) = runCatching { required(name).jsonPrimitive.long }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
private fun JsonObject.requiredBoolean(name: String) = runCatching { required(name).jsonPrimitive.boolean }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
