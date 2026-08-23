package vn.loi.learning.desktop.sync

import java.nio.file.Path
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.sync.supabase.*

object DesktopSyncRuntimeFactory {
    fun create(configDirectory: Path, dataDirectory: Path, context: LearningApplicationContext): DesktopSyncController {
        val store = DesktopSupabaseConnectionStore(configDirectory.resolve(DesktopSupabaseConnectionStore.FILE_NAME))
        val dpapi = WindowsDpapiProtector()
        val sessionStore = DesktopSupabaseSessionStore(
            configDirectory.resolve(DesktopSupabaseSessionStore.FILE_NAME), dpapi, dpapi
        )
        return DesktopSyncController(store, runtimeFactory = { configuration ->
            val http = UrlConnectionSupabaseHttpClient()
            val sessions = RefreshingSupabaseSessionProvider(
                SupabaseAuthClient(configuration, http), sessionStore
            )
            val eventTransport = SupabaseSyncTransport(configuration, sessions, http)
            val blobTransport = SupabaseMediaBlobTransport(configuration, sessions, http)
            DesktopSyncController.Runtime(
                object : DesktopSyncController.SessionAccess {
                    override fun currentSession() = sessions.currentSession()
                    override fun signIn(email: String, password: CharArray) = sessions.signIn(email, password)
                    override fun signOut() = sessions.signOut()
                },
                DesktopManualSyncAction(context, JvmContentMediaStorage(dataDirectory.resolve("media")), eventTransport, blobTransport)
            )
        })
    }
}
