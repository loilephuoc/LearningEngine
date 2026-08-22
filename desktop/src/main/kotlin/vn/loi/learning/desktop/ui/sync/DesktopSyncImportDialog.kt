package vn.loi.learning.desktop.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Sync
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
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy

@Composable
fun DesktopSyncImportDialog(
    state: DesktopSyncImportDialogState,
    onSelectFile: () -> Unit,
    onStrategySelected: (ConflictResolutionStrategy) -> Unit,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isImporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.65f)
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
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(LESpacing.sm))
                        Column {
                            Text(
                                text = "Nạp gói đồng bộ vi sai (Import Sync)",
                                style = LETypography.paneTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Xem trước chi tiết thay đổi thẻ từ vựng, âm thanh và lượt ôn tập trước khi áp dụng.",
                                style = LETypography.caption,
                                color = LEColors.textSecondary
                            )
                        }
                    }
                    if (!state.isImporting) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.md))

                if (state.importSuccessSummary != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.sm)
                    ) {
                        Row(modifier = Modifier.padding(LESpacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            Column {
                                Text(
                                    text = "Đã nạp và hòa giải đồng bộ thành công!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = state.importSuccessSummary.message,
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

                if (state.previewReport == null && state.importSuccessSummary == null) {
                    // Pick file state
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
                                text = "Chọn tệp đồng bộ vi sai (.lesync)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(LESpacing.md))
                            LEPrimaryButton(
                                text = "Chọn tệp .lesync...",
                                onClick = onSelectFile
                            )
                        }
                    }
                } else if (state.previewReport != null && state.importSuccessSummary == null) {
                    // Preview report
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(LESpacing.md)) {
                            Text(
                                text = "Xem trước thay đổi (Sync Preview):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(LESpacing.xs))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nguồn gửi:", style = MaterialTheme.typography.bodySmall)
                                Text(state.previewReport.sourcePlatform.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gói học ảnh hưởng:", style = MaterialTheme.typography.bodySmall)
                                Text(state.previewReport.packagesAffected.joinToString().ifEmpty { "Tất cả" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Thẻ từ vựng cập nhật:", style = MaterialTheme.typography.bodySmall)
                                Text("${state.previewReport.contentChangesCount} thẻ", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tệp media mới:", style = MaterialTheme.typography.bodySmall)
                                Text("${state.previewReport.newMediaCount} file (${state.previewReport.newMediaBytes / 1024} KB)", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Media có sẵn (tái sử dụng):", style = MaterialTheme.typography.bodySmall)
                                Text("${state.previewReport.existingMediaReusedCount} file (không truyền lại)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lượt ôn tập mới / Trùng lặp:", style = MaterialTheme.typography.bodySmall)
                                Text("${state.previewReport.reviewEventsCount} mới / ${state.previewReport.reviewEventsDeduplicatedCount} bỏ qua", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (state.previewReport.requiresFullBackup) {
                        Spacer(modifier = Modifier.height(LESpacing.xs))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(LESpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(LESpacing.sm))
                                Text(
                                    text = state.previewReport.warnings.firstOrNull() ?: "Thiết bị thiếu baseline cho gói này. Cần sao lưu toàn diện trước.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (state.previewReport.conflicts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(LESpacing.sm))
                        Text(
                            text = "Phát hiện ${state.previewReport.conflicts.size} xung đột nội dung chỉnh sửa song song:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(LESpacing.xs))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LEColors.surfaceSubtle),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(LESpacing.xs)) {
                                items(state.previewReport.conflicts) { conflict ->
                                    Column(modifier = Modifier.padding(LESpacing.xs)) {
                                        Text("Thẻ: ${conflict.entityId} (Trường: ${conflict.fieldName})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("• Bản hiện tại trên máy: \"${conflict.localValueSummary}\"", style = MaterialTheme.typography.bodySmall)
                                        Text("• Bản nhận từ gói sync: \"${conflict.incomingValueSummary}\"", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(LESpacing.sm))

                        Text(
                            text = "Chọn phương thức hòa giải xung đột:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.conflictStrategy == ConflictResolutionStrategy.MERGE_FIELD_LEVEL,
                                onClick = { onStrategySelected(ConflictResolutionStrategy.MERGE_FIELD_LEVEL) },
                                enabled = !state.isImporting
                            )
                            Text("Hòa giải từng trường (Merge field-level - Khuyến nghị)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.conflictStrategy == ConflictResolutionStrategy.PRESERVE_LOCAL,
                                onClick = { onStrategySelected(ConflictResolutionStrategy.PRESERVE_LOCAL) },
                                enabled = !state.isImporting
                            )
                            Text("Giữ nguyên bản máy này (Keep local)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.conflictStrategy == ConflictResolutionStrategy.APPLY_INCOMING,
                                onClick = { onStrategySelected(ConflictResolutionStrategy.APPLY_INCOMING) },
                                enabled = !state.isImporting
                            )
                            Text("Ghi đè bằng bản mới từ gói sync (Apply incoming)", style = MaterialTheme.typography.bodySmall)
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
                        text = if (state.importSuccessSummary != null) "Đóng" else "Hủy",
                        onClick = onDismiss,
                        enabled = !state.isImporting
                    )
                    if (state.previewReport != null && state.importSuccessSummary == null) {
                        Spacer(modifier = Modifier.width(LESpacing.sm))
                        LEPrimaryButton(
                            text = if (state.isImporting) "Đang áp dụng..." else "Áp dụng thay đổi",
                            onClick = onConfirmImport,
                            enabled = !state.isImporting
                        )
                    }
                }
            }
        }
    }
}
