package vn.loi.learning.android.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AndroidSyncUiState(
    val phase: AndroidSyncPhase = AndroidSyncPhase.UNCONFIGURED,
    val projectUrl: String = "",
    val publishableKey: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val signedInEmail: String? = null,
    val summary: AndroidSyncSummary? = null,
    val diagnosticCode: String? = null,
    val lastSuccessfulSync: Instant? = null
) {
    val configured: Boolean get() = phase != AndroidSyncPhase.UNCONFIGURED
    val canSignIn: Boolean get() = configured && !email.isBlank() && !password.isBlank() && phase == AndroidSyncPhase.SIGNED_OUT
    val canSync: Boolean get() = phase in setOf(AndroidSyncPhase.READY, AndroidSyncPhase.SUCCESS, AndroidSyncPhase.NO_CHANGES,
        AndroidSyncPhase.OFFLINE, AndroidSyncPhase.RETRYABLE_ERROR, AndroidSyncPhase.CONFLICTS, AndroidSyncPhase.PENDING_MEDIA)
}

class AndroidSyncViewModel(
    private val controller: AndroidSyncRuntimeController,
    private val ownedScope: CoroutineScope? = null
) : ViewModel() {
    private val mutableState = MutableStateFlow(fromController(controller.state.value))
    val state: StateFlow<AndroidSyncUiState> = mutableState.asStateFlow()

    init {
        controller.state.collectInto(this)
    }

    fun updateProjectUrl(value: String) = update { copy(projectUrl = value, diagnosticCode = null) }
    fun updatePublishableKey(value: String) = update { copy(publishableKey = value, diagnosticCode = null) }
    fun updateEmail(value: String) = update { copy(email = value, diagnosticCode = null) }
    fun updatePassword(value: String) = update { copy(password = value, diagnosticCode = null) }
    fun togglePasswordVisibility() = update { copy(passwordVisible = !passwordVisible) }

    fun saveConfiguration(): Boolean = runCatching {
        controller.configure(state.value.projectUrl, state.value.publishableKey)
    }.fold(
        onSuccess = { mutableState.value = fromController(controller.state.value, mutableState.value).copy(publishableKey = ""); true },
        onFailure = { update { copy(diagnosticCode = "SYNC_CONFIGURATION_INVALID") }; false }
    )

    fun signIn(): Boolean {
        val current = state.value
        if (!current.canSignIn) return false
        val accepted = controller.signIn(current.email.trim(), current.password.toCharArray()) != null
        update { copy(password = "", passwordVisible = false) }
        return accepted
    }

    fun signOut() = controller.signOut()
    fun syncNow(): Boolean = controller.syncNow() != null
    fun cancelSync(): Boolean = controller.cancelSync()

    override fun onCleared() {
        controller.cancelSync()
        ownedScope?.cancel()
        super.onCleared()
    }

    private fun update(block: AndroidSyncUiState.() -> AndroidSyncUiState) { mutableState.value = mutableState.value.block() }

    private fun StateFlow<AndroidSyncState>.collectInto(viewModelScope: AndroidSyncViewModel) {
        viewModelScope.viewModelScope.launch {
            collect { next ->
                val previous = mutableState.value
                val mapped = fromController(next, previous)
                mutableState.value = if (next.phase == AndroidSyncPhase.SUCCESS || next.phase == AndroidSyncPhase.NO_CHANGES)
                    mapped.copy(lastSuccessfulSync = Instant.now()) else mapped
            }
        }
    }

    private fun fromController(value: AndroidSyncState, previous: AndroidSyncUiState? = null) = AndroidSyncUiState(
        phase = value.phase,
        projectUrl = value.connection?.projectUrl ?: previous?.projectUrl.orEmpty(),
        publishableKey = previous?.publishableKey.orEmpty(),
        email = previous?.email.orEmpty(),
        password = previous?.password.orEmpty(),
        passwordVisible = previous?.passwordVisible ?: false,
        signedInEmail = value.signedInEmail,
        summary = value.summary,
        diagnosticCode = value.diagnosticCode,
        lastSuccessfulSync = previous?.lastSuccessfulSync
    )

    companion object {
        fun production(
            context: Context,
            engine: vn.loi.learning.infrastructure.LearningApplicationContext,
            media: vn.loi.learning.application.port.ContentMediaStorage
        ): AndroidSyncViewModel {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return AndroidSyncViewModel(AndroidSyncCompositionFactory.create(context, scope, engine, media), scope)
        }
    }
}
