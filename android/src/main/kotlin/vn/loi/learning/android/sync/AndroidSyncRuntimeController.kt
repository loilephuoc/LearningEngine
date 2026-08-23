package vn.loi.learning.android.sync

import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import vn.loi.learning.domain.sync.protocol.SyncDeviceId
import vn.loi.learning.infrastructure.sync.supabase.*

enum class AndroidSyncPhase {
    UNCONFIGURED, SIGNED_OUT, AUTHENTICATING, READY, SYNCING, SUCCESS, NO_CHANGES,
    OFFLINE, REQUIRES_LOGIN, CONFLICTS, PENDING_MEDIA, RETRYABLE_ERROR, FATAL_ERROR, CANCELLED
}

data class AndroidSyncState(
    val phase: AndroidSyncPhase,
    val connection: AndroidSyncConnection? = null,
    val signedInEmail: String? = null,
    val summary: AndroidSyncSummary? = null,
    val diagnosticCode: String? = null
) { val busy get() = phase == AndroidSyncPhase.AUTHENTICATING || phase == AndroidSyncPhase.SYNCING }

class AndroidSyncRuntimeController(
    private val scope: CoroutineScope,
    private val connections: AndroidSyncConnectionRepository,
    private val runtimeFactory: (SupabaseConfiguration) -> Runtime,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    initialConnection: AndroidSyncConnection? = runCatching { connections.load() }.getOrNull()
) {
    data class Runtime(val sessions: AndroidSyncSessionAccess, val operation: AndroidManualSyncOperation)
    private val mutableState = MutableStateFlow(AndroidSyncState(if (initialConnection == null) AndroidSyncPhase.UNCONFIGURED else AndroidSyncPhase.SIGNED_OUT, initialConnection))
    val state: StateFlow<AndroidSyncState> = mutableState.asStateFlow()
    private var runtime: Runtime? = null
    private var activeJob: Job? = null

    @Synchronized fun configure(projectUrl: String, publishableKey: String) {
        check(activeJob?.isActive != true) { "Android sync is busy." }
        val connection = AndroidSyncConnection(projectUrl.trim(), publishableKey.trim())
        val configuration = connection.validated()
        connections.save(connection)
        runtime?.sessions?.signOut()
        runtime = runtimeFactory(configuration)
        mutableState.value = AndroidSyncState(AndroidSyncPhase.SIGNED_OUT, connection)
    }

    @Synchronized fun signIn(email: String, password: CharArray): Job? {
        if (activeJob?.isActive == true) { password.fill('\u0000'); return null }
        val connection = mutableState.value.connection ?: run { password.fill('\u0000'); return null }
        val active = runtime ?: runtimeFactory(connection.validated()).also { runtime = it }
        mutableState.value = mutableState.value.copy(phase = AndroidSyncPhase.AUTHENTICATING, diagnosticCode = null)
        return scope.launch(dispatcher) {
            try {
                val session = runInterruptible { active.sessions.signIn(email, password) }
                mutableState.value = mutableState.value.copy(phase = AndroidSyncPhase.READY, signedInEmail = session.userEmail)
            } catch (failure: Exception) {
                password.fill('\u0000')
                mutableState.value = mutableState.value.copy(phase = AndroidSyncPhase.SIGNED_OUT, diagnosticCode = (failure as? SupabaseAuthException)?.code ?: "SYNC_AUTH_FAILED")
            } finally { synchronized(this@AndroidSyncRuntimeController) { activeJob = null } }
        }.also { activeJob = it }
    }

    @Synchronized fun signOut() {
        activeJob?.cancel(); activeJob = null
        runtime?.sessions?.signOut()
        mutableState.value = mutableState.value.copy(phase = if (mutableState.value.connection == null) AndroidSyncPhase.UNCONFIGURED else AndroidSyncPhase.SIGNED_OUT, signedInEmail = null, summary = null)
    }

    @Synchronized fun syncNow(): Job? {
        if (activeJob?.isActive == true) return null
        val active = runtime ?: return null
        val session = try { active.sessions.currentSession() } catch (failure: Exception) {
            mutableState.value = mutableState.value.copy(phase = AndroidSyncPhase.REQUIRES_LOGIN, diagnosticCode = "SYNC_AUTH_RELOGIN_REQUIRED")
            return null
        } ?: run {
            mutableState.value = mutableState.value.copy(phase = AndroidSyncPhase.REQUIRES_LOGIN, diagnosticCode = "SYNC_AUTH_RELOGIN_REQUIRED")
            return null
        }
        mutableState.value = mutableState.value.copy(phase = AndroidSyncPhase.SYNCING, diagnosticCode = null)
        return scope.launch(dispatcher) {
            try {
                val summary = runInterruptible { active.operation.run(session.accountId, SyncDeviceId(DEVICE_ID)) }
                val phase = when {
                    summary.conflicts > 0 -> AndroidSyncPhase.CONFLICTS
                    summary.pendingMedia > 0 -> AndroidSyncPhase.PENDING_MEDIA
                    summary.pushed == 0 && summary.applied == 0 -> AndroidSyncPhase.NO_CHANGES
                    else -> AndroidSyncPhase.SUCCESS
                }
                mutableState.value = mutableState.value.copy(phase = phase, summary = summary)
            } catch (_: CancellationException) {
                mutableState.value = mutableState.value.copy(phase = AndroidSyncPhase.CANCELLED, diagnosticCode = "SYNC_CANCELLED")
            } catch (failure: Exception) {
                val phase = when (failure) {
                    is SupabaseAuthException -> AndroidSyncPhase.REQUIRES_LOGIN
                    is AndroidPendingRemoteMediaException -> AndroidSyncPhase.PENDING_MEDIA
                    is SupabaseTransportException -> when {
                        !failure.retryable -> AndroidSyncPhase.FATAL_ERROR
                        failure.code == "SYNC_SUPABASE_NETWORK_ERROR" -> AndroidSyncPhase.OFFLINE
                        else -> AndroidSyncPhase.RETRYABLE_ERROR
                    }
                    else -> AndroidSyncPhase.FATAL_ERROR
                }
                mutableState.value = mutableState.value.copy(phase = phase, diagnosticCode = diagnostic(failure))
            } finally { synchronized(this@AndroidSyncRuntimeController) { activeJob = null } }
        }.also { activeJob = it }
    }

    @Synchronized fun cancelSync(): Boolean {
        if (mutableState.value.phase != AndroidSyncPhase.SYNCING) return false
        activeJob?.cancel(CancellationException("Manual Android sync cancelled."))
        return true
    }

    private fun diagnostic(failure: Exception) = when (failure) {
        is SupabaseAuthException -> failure.code
        is SupabaseTransportException -> failure.code
        is AndroidPendingRemoteMediaException -> failure.message
        else -> "SYNC_ANDROID_FATAL"
    }

    companion object { val DEVICE_ID = "android-${UUID.randomUUID()}" }
}
