package vn.loi.learning.desktop.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
fun DesktopBackupDialog(
    state: DesktopBackupDialogState,
    onToggleSelectAll: (Boolean) -> Unit,
    onTogglePackage: (String, Boolean) -> Unit,
    onToggleIncludeProgress: (Boolean) -> Unit,
    onExecuteBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.55f)
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
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(LESpacing.sm))
                        Column {
                            Text(
                                text = "Sao lưu toàn diện (Portable Backup)",
                                style = LETypography.paneTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tạo tệp sao lưu .lebak chứa toàn bộ gói học, media và tiến độ để chuyển sang Android / PC khác.",
                                style = LETypography.caption,
                                color = LEColors.textSecondary
                            )
                        }
                    }
                    if (!state.isExporting) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.md))

                if (state.exportSuccessPath != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.sm)
                    ) {
                        Row(modifier = Modifier.padding(LESpacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                            Column {
                                Text(
                                    text = "Đã tạo bản sao lưu thành công!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = state.exportSuccessPath,
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

                // Scope selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isExporting) { onToggleSelectAll(!state.selectAllPackages) }
                        .padding(vertical = LESpacing.xs)
                ) {
                    Checkbox(
                        checked = state.selectAllPackages,
                        onCheckedChange = { onToggleSelectAll(it) },
                        enabled = !state.isExporting
                    )
                    Spacer(modifier = Modifier.width(LESpacing.xs))
                    Text(
                        text = "Sao lưu tất cả gói học (${state.availablePackages.size} gói)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Package list if not selectAll
                if (!state.selectAllPackages) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceSubtle),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(LESpacing.xs)) {
                            items(state.availablePackages, key = { it.id }) { pkg ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !state.isExporting) { onTogglePackage(pkg.id, !pkg.isSelected) }
                                        .padding(horizontal = LESpacing.sm, vertical = LESpacing.xs)
                                ) {
                                    Checkbox(
                                        checked = pkg.isSelected,
                                        onCheckedChange = { onTogglePackage(pkg.id, it) },
                                        enabled = !state.isExporting
                                    )
                                    Spacer(modifier = Modifier.width(LESpacing.xs))
                                    Text(
                                        text = "${pkg.name} (${pkg.cardCount} thẻ)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.sm))

                // Include learning progress toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isExporting) { onToggleIncludeProgress(!state.includeLearningProgress) }
                        .padding(vertical = LESpacing.xs)
                ) {
                    Checkbox(
                        checked = state.includeLearningProgress,
                        onCheckedChange = { onToggleIncludeProgress(it) },
                        enabled = !state.isExporting
                    )
                    Spacer(modifier = Modifier.width(LESpacing.xs))
                    Column {
                        Text(
                            text = "Bao gồm tiến độ học và lịch sử ôn tập (FSRS)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Đồng bộ điểm nhớ, khoảng cách ôn và các lượt đánh giá thẻ từ vựng.",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )
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
                        text = if (state.exportSuccessPath != null) "Đóng" else "Hủy",
                        onClick = onDismiss,
                        enabled = !state.isExporting
                    )
                    Spacer(modifier = Modifier.width(LESpacing.sm))
                    LEPrimaryButton(
                        text = if (state.isExporting) "Đang tạo bản sao lưu..." else "Bắt đầu sao lưu (.lebak)",
                        onClick = onExecuteBackup,
                        enabled = !state.isExporting && (state.selectAllPackages || state.availablePackages.any { it.isSelected })
                    )
                }
            }
        }
    }
}
