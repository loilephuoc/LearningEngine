package vn.loi.learning.android.sync

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.sync.supabase.*

object AndroidSyncCompositionFactory {
    fun create(
        androidContext: Context,
        scope: CoroutineScope,
        engine: LearningApplicationContext,
        mediaStorage: ContentMediaStorage
    ): AndroidSyncRuntimeController {
        val repository = SharedPreferencesAndroidSyncConnectionRepository(androidContext)
        return AndroidSyncRuntimeController(scope, repository, runtimeFactory = { configuration ->
            val http = UrlConnectionSupabaseHttpClient()
            val sessions = AndroidSupabaseSessionRuntime(configuration, http)
            val eventTransport = SupabaseSyncTransport(configuration, sessions, http)
            val blobTransport = SupabaseMediaBlobTransport(configuration, sessions, http)
            AndroidSyncRuntimeController.Runtime(
                sessions,
                DefaultAndroidManualSyncOperation(engine, mediaStorage, eventTransport, blobTransport)
            )
        })
    }
}
