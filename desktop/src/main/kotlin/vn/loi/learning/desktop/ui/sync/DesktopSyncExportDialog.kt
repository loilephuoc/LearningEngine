package vn.loi.learning.desktop.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
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
fun DesktopSyncExportDialog(
    state: DesktopSyncExportDialogState,
    onTogglePackage: (String) -> Unit,
    onToggleIncludeReviews: (Boolean) -> Unit,
    onExecuteExport: () -> Unit,
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
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(LESpacing.sm))
                        Column {
                            Text(
                                text = "Xuất gói đồng bộ vi sai (Export Sync)",
                                style = LETypography.paneTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tạo tệp .lesync nhẹ chỉ chứa nội dung thay đổi, audio mới và lượt ôn tập.",
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
                                    text = "Đã xuất gói đồng bộ vi sai thành công!",
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

                // Baseline Info Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.xs)
                ) {
                    Row(modifier = Modifier.padding(LESpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(LESpacing.sm))
                        Text(
                            text = "Quy tắc baseline: Chỉ xuất .lesync cho các gói học đã có trên thiết bị nhận. Nếu thiết bị nhận chưa có gói học này, vui lòng dùng Sao lưu toàn diện (.lebak) trước.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.sm))

                Text(
                    text = "Chọn phạm vi gói học cần đồng bộ (${state.selectedPackageIds.size}/${state.availablePackages.size} đã chọn):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(LESpacing.xs))

                Card(
                    colors = CardDefaults.cardColors(containerColor = LEColors.surfaceSubtle),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(LESpacing.xs)) {
                        items(state.availablePackages, key = { it.id }) { pkg ->
                            val isChecked = pkg.packageId in state.selectedPackageIds || pkg.id in state.selectedPackageIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.isExporting) { onTogglePackage(pkg.packageId) }
                                    .padding(horizontal = LESpacing.sm, vertical = LESpacing.xs)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onTogglePackage(pkg.packageId) },
                                    enabled = !state.isExporting
                                )
                                Spacer(modifier = Modifier.width(LESpacing.xs))
                                Column {
                                    Text(
                                        text = pkg.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "ID: ${pkg.packageId} • ${pkg.cardCount} thẻ",
                                        style = LETypography.caption,
                                        color = LEColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isExporting) { onToggleIncludeReviews(!state.includeReviewEvents) }
                        .padding(vertical = LESpacing.xs)
                ) {
                    Checkbox(
                        checked = state.includeReviewEvents,
                        onCheckedChange = { onToggleIncludeReviews(it) },
                        enabled = !state.isExporting
                    )
                    Spacer(modifier = Modifier.width(LESpacing.xs))
                    Text(
                        text = "Đồng bộ cả lượt ôn tập mới trên các gói đã chọn",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                        text = if (state.isExporting) "Đang xuất gói..." else "Xuất thay đổi (.lesync)",
                        onClick = onExecuteExport,
                        enabled = !state.isExporting && state.selectedPackageIds.isNotEmpty()
                    )
                }
            }
        }
    }
}
