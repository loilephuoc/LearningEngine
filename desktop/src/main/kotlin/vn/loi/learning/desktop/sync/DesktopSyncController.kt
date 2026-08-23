package vn.loi.learning.desktop.sync

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.loi.learning.infrastructure.sync.supabase.*
import vn.loi.learning.domain.sync.protocol.SyncDeviceId

enum class DesktopSyncPhase { UNCONFIGURED, SIGNED_OUT, SIGNING_IN, READY, SYNCING, SUCCESS, NO_CHANGES, OFFLINE, RELOGIN_REQUIRED, CONFLICT, PENDING_BLOB, ERROR }

data class DesktopSyncUiState(
    val phase: DesktopSyncPhase,
    val connection: DesktopSupabaseConnection? = null,
    val signedInEmail: String? = null,
    val message: String? = null,
    val lastSuccessfulSync: Instant? = null,
    val summary: DesktopManualSyncSummary? = null
) { val busy get() = phase == DesktopSyncPhase.SIGNING_IN || phase == DesktopSyncPhase.SYNCING }

class DesktopSyncController(
    private val store: DesktopSupabaseConnectionStore,
    private val runtimeFactory: (SupabaseConfiguration) -> Runtime,
    initialConnection: DesktopSupabaseConnection? = runCatching { store.load() }.getOrNull()
) {
    interface SessionAccess {
        fun currentSession(): SupabaseSession?
        fun signIn(email: String, password: CharArray): SupabaseSession
        fun signOut()
    }
    data class Runtime(val sessions: SessionAccess, val action: DesktopManualSyncOperation)
    private val mutableState = MutableStateFlow(DesktopSyncUiState(if (initialConnection == null) DesktopSyncPhase.UNCONFIGURED else DesktopSyncPhase.SIGNED_OUT, initialConnection))
    val state: StateFlow<DesktopSyncUiState> = mutableState.asStateFlow()
    @Volatile private var runtime: Runtime? = null

    @Synchronized fun saveConnection(url: String, key: String) {
        check(!mutableState.value.busy)
        val connection = DesktopSupabaseConnection(url.trim(), key.trim())
        val config = connection.validated()
        runtime?.sessions?.signOut(); runtime = runtimeFactory(config)
        store.save(connection)
        mutableState.value = DesktopSyncUiState(DesktopSyncPhase.SIGNED_OUT, connection, message = "Đã lưu cấu hình. Khóa chỉ được hiển thị rút gọn.")
    }

    fun signIn(email: String, password: CharArray) {
        synchronized(this) {
            if (mutableState.value.busy) { password.fill('\u0000'); return }
            val connection = mutableState.value.connection ?: run { password.fill('\u0000'); return }
            if (runtime == null) runtime = runtimeFactory(connection.validated())
            mutableState.value = mutableState.value.copy(phase = DesktopSyncPhase.SIGNING_IN, message = null)
        }
        try {
            val session = runtime!!.sessions.signIn(email, password)
            mutableState.value = mutableState.value.copy(phase = DesktopSyncPhase.READY, signedInEmail = session.userEmail, message = "Đã đăng nhập. Phiên chỉ được giữ trong bộ nhớ.")
        } catch (failure: Exception) {
            password.fill('\u0000')
            mutableState.value = mutableState.value.copy(phase = DesktopSyncPhase.SIGNED_OUT, message = authMessage(failure))
        }
    }

    @Synchronized fun signOut() {
        runtime?.sessions?.signOut()
        mutableState.value = mutableState.value.copy(phase = if (mutableState.value.connection == null) DesktopSyncPhase.UNCONFIGURED else DesktopSyncPhase.SIGNED_OUT, signedInEmail = null, message = "Đã đăng xuất. Dữ liệu học cục bộ được giữ nguyên.")
    }

    fun syncNow() {
        val active: Runtime
        val session: SupabaseSession
        synchronized(this) {
            if (mutableState.value.busy) return
            active = runtime ?: return
            session = active.sessions.currentSession() ?: return
            mutableState.value = mutableState.value.copy(phase = DesktopSyncPhase.SYNCING, message = "Đang Push → Pull → Apply → ACK…")
        }
        try {
            val summary = active.action.run(session.accountId, SyncDeviceId(DEVICE_ID))
            val phase = when { summary.conflicts > 0 -> DesktopSyncPhase.CONFLICT; summary.pendingBlobs > 0 -> DesktopSyncPhase.PENDING_BLOB; summary.pushed == 0 && summary.applied == 0 -> DesktopSyncPhase.NO_CHANGES; else -> DesktopSyncPhase.SUCCESS }
            val now = Instant.now()
            mutableState.value = mutableState.value.copy(phase = phase, summary = summary, lastSuccessfulSync = if (phase == DesktopSyncPhase.SUCCESS || phase == DesktopSyncPhase.NO_CHANGES) now else mutableState.value.lastSuccessfulSync, message = if (phase == DesktopSyncPhase.NO_CHANGES) "Không có thay đổi mới." else "Đồng bộ đã hoàn tất.")
        } catch (failure: Exception) {
            val phase = when (failure) { is SupabaseAuthException -> DesktopSyncPhase.RELOGIN_REQUIRED; is PendingRemoteMediaException -> DesktopSyncPhase.PENDING_BLOB; is SupabaseTransportException -> if (failure.retryable) DesktopSyncPhase.OFFLINE else DesktopSyncPhase.ERROR; else -> DesktopSyncPhase.ERROR }
            mutableState.value = mutableState.value.copy(phase = phase, message = diagnostic(failure))
        }
    }

    private fun authMessage(failure: Exception) = when ((failure as? SupabaseAuthException)?.code) { "SYNC_AUTH_INVALID_CREDENTIALS" -> "Email hoặc mật khẩu không đúng."; else -> "Không thể đăng nhập. Vui lòng thử lại." }
    private fun diagnostic(failure: Exception) = when (failure) { is SupabaseAuthException -> failure.code; is SupabaseTransportException -> failure.code; is PendingRemoteMediaException -> failure.message; else -> "SYNC_DESKTOP_SYNC_FAILED" }
    companion object { val DEVICE_ID = "desktop-${UUID.randomUUID()}" }
}
