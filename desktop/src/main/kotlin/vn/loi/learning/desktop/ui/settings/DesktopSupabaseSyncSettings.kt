package vn.loi.learning.desktop.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.loi.learning.desktop.sync.*

@Composable
fun DesktopSupabaseSyncSettings(controller: DesktopSyncController) {
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    var url by remember(state.connection?.projectUrl) { mutableStateOf(state.connection?.projectUrl.orEmpty()) }
    var key by remember(state.connection) { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Đồng bộ", style = MaterialTheme.typography.titleLarge)
            Text(phaseLabel(state.phase), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(url, { url = it; localError = null }, label = { Text("Project URL") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            val savedConnection = state.connection
            OutlinedTextField(
                key, { key = it; localError = null },
                label = { Text(if (savedConnection == null) "Publishable key" else "Publishable key mới (đang lưu: ${savedConnection.maskedKey()})") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
            Button(enabled = !state.busy && key.isNotBlank(), onClick = {
                runCatching { controller.saveConnection(url, key); key = "" }
                    .onFailure { localError = "Cấu hình không hợp lệ. URL phải dùng HTTPS (trừ localhost)." }
            }) { Text("Lưu cấu hình") }
            Text("Chỉ dùng publishable/anon key. Tuyệt đối không nhập service-role key.", style = MaterialTheme.typography.bodySmall)

            if (state.signedInEmail == null) {
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                Button(enabled = state.connection != null && !state.busy && email.isNotBlank() && password.isNotEmpty(), onClick = {
                    val submitted = password.toCharArray(); password = ""
                    scope.launch(Dispatchers.IO) { controller.signIn(email, submitted) }
                }) { Text("Đăng nhập") }
                Text("Phiên đăng nhập sẽ được ghi nhớ an toàn trên thiết bị này và có thể yêu cầu đăng nhập lại khi hết hiệu lực.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Đã đăng nhập: ${state.signedInEmail}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !state.busy, onClick = { scope.launch(Dispatchers.IO) { controller.syncNow() } }) { Text("Đồng bộ ngay") }
                    OutlinedButton(enabled = !state.busy, onClick = controller::signOut) { Text("Đăng xuất") }
                }
            }
            state.summary?.let { Text("Push: ${it.pushed} · Apply: ${it.applied} · Upload: ${it.uploadedBlobs} · Download: ${it.downloadedBlobs} · Conflict: ${it.conflicts} · Pending: ${it.pendingBlobs}") }
            state.lastSuccessfulSync?.let { Text("Đồng bộ thành công gần nhất: $it", style = MaterialTheme.typography.bodySmall) }
            (localError ?: state.message)?.let { Text(it, color = if (state.phase == DesktopSyncPhase.ERROR || localError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun phaseLabel(phase: DesktopSyncPhase) = when (phase) {
    DesktopSyncPhase.UNCONFIGURED -> "Chưa cấu hình"
    DesktopSyncPhase.SIGNED_OUT -> "Chưa đăng nhập"
    DesktopSyncPhase.SIGNING_IN -> "Đang đăng nhập…"
    DesktopSyncPhase.READY -> "Đã đăng nhập"
    DesktopSyncPhase.SYNCING -> "Đang đồng bộ…"
    DesktopSyncPhase.SUCCESS -> "Đồng bộ thành công"
    DesktopSyncPhase.NO_CHANGES -> "Không có thay đổi"
    DesktopSyncPhase.OFFLINE -> "Ngoại tuyến — outbox được giữ nguyên"
    DesktopSyncPhase.RELOGIN_REQUIRED -> "Cần đăng nhập lại"
    DesktopSyncPhase.CONFLICT -> "Có conflict/quarantine"
    DesktopSyncPhase.PENDING_BLOB -> "Có media đang chờ"
    DesktopSyncPhase.ERROR -> "Đồng bộ thất bại"
}
