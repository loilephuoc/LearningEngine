package vn.loi.learning.desktop.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.LEPrimaryButton
import vn.loi.learning.desktop.ui.designsystem.components.LESecondaryButton

@Composable
fun DesktopRestoreDialog(
    state: DesktopRestoreDialogState,
    onSelectFile: () -> Unit,
    onConfirmRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isRestoring) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight()
                .padding(LESpacing.md),
            shape = LERadius.lg,
            color = LEColors.surface,
            tonalElevation = LEElevation.modal,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle)
        ) {
            Column(modifier = Modifier.padding(LESpacing.lg)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(LESpacing.sm))
                        Column {
                            Text(
                                text = "Phục hồi sao lưu (Restore Backup)",
                                style = LETypography.paneTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Nạp bản sao lưu .lebak từ thiết bị khác. Hệ thống tự động tạo bản sao an toàn trước khi khôi phục.",
                                style = LETypography.caption,
                                color = LEColors.textSecondary
                            )
                        }
                    }
                    if (!state.isRestoring) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.md))

                if (state.restoreSuccessSummary != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.sm)
                    ) {
                        Row(modifier = Modifier.padding(LESpacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            Column {
                                Text(
                                    text = "Khôi phục dữ liệu thành công!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = state.restoreSuccessSummary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                state.errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = LESpacing.xs)
                    )
                }

                if (state.preview == null && state.restoreSuccessSummary == null) {
                    // File selection state
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceSubtle),
                        modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.md)
                    ) {
                        Column(
                            modifier = Modifier.padding(LESpacing.xl).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(LESpacing.sm))
                            Text(
                                text = "Chọn tệp sao lưu (.lebak) để xem trước thông tin",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(LESpacing.md))
                            LEPrimaryButton(
                                text = "Chọn tệp .lebak...",
                                onClick = onSelectFile
                            )
                        }
                    }
                } else if (state.preview != null && state.restoreSuccessSummary == null) {
                    // Preview state
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(LESpacing.md)) {
                            Text(
                                text = "Thông tin bản sao lưu:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(LESpacing.xs))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nền tảng xuất:", style = MaterialTheme.typography.bodySmall)
                                Text(state.preview.sourcePlatform.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Thời điểm tạo:", style = MaterialTheme.typography.bodySmall)
                                Text(state.preview.createdAtUtc, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Số lượng thẻ từ vựng:", style = MaterialTheme.typography.bodySmall)
                                Text("${state.preview.counts.contents} thẻ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tệp media & âm thanh:", style = MaterialTheme.typography.bodySmall)
                                Text("${state.preview.counts.mediaFiles} file (${state.preview.bytes.mediaBytes / 1024} KB)", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lịch sử ôn tập (FSRS):", style = MaterialTheme.typography.bodySmall)
                                Text("${state.preview.counts.reviewEvents} lượt ôn", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(LESpacing.sm))

                    // Safety Warning Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(LESpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            Text(
                                text = "Lưu ý: Quá trình khôi phục sẽ thay thế dữ liệu hiện tại bằng nội dung bản sao lưu. Một bản sao an toàn (safety backup) sẽ được tự động tạo trước khi ghi đè.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.lg))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LESecondaryButton(
                        text = if (state.restoreSuccessSummary != null) "Đóng" else "Hủy",
                        onClick = onDismiss,
                        enabled = !state.isRestoring
                    )
                    if (state.preview != null && state.restoreSuccessSummary == null) {
                        Spacer(modifier = Modifier.width(LESpacing.sm))
                        LEPrimaryButton(
                            text = if (state.isRestoring) "Đang khôi phục..." else "Xác nhận khôi phục",
                            onClick = onConfirmRestore,
                            enabled = !state.isRestoring
                        )
                    }
                }
            }
        }
    }
}
