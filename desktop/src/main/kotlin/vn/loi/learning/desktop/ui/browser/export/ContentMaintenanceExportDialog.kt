package vn.loi.learning.desktop.ui.browser.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import vn.loi.learning.desktop.platform.DesktopFileActions
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.LEPrimaryButton
import vn.loi.learning.desktop.ui.designsystem.components.LESecondaryButton

@Composable
fun ContentMaintenanceExportDialog(
    state: ContentMaintenanceExportState,
    availableScopes: Map<ContentMaintenanceExportScope, Int>,
    onSelectScope: (ContentMaintenanceExportScope) -> Unit,
    onTargetDirectoryChanged: (String) -> Unit,
    onTargetFileNameChanged: (String) -> Unit,
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
                .wrapContentHeight(),
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
                    Column {
                        Text(
                            text = "Export Content JSON",
                            style = LETypography.paneTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export canonical content data for external processing or review.",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !state.isExporting
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(LESpacing.md))
                HorizontalDivider(color = LEColors.borderSubtle)
                Spacer(modifier = Modifier.height(LESpacing.md))

                if (state.exportSuccessMessage != null) {
                    // Success View
                    Surface(
                        color = LEColors.successContainer,
                        shape = LERadius.md,
                        modifier = Modifier.fillMaxWidth().padding(vertical = LESpacing.sm)
                    ) {
                        Column(modifier = Modifier.padding(LESpacing.md)) {
                            Text(
                                text = "Export Completed Successfully",
                                style = LETypography.fieldValueEmphasized,
                                color = LEColors.success,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.exportSuccessMessage,
                                style = LETypography.caption,
                                color = LEColors.textPrimary
                            )
                            if (state.exportedFilePath != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.exportedFilePath,
                                    style = LETypography.caption,
                                    color = LEColors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(LESpacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (state.exportedFilePath != null) {
                            LESecondaryButton(
                                text = "Open Folder",
                                onClick = {
                                    DesktopFileActions.showInFolder(java.nio.file.Path.of(state.exportedFilePath))
                                }
                            )
                            Spacer(modifier = Modifier.width(LESpacing.sm))
                        }
                        LEPrimaryButton(
                            text = "Done",
                            onClick = onDismiss
                        )
                    }
                } else {
                    // Export Form
                    Text(
                        text = "Select Export Scope:",
                        style = LETypography.fieldLabel,
                        color = LEColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(LESpacing.xs))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableScopes.forEach { (scope, count) ->
                            val isSelected = state.selectedScope == scope
                            Surface(
                                shape = LERadius.sm,
                                color = if (isSelected) LEColors.primarySoft.copy(alpha = 0.5f) else LEColors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LEColors.primary else LEColors.borderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(LERadius.sm)
                                    .clickable(enabled = !state.isExporting) { onSelectScope(scope) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = LESpacing.md, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onSelectScope(scope) },
                                            enabled = !state.isExporting,
                                            colors = RadioButtonDefaults.colors(selectedColor = LEColors.primary)
                                        )
                                        Text(
                                            text = scope.label,
                                            style = LETypography.fieldValue,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    Text(
                                        text = "$count items",
                                        style = LETypography.caption,
                                        color = if (isSelected) LEColors.primary else LEColors.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(LESpacing.md))

                    // Destination Directory & File Name
                    Text(
                        text = "Destination Directory:",
                        style = LETypography.fieldLabel,
                        color = LEColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = state.targetDirectory,
                        onValueChange = onTargetDirectoryChanged,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LETypography.caption
                    )

                    Spacer(modifier = Modifier.height(LESpacing.sm))

                    Text(
                        text = "File Name:",
                        style = LETypography.fieldLabel,
                        color = LEColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = state.targetFileName,
                        onValueChange = onTargetFileNameChanged,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LETypography.caption
                    )

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(LESpacing.xs))
                        Text(
                            text = state.errorMessage,
                            style = LETypography.caption,
                            color = LEColors.danger
                        )
                    }

                    Spacer(modifier = Modifier.height(LESpacing.lg))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val count = availableScopes[state.selectedScope] ?: 0
                        Text(
                            text = "Exporting $count items",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                            LESecondaryButton(
                                text = "Cancel",
                                onClick = onDismiss,
                                enabled = !state.isExporting
                            )
                            LEPrimaryButton(
                                text = if (state.isExporting) "Exporting..." else "Export JSON",
                                onClick = onExecuteExport,
                                enabled = !state.isExporting && count > 0 && state.targetFileName.isNotBlank()
                            )
                        }
                    }
                }
            }
        }
    }
}
