package vn.loi.learning.infrastructure.sync.supabase

import java.net.URI
import java.time.Instant
import kotlinx.serialization.json.*
import vn.loi.learning.domain.sync.protocol.SyncAccountId

interface SecureSupabaseSessionStore {
    fun load(): SupabaseSession?
    fun replace(session: SupabaseSession)
    fun clear()
}

/** Deliberately non-persistent fallback: a process restart always requires sign-in. */
class MemoryOnlySupabaseSessionStore : SecureSupabaseSessionStore {
    @Volatile private var session: SupabaseSession? = null
    override fun load(): SupabaseSession? = session
    override fun replace(session: SupabaseSession) { this.session = session }
    override fun clear() { session = null }
}

class SupabaseAuthException(val code: String, val status: Int? = null, cause: Throwable? = null) :
    RuntimeException(code, cause)

class SupabaseAuthClient(
    private val configuration: SupabaseConfiguration,
    private val http: SupabaseHttpClient = UrlConnectionSupabaseHttpClient(),
    private val nowEpochSeconds: () -> Long = { Instant.now().epochSecond }
) {
    fun signIn(email: String, password: CharArray): SupabaseSession {
        val normalizedEmail = email.trim()
        require(EMAIL.matches(normalizedEmail)) { "Email address is invalid." }
        require(password.isNotEmpty()) { "Password must not be empty." }
        val body = buildJsonObject {
            put("email", normalizedEmail)
            put("password", password.concatToString())
        }.toString().encodeToByteArray()
        return request("/auth/v1/token?grant_type=password", body)
    }

    fun refresh(refreshToken: String): SupabaseSession {
        require(refreshToken.isNotBlank())
        val body = buildJsonObject { put("refresh_token", refreshToken) }.toString().encodeToByteArray()
        return request("/auth/v1/token?grant_type=refresh_token", body)
    }

    fun signOut(accessToken: String) {
        val response = http.execute(authRequest("POST", "/auth/v1/logout", byteArrayOf(), accessToken))
        if (response.status !in 200..299 && response.status != 401) throw classified(response.status)
    }

    private fun request(path: String, body: ByteArray): SupabaseSession {
        val response = try { http.execute(authRequest("POST", path, body)) }
        catch (failure: Exception) { throw SupabaseAuthException("SYNC_AUTH_NETWORK_ERROR", cause = failure) }
        if (response.status !in 200..299) throw classified(response.status)
        return parseSession(response.body.decodeToString())
    }

    private fun authRequest(method: String, path: String, body: ByteArray?, bearer: String? = null) =
        SupabaseHttpRequest(method, URI(configuration.baseUri.toString() + path), buildMap {
            put("apikey", configuration.publishableKey)
            put("Content-Type", "application/json")
            bearer?.let { put("Authorization", "Bearer $it") }
        }, body, configuration.requestTimeoutMillis)

    private fun parseSession(body: String): SupabaseSession = try {
        val root = Json.parseToJsonElement(body).jsonObject
        val user = root.getValue("user").jsonObject
        val id = user.getValue("id").jsonPrimitive.content
        require(UUID.matches(id))
        val expiresIn = root.getValue("expires_in").jsonPrimitive.long
        require(expiresIn > 0)
        SupabaseSession(
            SyncAccountId(id),
            root.getValue("access_token").jsonPrimitive.content.also { require(it.isNotBlank()) },
            root.getValue("refresh_token").jsonPrimitive.content.also { require(it.isNotBlank()) },
            nowEpochSeconds() + expiresIn,
            user["email"]?.jsonPrimitive?.contentOrNull
        )
    } catch (failure: Exception) {
        throw SupabaseAuthException("SYNC_AUTH_RESPONSE_MALFORMED", cause = failure)
    }

    private fun classified(status: Int) = SupabaseAuthException(
        when (status) { 400, 401 -> "SYNC_AUTH_INVALID_CREDENTIALS"; 403 -> "SYNC_AUTH_FORBIDDEN"; 429 -> "SYNC_AUTH_RATE_LIMITED"; else -> "SYNC_AUTH_REQUEST_FAILED" },
        status
    )

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        val UUID = Regex("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    }
}

class RefreshingSupabaseSessionProvider(
    private val auth: SupabaseAuthClient,
    private val store: SecureSupabaseSessionStore,
    private val nowEpochSeconds: () -> Long = { Instant.now().epochSecond },
    private val refreshAheadSeconds: Long = 60
) : RefreshableSupabaseSessionProvider {
    @Synchronized override fun currentSession(): SupabaseSession? {
        val current = store.load() ?: return null
        if (current.expiresAtEpochSeconds - nowEpochSeconds() > refreshAheadSeconds) return current
        return rotate(current)
    }

    @Synchronized override fun refreshAfterUnauthorized(rejectedAccessToken: String): SupabaseSession? {
        val current = store.load() ?: return null
        if (current.accessToken != rejectedAccessToken) return current
        return rotate(current)
    }

    @Synchronized fun signIn(email: String, password: CharArray): SupabaseSession {
        val session = try { auth.signIn(email, password) } finally { password.fill('\u0000') }
        store.replace(session)
        return session
    }

    @Synchronized fun signOut() {
        val current = store.load()
        store.clear()
        current?.let { runCatching { auth.signOut(it.accessToken) } }
    }

    private fun rotate(current: SupabaseSession): SupabaseSession {
        val refresh = current.refreshToken ?: return requireSignIn()
        return try { auth.refresh(refresh).also(store::replace) }
        catch (failure: Exception) { store.clear(); throw SupabaseAuthException("SYNC_AUTH_RELOGIN_REQUIRED", cause = failure) }
    }

    private fun requireSignIn(): Nothing {
        store.clear()
        throw SupabaseAuthException("SYNC_AUTH_RELOGIN_REQUIRED")
    }
}
