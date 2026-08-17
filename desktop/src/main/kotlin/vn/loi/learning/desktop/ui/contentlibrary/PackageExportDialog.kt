package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.desktop.ui.designsystem.LEColors

@Composable
fun PackageExportDialog(
    state: PackageExportDialogState,
    onDismiss: () -> Unit
) {
    if (!state.visible) return

    val summaryDescription = when {
        state.exporting -> "Exporting ${state.packageName}, ${state.percentageText} complete"
        state.result != null -> "Export completed for ${state.packageName}"
        state.error != null -> "Export failed for ${state.packageName}"
        else -> "Package export"
    }

    AlertDialog(
        onDismissRequest = { if (!state.exporting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.semantics { heading() }
            ) {
                when {
                    state.exporting -> {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LEColors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = LEColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Exporting Package",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LEColors.textPrimary
                            )
                            Text(
                                text = state.packageName,
                                fontSize = 13.sp,
                                color = LEColors.textSecondary
                            )
                        }
                    }
                    state.result != null -> {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LEColors.success.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LEColors.success,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Export Completed",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LEColors.success
                            )
                            Text(
                                text = state.packageName,
                                fontSize = 13.sp,
                                color = LEColors.textSecondary
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LEColors.danger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = LEColors.danger,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Export Failed",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LEColors.danger
                            )
                            Text(
                                text = state.packageName,
                                fontSize = 13.sp,
                                color = LEColors.textSecondary
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = summaryDescription }
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    state.exporting -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.phase.ifBlank { "Exporting package data..." },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LEColors.textPrimary
                                )
                                Text(
                                    text = state.percentageText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LEColors.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { state.fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = LEColors.primary,
                                trackColor = LEColors.primary.copy(alpha = 0.15f)
                            )
                            Text(
                                text = "Export in progress — please wait",
                                fontSize = 12.sp,
                                color = LEColors.textSecondary
                            )
                        }
                    }

                    state.result != null -> {
                        val result = state.result
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = LEColors.surfaceElevated.copy(alpha = 0.65f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Destination:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LEColors.textSecondary
                                )
                                Text(
                                    text = state.outputPath?.toAbsolutePath()?.toString() ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LEColors.textPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column {
                                        Text("Contents", fontSize = 11.sp, color = LEColors.textSecondary)
                                        Text("${result.contentCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                                    }
                                    Column {
                                        Text("Learning Items", fontSize = 11.sp, color = LEColors.textSecondary)
                                        Text("${result.learningItemCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                                    }
                                    Column {
                                        Text("Media Files", fontSize = 11.sp, color = LEColors.textSecondary)
                                        Text("${result.mediaAssetCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                                    }
                                    state.outputPath?.let { p ->
                                        if (Files.exists(p)) {
                                            val sizeKb = Files.size(p) / 1024
                                            Column {
                                                Text("Size", fontSize = 11.sp, color = LEColors.textSecondary)
                                                Text("${sizeKb} KB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LEColors.textPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    state.error != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = LEColors.dangerContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Error details:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LEColors.dangerText
                                )
                                Text(
                                    text = state.error,
                                    fontSize = 13.sp,
                                    color = LEColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                state.exporting -> {
                    // Export in progress - dismiss disabled
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Exporting…")
                    }
                }
                state.result != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.outputPath?.let { path ->
                            OutlinedButton(
                                onClick = { openContainingFolder(path) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Folder")
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = LEColors.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    )
}

private fun openContainingFolder(filePath: Path) {
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            val parent = filePath.parent ?: filePath
            if (desktop.isSupported(Desktop.Action.OPEN) && Files.exists(parent)) {
                desktop.open(parent.toFile())
            }
        }
    } catch (_: Exception) {
    }
}
