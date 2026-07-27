package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.library.query.InstalledPackageSummary
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageState

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.nio.file.Path

@Composable
fun PackageListSection(
    title: String,
    packages: List<InstalledPackageSummary>,
    packageProgress: Map<InstalledPackageId, PackageProgressPresentation> = emptyMap(),
    activePackageId: InstalledPackageId? = null,
    onArchivePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onRestorePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onSetActivePackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveUpPackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveDownPackage: ((InstalledPackageId) -> Unit)? = null,
    onOpenLibrary: ((InstalledPackageId, String) -> Unit)? = null,
    onExportPackage: ((InstalledPackageId, String, Path) -> Unit)? = null,
    onRemovePackage: ((String, String) -> Unit)? = null,
    onResetPackageProgress: ((InstalledPackageId, String) -> Unit)? = null,
    packageExportChooser: (String) -> Path? = ::choosePackageExportDestination,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (packages.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "No packages in this section.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            packages.forEachIndexed { index, pkg ->
                PackageCard(
                    pkg = pkg,
                    progress = packageProgress[pkg.id] ?: PackageProgressPresentation.Unavailable,
                    isActivePackage = pkg.id == activePackageId,
                    canMoveUp = index > 0,
                    canMoveDown = index < packages.size - 1,
                    onArchive = { onArchivePackage(pkg.id, pkg.name) },
                    onRestore = { onRestorePackage(pkg.id, pkg.name) },
                    onSetActive = { onSetActivePackage?.invoke(pkg.id) },
                    onMoveUp = { onMoveUpPackage?.invoke(pkg.id) },
                    onMoveDown = { onMoveDownPackage?.invoke(pkg.id) },
                    onOpenLibrary = onOpenLibrary,
                    onExportPackage = onExportPackage,
                    onRemovePackage = onRemovePackage,
                    onResetProgress = { onResetPackageProgress?.invoke(pkg.id, pkg.name) },
                    packageExportChooser = packageExportChooser
                )
            }
        }
    }
}

@Composable
fun PackageCard(
    pkg: InstalledPackageSummary,
    progress: PackageProgressPresentation = PackageProgressPresentation.Unavailable,
    isActivePackage: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onArchive: () -> Unit = {},
    onRestore: () -> Unit = {},
    onSetActive: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onOpenLibrary: ((InstalledPackageId, String) -> Unit)? = null,
    onExportPackage: ((InstalledPackageId, String, Path) -> Unit)? = null,
    onRemovePackage: ((String, String) -> Unit)? = null,
    onResetProgress: (() -> Unit)? = null,
    packageExportChooser: (String) -> Path? = ::choosePackageExportDestination,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PackageProgressSummary(progress)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pkg.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isActivePackage) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Current Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    PackageStateBadge(state = pkg.state)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Version: ${pkg.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Contents: ${pkg.contentCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Learning Items: ${pkg.learningItemCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onOpenLibrary != null && pkg.state == PackageState.ACTIVE) {
                        TextButton(onClick = { onOpenLibrary(pkg.id, pkg.name) }) {
                            Text("Browse Lessons", maxLines = 1, softWrap = false)
                        }
                    }
                    if (onExportPackage != null && (pkg.state == PackageState.ACTIVE || pkg.state == PackageState.ARCHIVED)) {
                        TextButton(
                            onClick = {
                                packageExportChooser(pkg.name)?.let { destPath ->
                                    onExportPackage(pkg.id, pkg.name, destPath)
                                }
                            }
                        ) {
                            Text("Export OPD3", maxLines = 1, softWrap = false)
                        }
                    }
                    if (pkg.state == PackageState.ACTIVE && onSetActive != null) {
                        if (isActivePackage) {
                            TextButton(onClick = {}, enabled = false) {
                                Text("Active", maxLines = 1, softWrap = false)
                            }
                        } else {
                            TextButton(onClick = onSetActive) {
                                Text("Set Active", maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    if (onMoveUp != null) {
                        TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                            Text("Move Up", maxLines = 1, softWrap = false)
                        }
                    }
                    if (onMoveDown != null) {
                        TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                            Text("Move Down", maxLines = 1, softWrap = false)
                        }
                    }
                    if (onResetProgress != null && pkg.state == PackageState.ACTIVE) {
                        TextButton(onClick = onResetProgress) {
                            Text("Đặt lại tiến độ", maxLines = 1, softWrap = false)
                        }
                    }
                    when (pkg.state) {
                        PackageState.ACTIVE -> {
                            TextButton(onClick = onArchive) {
                                Text("Archive", maxLines = 1, softWrap = false)
                            }
                        }
                        PackageState.ARCHIVED -> {
                            TextButton(onClick = onRestore) {
                                Text("Restore", maxLines = 1, softWrap = false)
                            }
                        }
                        PackageState.REMOVED -> {}
                    }
                    if (onRemovePackage != null) {
                        TextButton(
                            onClick = { onRemovePackage(pkg.packageId.value, pkg.name) },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove Topic", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageProgressSummary(progress: PackageProgressPresentation) {
    when (progress) {
        PackageProgressPresentation.Unavailable ->
            Text(
                text = "Learning progress unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        is PackageProgressPresentation.Available -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Total ${progress.totalLearningItemCount} • Unseen ${progress.unseenItemCount} • New state ${progress.newStateItemCount} • Started ${progress.startedItemCount} • Due now ${progress.dueItemCount} • Mastered ${progress.masteredItemCount}" +
                        if (progress.suspendedItemCount > 0) " • Suspended ${progress.suspendedItemCount}" else "",
                    style = MaterialTheme.typography.bodySmall
                )
                LinearProgressIndicator(
                    progress = { progress.startedPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Started ${progress.startedItemCount} / ${progress.totalLearningItemCount} (${progress.startedPercent}%) • Mastered ${progress.completionPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun choosePackageExportDestination(defaultPackageName: String): Path? {
    val sanitized = defaultPackageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val defaultFileName = "$sanitized.opd3"
    val chooser = JFileChooser().apply {
        dialogTitle = "Export OPD3 Package Archive"
        selectedFile = java.io.File(defaultFileName)
        fileFilter = FileNameExtensionFilter("OPD3 Package (*.opd3)", "opd3")
    }
    val result = chooser.showSaveDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        var selected = chooser.selectedFile.toPath()
        if (!selected.toString().lowercase().endsWith(".opd3")) {
            selected = selected.parent?.resolve("${selected.fileName}.opd3") ?: selected
        }
        return selected
    }
    return null
}

@Composable
fun PackageStateBadge(
    state: PackageState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (state) {
        PackageState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PackageState.ARCHIVED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PackageState.REMOVED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = state.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
