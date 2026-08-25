package vn.loi.learning.android.family

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.work.*
import java.security.KeyStore
import java.time.Duration
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.android.sync.AndroidSupabaseSessionRuntime
import vn.loi.learning.infrastructure.sync.supabase.*

class AndroidKeystoreSupabaseSessionStore(context: Context) : SecureSupabaseSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val json = Json { encodeDefaults = true }

    @Synchronized override fun load(): SupabaseSession? = runCatching {
        val encoded = preferences.getString(SESSION, null) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val ivSize = bytes.first().toInt() and 0xff
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 1 + ivSize)))
        val stored = json.decodeFromString<StoredSession>(cipher.doFinal(bytes.copyOfRange(1 + ivSize, bytes.size)).decodeToString())
        SupabaseSession(vn.loi.learning.domain.sync.protocol.SyncAccountId(stored.accountId), stored.accessToken, stored.refreshToken, stored.expiresAtEpochSeconds, stored.userEmail)
    }.getOrElse { clear(); null }

    @Synchronized override fun replace(session: SupabaseSession) {
        val plain = json.encodeToString(StoredSession(session.accountId.value, session.accessToken, session.refreshToken, session.expiresAtEpochSeconds, session.userEmail)).encodeToByteArray()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain)
        val packed = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
        check(preferences.edit().putString(SESSION, Base64.encodeToString(packed, Base64.NO_WRAP)).commit())
    }

    @Synchronized override fun clear() { preferences.edit().remove(SESSION).apply() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }

    @Serializable private data class StoredSession(val accountId: String, val accessToken: String, val refreshToken: String?, val expiresAtEpochSeconds: Long, val userEmail: String?)
    companion object { private const val PREFERENCES = "family_supabase_session_v1"; private const val SESSION = "encrypted_session"; private const val KEY_ALIAS = "learning_engine_family_supabase_session"; private const val TRANSFORMATION = "AES/GCM/NoPadding" }
}

class FamilyCloudSyncController(
    private val context: Context,
    private val scope: CoroutineScope,
    configuration: SupabaseConfiguration?,
    repository: FamilyRepository,
    metadata: FamilySyncMetadataStore,
    afterMerge: () -> Unit
) {
    private val sessions: AndroidSupabaseSessionRuntime?
    private val engine: FamilySyncEngine?
    private val fallbackState = kotlinx.coroutines.flow.MutableStateFlow<FamilySyncState>(FamilySyncState.NotConfigured)
    val state: StateFlow<FamilySyncState>

    init {
        if (configuration == null) {
            sessions = null; engine = null; state = fallbackState
        } else {
            val http = UrlConnectionSupabaseHttpClient()
            sessions = AndroidSupabaseSessionRuntime(configuration, http, AndroidKeystoreSupabaseSessionStore(context))
            engine = FamilySyncEngine(repository, metadata, sessions, SupabaseFamilyRemoteTransport(configuration, sessions, http), afterMerge)
            state = engine.state
            engine.refreshState()
        }
    }

    fun signIn(email: String, password: CharArray, onResult: (Result<Unit>) -> Unit = {}) {
        val runtime = sessions ?: return onResult(Result.failure(IllegalStateException("Supabase chưa được cấu hình")))
        scope.launch(Dispatchers.IO) {
            val result = runCatching { runtime.signIn(email, password); engine!!.refreshState(); engine.sync(); enqueueBackground(); Unit }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun signOut() { sessions?.signOut(); engine?.refreshState() }
    fun syncNow() { scope.launch { engine?.sync() } }
    suspend fun syncFromWorker(): FamilySyncState = engine?.sync() ?: FamilySyncState.NotConfigured

    fun enqueueBackground() {
        val request = OneTimeWorkRequestBuilder<FamilySyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object { const val WORK_NAME = "family-supabase-sync-v7" }
}

class FamilySyncWorker(appContext: Context, parameters: WorkerParameters) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication ?: return Result.failure()
        return when (app.familyCloudSyncController.syncFromWorker()) {
            is FamilySyncState.Offline -> Result.retry()
            is FamilySyncState.Failed -> Result.failure()
            else -> Result.success()
        }
    }
}
