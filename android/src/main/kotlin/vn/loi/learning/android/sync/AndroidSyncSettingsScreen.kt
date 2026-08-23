package vn.loi.learning.android.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.loi.learning.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidSyncSettingsScreen(viewModel: AndroidSyncViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.sync_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sync_back)) } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SyncSection(stringResource(R.string.sync_connection_section)) {
                Text(if (state.configured) stringResource(R.string.sync_configured) else stringResource(R.string.sync_unconfigured))
                OutlinedTextField(state.projectUrl, viewModel::updateProjectUrl, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sync_project_url)) }, singleLine = true)
                OutlinedTextField(state.publishableKey, viewModel::updatePublishableKey, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sync_publishable_key)) }, placeholder = { if (state.configured) Text("••••••••") }, singleLine = true)
                Text(stringResource(R.string.sync_key_warning), style = MaterialTheme.typography.bodySmall)
                Button(viewModel::saveConfiguration, enabled = !state.busy && state.projectUrl.isNotBlank() && state.publishableKey.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sync_save_configuration)) }
            }
            SyncSection(stringResource(R.string.sync_account_section)) {
                val signedInEmail = state.signedInEmail
                if (signedInEmail == null) {
                    OutlinedTextField(state.email, viewModel::updateEmail, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sync_email)) }, singleLine = true)
                    OutlinedTextField(
                        state.password, viewModel::updatePassword, Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.sync_password)) }, singleLine = true,
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(viewModel::togglePasswordVisibility) { Icon(if (state.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, stringResource(if (state.passwordVisible) R.string.sync_hide_password else R.string.sync_show_password)) } }
                    )
                    Text(stringResource(R.string.sync_memory_session), style = MaterialTheme.typography.bodySmall)
                    Button(viewModel::signIn, enabled = state.canSignIn, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sync_sign_in)) }
                } else {
                    Text(stringResource(R.string.sync_signed_in_as, signedInEmail))
                    OutlinedButton(viewModel::signOut, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sync_sign_out)) }
                }
            }
            SyncSection(stringResource(R.string.sync_manual_section)) {
                Text(syncPhaseText(state.phase))
                if (state.phase == AndroidSyncPhase.SYNCING) {
                    OutlinedButton(viewModel::cancelSync, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sync_cancel)) }
                } else {
                    Button(viewModel::syncNow, enabled = state.canSync, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sync_now)) }
                }
                state.summary?.let { Summary(it) }
                state.diagnosticCode?.let { Text(diagnosticText(it), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private val AndroidSyncUiState.busy get() = phase == AndroidSyncPhase.AUTHENTICATING || phase == AndroidSyncPhase.SYNCING

@Composable private fun SyncSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); content() } }
}

@Composable private fun Summary(value: AndroidSyncSummary) {
    Text(stringResource(R.string.sync_summary_events, value.pushed, value.applied))
    Text(stringResource(R.string.sync_summary_media, value.uploadedMedia, value.downloadedMedia))
    Text(stringResource(R.string.sync_summary_attention, value.conflicts, value.pendingMedia))
}

@Composable private fun syncPhaseText(phase: AndroidSyncPhase) = stringResource(when (phase) {
    AndroidSyncPhase.UNCONFIGURED -> R.string.sync_unconfigured
    AndroidSyncPhase.SIGNED_OUT -> R.string.sync_signed_out
    AndroidSyncPhase.AUTHENTICATING -> R.string.sync_authenticating
    AndroidSyncPhase.READY -> R.string.sync_ready
    AndroidSyncPhase.SYNCING -> R.string.sync_syncing
    AndroidSyncPhase.SUCCESS -> R.string.sync_success
    AndroidSyncPhase.NO_CHANGES -> R.string.sync_no_changes
    AndroidSyncPhase.OFFLINE -> R.string.sync_offline
    AndroidSyncPhase.REQUIRES_LOGIN -> R.string.sync_requires_login
    AndroidSyncPhase.CONFLICTS -> R.string.sync_conflicts
    AndroidSyncPhase.PENDING_MEDIA -> R.string.sync_pending_media
    AndroidSyncPhase.RETRYABLE_ERROR -> R.string.sync_retryable_error
    AndroidSyncPhase.FATAL_ERROR -> R.string.sync_fatal_error
    AndroidSyncPhase.CANCELLED -> R.string.sync_cancelled
})

@Composable private fun diagnosticText(code: String) = when (code) {
    "SYNC_CONFIGURATION_INVALID" -> stringResource(R.string.sync_invalid_configuration)
    "SYNC_AUTH_RELOGIN_REQUIRED" -> stringResource(R.string.sync_requires_login)
    "SYNC_SUPABASE_NETWORK_ERROR" -> stringResource(R.string.sync_offline)
    else -> stringResource(R.string.sync_safe_error)
}
