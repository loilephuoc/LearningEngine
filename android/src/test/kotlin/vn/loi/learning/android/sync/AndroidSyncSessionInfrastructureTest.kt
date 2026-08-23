package vn.loi.learning.android.sync

import kotlin.test.*
import org.junit.Test
import vn.loi.learning.infrastructure.sync.supabase.*

class AndroidSyncSessionInfrastructureTest {
    @Test fun `missing configuration performs no runtime or network construction`() {
        val repository = FakeConnectionRepository()
        var constructions = 0
        repository.load()?.let { constructions++; it.validated() }
        assertEquals(0, constructions)
    }

    @Test fun `connection validation matches secure Supabase transport rules`() {
        assertEquals("https://project.supabase.co", AndroidSyncConnection("https://project.supabase.co/", "publishable").validated().baseUri.toString())
        assertFailsWith<IllegalArgumentException> { AndroidSyncConnection("http://remote.example", "key").validated() }
        assertFailsWith<IllegalArgumentException> { AndroidSyncConnection("https://project.supabase.co/path?x=1", "key").validated() }
    }

    @Test fun `connection repository stores only non-secret connection values`() {
        val repository = FakeConnectionRepository()
        repository.save(AndroidSyncConnection("https://project.supabase.co", "sb_publishable_client"))
        assertEquals("https://project.supabase.co", repository.load()!!.projectUrl)
        assertFalse(repository.serialized().contains("access")); assertFalse(repository.serialized().contains("refresh")); assertFalse(repository.serialized().contains("password"))
        assertFalse(repository.load()!!.maskedKey().contains("publishable"))
    }

    @Test fun `sign in and sign out use memory only session and redact failures`() {
        val http = QueueHttp(ok(), SupabaseHttpResponse(204, byteArrayOf()))
        val runtime = AndroidSupabaseSessionRuntime(SupabaseConfiguration("https://project.supabase.co", "public-key"), http)
        val password = "private-password".toCharArray()
        assertEquals(USER, runtime.signIn("learner@example.com", password).accountId.value)
        assertTrue(password.all { it == '\u0000' })
        runtime.signOut(); assertNull(runtime.currentSession())
        assertFalse(http.requests.joinToString().contains("private-password"))
    }

    @Test fun `new memory only runtime requires login again after process recreation`() {
        val config = SupabaseConfiguration("https://project.supabase.co", "key")
        val first = AndroidSupabaseSessionRuntime(config, QueueHttp(ok()))
        first.signIn("learner@example.com", "password".toCharArray())
        assertNotNull(first.currentSession())
        assertNull(AndroidSupabaseSessionRuntime(config, QueueHttp()).currentSession())
    }

    private class FakeConnectionRepository : AndroidSyncConnectionRepository {
        private var value: AndroidSyncConnection? = null
        override fun load() = value
        override fun save(connection: AndroidSyncConnection) { connection.validated(); value = connection }
        fun serialized() = value?.let { "${it.projectUrl}|${it.publishableKey}" }.orEmpty()
    }
    private class QueueHttp(vararg responses: SupabaseHttpResponse) : SupabaseHttpClient {
        private val queue = ArrayDeque(responses.toList()); val requests = mutableListOf<SupabaseHttpRequest>()
        override fun execute(request: SupabaseHttpRequest): SupabaseHttpResponse { requests += request; return queue.removeFirst() }
    }
    private fun ok() = SupabaseHttpResponse(200, """{"access_token":"access","refresh_token":"refresh","expires_in":3600,"user":{"id":"$USER","email":"learner@example.com"}}""".encodeToByteArray())
    private companion object { const val USER = "00000000-0000-0000-0000-000000000001" }
}
