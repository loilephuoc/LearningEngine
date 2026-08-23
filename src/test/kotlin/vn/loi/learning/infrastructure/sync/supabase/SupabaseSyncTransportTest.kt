package vn.loi.learning.infrastructure.sync.supabase

import kotlin.test.*
import vn.loi.learning.domain.sync.protocol.*

class SupabaseSyncTransportTest {
    private val account = SyncAccountId("00000000-0000-0000-0000-000000000001")
    private val session = SupabaseSession(account, "jwt-secret-value")
    private val config = SupabaseConfiguration("https://project.supabase.co", "publishable-secret", retry = SupabaseRetryPolicy(3, 0, 0))

    @Test fun `push preserves identity and parses accepted and duplicate acknowledgements`() {
        val client = QueueClient(response(200, """[{"event_id":"event-1","revision":7,"duplicate":false},{"event_id":"event-2","revision":8,"duplicate":true}]"""))
        val result = transport(client).push(listOf(change(1), change(2)))
        assertEquals(7, result.accepted.getValue(SyncEventId("event-1")).value)
        assertEquals(8, result.duplicates.getValue(SyncEventId("event-2")).value)
        assertFalse(client.requests.single().body!!.decodeToString().contains(account.value))
        assertTrue(client.requests.single().body!!.decodeToString().contains("key-1"))
    }

    @Test fun `partial push only acknowledges events explicitly returned`() {
        val result = transport(QueueClient(response(200, """[{"event_id":"event-1","revision":1,"duplicate":false}]""")))
            .push(listOf(change(1), change(2)))
        assertEquals(setOf(SyncEventId("event-1")), result.accepted.keys)
        assertFalse(SyncEventId("event-2") in result.accepted)
    }

    @Test fun `pull uses exclusive cursor ordering and validates payload version`() {
        val body = """[{"revision":3,"event_id":"event-1","idempotency_key":"key-1","origin_device_id":"desktop","namespace":"CONTENT","kind":"CONTENT_FIELD","entity_id":"content-1","payload_version":1,"payload":{"field":"QUESTION","operation":"SET","value":"hello"}}]"""
        val client = QueueClient(response(200, body))
        val page = transport(client).pull(account, SyncCursor(2), 10)
        assertEquals(3, page.nextCursor.value)
        assertEquals("hello", (page.changes.single().change.delta as ContentFieldDelta).value)
        assertTrue(client.requests.single().uri.query.contains("revision=gt.2"))
        assertTrue(client.requests.single().uri.query.contains("order=revision.asc"))
    }

    @Test fun `unknown payload and malformed response fail without a cursor side effect`() {
        val unknown = """[{"revision":1,"event_id":"e","idempotency_key":"k","origin_device_id":"d","namespace":"CONTENT","kind":"CONTENT_FIELD","entity_id":"c","payload_version":2,"payload":{}}]"""
        assertEquals("SYNC_SUPABASE_PAYLOAD_VERSION_UNSUPPORTED", assertFailsWith<SupabaseTransportException> { transport(QueueClient(response(200, unknown))).pull(account, SyncCursor.START, 10) }.code)
        assertEquals("SYNC_SUPABASE_RESPONSE_MALFORMED", assertFailsWith<SupabaseTransportException> { transport(QueueClient(response(200, "{"))).pull(account, SyncCursor.START, 10) }.code)
    }

    @Test fun `authentication errors do not retry and diagnostics never contain credentials`() {
        val client = QueueClient(response(401, "jwt-secret-value publishable-secret"))
        val error = assertFailsWith<SupabaseTransportException> { transport(client).pull(account, SyncCursor.START, 10) }
        assertEquals("SYNC_SUPABASE_AUTHENTICATION_FAILED", error.code)
        assertEquals(1, client.requests.size)
        assertFalse(error.toString().contains("secret"))
    }

    @Test fun `rate limit and server failures retry only to the configured bound`() {
        val client = QueueClient(response(429, ""), response(500, ""), response(200, "[]"))
        assertTrue(transport(client).pull(account, SyncCursor.START, 10).changes.isEmpty())
        assertEquals(3, client.requests.size)
    }

    @Test fun `untrusted account identity is rejected before network`() {
        val client = QueueClient(response(200, "[]"))
        assertFailsWith<IllegalArgumentException> { transport(client).push(listOf(change(1).copy(accountId = SyncAccountId("other")))) }
        assertTrue(client.requests.isEmpty())
    }

    @Test fun `unauthorized response refreshes once and retries with rotated access token`() {
        val client = QueueClient(response(401, ""), response(200, "[]"))
        val provider = object : RefreshableSupabaseSessionProvider {
            var current = session
            var refreshes = 0
            override fun currentSession() = current
            override fun refreshAfterUnauthorized(rejectedAccessToken: String): SupabaseSession {
                refreshes++
                current = current.copy(accessToken = "rotated-token")
                return current
            }
        }
        assertTrue(SupabaseSyncTransport(config, provider, client).pull(account, SyncCursor.START, 10).changes.isEmpty())
        assertEquals(1, provider.refreshes)
        assertEquals("Bearer rotated-token", client.requests.last().headers["Authorization"])
    }

    private fun transport(client: QueueClient) = SupabaseSyncTransport(config, SupabaseSessionProvider { session }, client)
    private fun change(i: Int) = OutboundSyncChange(account, SyncEventId("event-$i"), IdempotencyKey("key-$i"), SyncDeviceId("desktop"), SyncEntityId("content-$i"), delta = ContentFieldDelta(ContentField.QUESTION, DeltaOperation.SET, "value-$i"))
    private fun response(status: Int, body: String) = SupabaseHttpResponse(status, body.toByteArray())

    private class QueueClient(vararg responses: SupabaseHttpResponse) : SupabaseHttpClient {
        private val responses = ArrayDeque(responses.toList())
        val requests = mutableListOf<SupabaseHttpRequest>()
        override fun execute(request: SupabaseHttpRequest): SupabaseHttpResponse { requests += request; return responses.removeFirst() }
    }
}
