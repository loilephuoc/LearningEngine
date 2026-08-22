package vn.loi.learning.infrastructure.sync.supabase

import java.security.MessageDigest
import kotlin.test.*
import vn.loi.learning.application.port.MediaBlobUploadResult
import vn.loi.learning.domain.sync.protocol.SyncAccountId

class SupabaseMediaBlobTransportTest {
    private val account = SyncAccountId("00000000-0000-0000-0000-000000000001")
    private val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1)
    private val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private val configuration = SupabaseConfiguration("https://project.supabase.co", "public-key", retry = SupabaseRetryPolicy(2, 0, 0))

    @Test fun `upload uses authenticated user scoped content addressed key`() {
        val client = QueueClient(response(404), response(201))
        assertEquals(MediaBlobUploadResult.UPLOADED, transport(client).upload(account, sha, "image/png", bytes))
        assertEquals(listOf("HEAD", "POST"), client.requests.map { it.method })
        assertTrue(client.requests.last().uri.path.endsWith("/sync-media/${account.value}/$sha"))
        assertContentEquals(bytes, client.requests.last().body)
    }

    @Test fun `existing content addressed object makes duplicate upload a no-op`() {
        val client = QueueClient(response(200))
        assertEquals(MediaBlobUploadResult.ALREADY_PRESENT, transport(client).upload(account, sha, "image/png", bytes))
        assertEquals(1, client.requests.size)
    }

    @Test fun `download returns verified bytes and missing object remains pending`() {
        assertContentEquals(bytes, transport(QueueClient(response(200, bytes))).download(account, sha))
        assertNull(transport(QueueClient(response(404))).download(account, sha))
    }

    @Test fun `corrupt download is rejected before media apply`() {
        val error = assertFailsWith<SupabaseTransportException> {
            transport(QueueClient(response(200, byteArrayOf(1, 2, 3)))).download(account, sha)
        }
        assertEquals("SYNC_SUPABASE_STORAGE_CHECKSUM_MISMATCH", error.code)
    }

    @Test fun `foreign identity and unsafe digest fail before network`() {
        val client = QueueClient(response(200))
        assertFailsWith<IllegalArgumentException> { transport(client).download(SyncAccountId("other"), sha) }
        assertFailsWith<IllegalArgumentException> { transport(client).download(account, "../object") }
        assertTrue(client.requests.isEmpty())
    }

    @Test fun `storage retry is bounded and authorization is not retried`() {
        val retry = QueueClient(response(500), response(404))
        assertNull(transport(retry).download(account, sha))
        assertEquals(2, retry.requests.size)
        val denied = QueueClient(response(403))
        assertEquals("SYNC_SUPABASE_AUTHORIZATION_FAILED", assertFailsWith<SupabaseTransportException> { transport(denied).download(account, sha) }.code)
        assertEquals(1, denied.requests.size)
    }

    private fun transport(client: QueueClient) = SupabaseMediaBlobTransport(configuration, SupabaseSessionProvider { SupabaseSession(account, "token-secret") }, client)
    private fun response(status: Int, body: ByteArray = byteArrayOf()) = SupabaseHttpResponse(status, body)
    private class QueueClient(vararg responses: SupabaseHttpResponse) : SupabaseHttpClient {
        private val responses = ArrayDeque(responses.toList())
        val requests = mutableListOf<SupabaseHttpRequest>()
        override fun execute(request: SupabaseHttpRequest): SupabaseHttpResponse { requests += request; return responses.removeFirst() }
    }
}
