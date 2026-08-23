package vn.loi.learning.infrastructure.sync.supabase

import java.net.URI
import vn.loi.learning.domain.sync.protocol.SyncAccountId

data class SupabaseSession(
    val accountId: SyncAccountId,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochSeconds: Long = Long.MAX_VALUE,
    val userEmail: String? = null
) {
    init {
        require(accessToken.isNotBlank()) { "Supabase access token must not be blank." }
        require(refreshToken == null || refreshToken.isNotBlank())
        require(expiresAtEpochSeconds > 0)
    }
}

fun interface SupabaseSessionProvider { fun currentSession(): SupabaseSession? }

interface RefreshableSupabaseSessionProvider : SupabaseSessionProvider {
    fun refreshAfterUnauthorized(rejectedAccessToken: String): SupabaseSession?
}

data class SupabaseRetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 100,
    val maxDelayMillis: Long = 2_000
) {
    init {
        require(maxAttempts in 1..10)
        require(initialDelayMillis >= 0 && maxDelayMillis >= initialDelayMillis)
    }
}

data class SupabaseConfiguration(
    val projectUrl: String,
    val publishableKey: String,
    val requestTimeoutMillis: Int = 15_000,
    val pageSize: Int = 100,
    val maxPushBatchSize: Int = 100,
    val retry: SupabaseRetryPolicy = SupabaseRetryPolicy(),
    val mediaBucket: String = "sync-media"
) {
    val baseUri: URI = URI(projectUrl.trimEnd('/'))

    init {
        require(baseUri.scheme == "https" || (baseUri.scheme == "http" && baseUri.host in LOCAL_HOSTS)) {
            "Supabase project URL must use HTTPS (HTTP is allowed only for local development)."
        }
        require(baseUri.userInfo == null && baseUri.query == null && baseUri.fragment == null)
        require(!baseUri.host.isNullOrBlank())
        require(publishableKey.isNotBlank()) { "Supabase publishable key must not be blank." }
        require(requestTimeoutMillis > 0 && pageSize > 0 && maxPushBatchSize > 0)
        require(mediaBucket.matches(Regex("[a-z0-9][a-z0-9-]{0,62}")))
    }

    companion object { private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1") }
}

sealed interface SupabaseAvailability {
    data class Enabled(val configuration: SupabaseConfiguration) : SupabaseAvailability
    data class Disabled(val code: String) : SupabaseAvailability
}

object SupabaseConfigurationLoader {
    fun fromEnvironment(values: Map<String, String> = System.getenv()): SupabaseAvailability {
        val url = values["LEARNING_ENGINE_SUPABASE_URL"]
        val key = values["LEARNING_ENGINE_SUPABASE_PUBLISHABLE_KEY"]
        if (url.isNullOrBlank() || key.isNullOrBlank()) return SupabaseAvailability.Disabled("SYNC_SUPABASE_NOT_CONFIGURED")
        return runCatching { SupabaseConfiguration(url, key) }
            .fold({ SupabaseAvailability.Enabled(it) }, { SupabaseAvailability.Disabled("SYNC_SUPABASE_CONFIGURATION_INVALID") })
    }
}
