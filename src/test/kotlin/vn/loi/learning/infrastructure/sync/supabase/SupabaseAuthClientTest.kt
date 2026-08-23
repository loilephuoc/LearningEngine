package vn.loi.learning.infrastructure.sync.supabase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class SupabaseAuthClientTest {
    private val config = SupabaseConfiguration("https://project.supabase.co", "publishable-secret")

    @Test fun `email password sign in returns authenticated session without retaining password`() {
        val http = QueueClient(ok("access-1", "refresh-1"))
        val password = "private-password".toCharArray()
        val provider = RefreshingSupabaseSessionProvider(
            SupabaseAuthClient(config, http) { 100 }, MemoryOnlySupabaseSessionStore(),
            nowEpochSeconds = { 100 }
        )
        val session = provider.signIn(" learner@example.com ", password)
        assertEquals(USER_ID, session.accountId.value)
        assertEquals("learner@example.com", session.userEmail)
        assertTrue(password.all { it == '\u0000' })
        assertFalse(session.toString().contains("private-password"))
    }

    @Test fun `password sign in accepts current Supabase token response schema and publishable key`() {
        val http = QueueClient(response(200, """{
            "access_token":"header.payload.signature",
            "token_type":"bearer",
            "expires_in":3600,
            "expires_at":3700,
            "refresh_token":"refresh-token",
            "user":{
                "id":"$USER_ID",
                "aud":"authenticated",
                "role":"authenticated",
                "email":"learner@example.com",
                "app_metadata":{"provider":"email"},
                "user_metadata":{}
            },
            "weak_password":null
        }"""))
        val client = SupabaseAuthClient(
            SupabaseConfiguration("https://project.supabase.co", "sb_publishable_fixture"),
            http
        ) { 100 }

        val session = client.signIn("learner@example.com", "password".toCharArray())

        val request = http.requests.single()
        assertEquals("POST", request.method)
        assertEquals("/auth/v1/token?grant_type=password", request.uri.rawPath + "?" + request.uri.rawQuery)
        assertEquals("sb_publishable_fixture", request.headers["apikey"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertNull(request.headers["Authorization"])
        assertEquals(USER_ID, session.accountId.value)
        assertEquals("header.payload.signature", session.accessToken)
        assertEquals("refresh-token", session.refreshToken)
        assertEquals(3700, session.expiresAtEpochSeconds)
        assertEquals("learner@example.com", session.userEmail)
    }

    @Test fun `invalid credentials and malformed response use redacted diagnostics`() {
        val invalid = assertFailsWith<SupabaseAuthException> { SupabaseAuthClient(config, QueueClient(response(400, "private-password publishable-secret"))).signIn("a@b.com", "private-password".toCharArray()) }
        assertEquals("SYNC_AUTH_INVALID_CREDENTIALS", invalid.code)
        assertFalse(invalid.toString().contains("private-password"))
        assertFalse(invalid.toString().contains("publishable-secret"))
        assertEquals("SYNC_AUTH_RESPONSE_MALFORMED", assertFailsWith<SupabaseAuthException> { SupabaseAuthClient(config, QueueClient(response(200, "{}"))).signIn("a@b.com", "x".toCharArray()) }.code)
    }

    @Test fun `expired session refresh rotates token pair atomically`() {
        val store = MemoryOnlySupabaseSessionStore().also { it.replace(session("old-access", "old-refresh", 100)) }
        val http = QueueClient(ok("new-access", "new-refresh"))
        val provider = RefreshingSupabaseSessionProvider(SupabaseAuthClient(config, http) { 100 }, store, { 100 }, 10)
        assertEquals("new-access", provider.currentSession()!!.accessToken)
        assertEquals("new-refresh", store.load()!!.refreshToken)
        assertFalse(http.requests.single().body!!.decodeToString().contains("old-access"))
    }

    @Test fun `concurrent token requests perform one refresh`() {
        val calls = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val http = SupabaseHttpClient { request -> calls.incrementAndGet(); entered.countDown(); release.await(); ok("new", "rotated") }
        val store = MemoryOnlySupabaseSessionStore().also { it.replace(session("old", "refresh", 100)) }
        val provider = RefreshingSupabaseSessionProvider(SupabaseAuthClient(config, http) { 100 }, store, { 100 }, 10)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit<String> { provider.currentSession()!!.accessToken }
            entered.await()
            val second = pool.submit<String> { provider.currentSession()!!.accessToken }
            release.countDown()
            assertEquals("new", first.get()); assertEquals("new", second.get()); assertEquals(1, calls.get())
        } finally { pool.shutdownNow() }
    }

    @Test fun `refresh failure clears only memory session and requires login again`() {
        val store = MemoryOnlySupabaseSessionStore().also { it.replace(session("old", "refresh", 100)) }
        val provider = RefreshingSupabaseSessionProvider(SupabaseAuthClient(config, QueueClient(response(401, "denied"))) { 100 }, store, { 100 }, 10)
        assertEquals("SYNC_AUTH_RELOGIN_REQUIRED", assertFailsWith<SupabaseAuthException> { provider.currentSession() }.code)
        assertNull(store.load())
    }

    @Test fun `sign out clears memory before remote call and process restart has no session`() {
        val store = MemoryOnlySupabaseSessionStore().also { it.replace(session("access", "refresh", 1_000)) }
        val provider = RefreshingSupabaseSessionProvider(SupabaseAuthClient(config, QueueClient(response(204, ""))), store)
        provider.signOut()
        assertNull(store.load())
        assertNull(MemoryOnlySupabaseSessionStore().load())
    }

    private fun session(access: String, refresh: String, expires: Long) = SupabaseSession(vn.loi.learning.domain.sync.protocol.SyncAccountId(USER_ID), access, refresh, expires, "learner@example.com")
    private fun ok(access: String, refresh: String) = response(200, """{"access_token":"$access","refresh_token":"$refresh","expires_in":3600,"user":{"id":"$USER_ID","email":"learner@example.com"}}""")
    private fun response(status: Int, body: String) = SupabaseHttpResponse(status, body.encodeToByteArray())
    private class QueueClient(vararg responses: SupabaseHttpResponse) : SupabaseHttpClient {
        private val queue = ArrayDeque(responses.toList()); val requests = mutableListOf<SupabaseHttpRequest>()
        override fun execute(request: SupabaseHttpRequest): SupabaseHttpResponse { requests += request; return queue.removeFirst() }
    }
    private companion object { const val USER_ID = "00000000-0000-0000-0000-000000000001" }
}
