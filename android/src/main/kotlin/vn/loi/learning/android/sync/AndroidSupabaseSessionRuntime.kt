package vn.loi.learning.android.sync

import vn.loi.learning.infrastructure.sync.supabase.*

interface AndroidSyncSessionAccess : SupabaseSessionProvider {
    fun signIn(email: String, password: CharArray): SupabaseSession
    fun signOut()
}

/** Memory-only by design until a reviewed Android Keystore credential adapter exists. */
class AndroidSupabaseSessionRuntime(
    configuration: SupabaseConfiguration,
    http: SupabaseHttpClient = UrlConnectionSupabaseHttpClient(),
    store: SecureSupabaseSessionStore = MemoryOnlySupabaseSessionStore()
) : AndroidSyncSessionAccess {
    private val provider = RefreshingSupabaseSessionProvider(SupabaseAuthClient(configuration, http), store)
    override fun currentSession(): SupabaseSession? = provider.currentSession()
    override fun signIn(email: String, password: CharArray): SupabaseSession = provider.signIn(email, password)
    override fun signOut() = provider.signOut()
}
