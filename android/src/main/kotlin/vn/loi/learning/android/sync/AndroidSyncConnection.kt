package vn.loi.learning.android.sync

import android.content.Context
import vn.loi.learning.infrastructure.sync.supabase.SupabaseConfiguration

data class AndroidSyncConnection(val projectUrl: String, val publishableKey: String) {
    fun validated(): SupabaseConfiguration = SupabaseConfiguration(projectUrl.trim(), publishableKey.trim())
    fun maskedKey(): String = if (publishableKey.length <= 8) "••••••••" else publishableKey.take(4) + "••••" + publishableKey.takeLast(4)
}

interface AndroidSyncConnectionRepository {
    fun load(): AndroidSyncConnection?
    fun save(connection: AndroidSyncConnection)
}

class SharedPreferencesAndroidSyncConnectionRepository(context: Context) : AndroidSyncConnectionRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): AndroidSyncConnection? {
        val url = preferences.getString(PROJECT_URL, null) ?: return null
        val key = preferences.getString(PUBLISHABLE_KEY, null) ?: return null
        return AndroidSyncConnection(url, key).also { it.validated() }
    }

    override fun save(connection: AndroidSyncConnection) {
        val configuration = connection.validated()
        check(
            preferences.edit()
                .putString(PROJECT_URL, configuration.baseUri.toString())
                .putString(PUBLISHABLE_KEY, connection.publishableKey.trim())
                .commit()
        ) { "Android sync connection configuration could not be saved." }
    }

    companion object {
        const val PREFERENCES_NAME = "learning_engine_sync_connection"
        internal const val PROJECT_URL = "project_url"
        internal const val PUBLISHABLE_KEY = "publishable_key"
    }
}
