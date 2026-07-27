package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.loi.learning.application.library.query.InstalledPackageSummary
import vn.loi.learning.desktop.ui.designsystem.LEColors
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
    val availableProgress = progress as? PackageProgressPresentation.Available
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LEColors.packageCardBackground),
        border = BorderStroke(1.dp, LEColors.packageCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1200.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PackageCardHeader(pkg, isActivePackage, availableProgress)
            if (availableProgress == null) {
                Text(
                    "Không thể tải tiến độ",
                    color = LEColors.warningText,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                PackageMetricRow(availableProgress)
                PackageStartedProgress(availableProgress)
            }
            PackageLatestRating(availableProgress?.latestRatings)
            HorizontalDivider(color = LEColors.borderSubtle)
            PackageActionBar(
                pkg, isActivePackage, canMoveUp, canMoveDown, onArchive, onRestore,
                onSetActive, onMoveUp, onMoveDown, onOpenLibrary, onExportPackage,
                onRemovePackage, onResetProgress, packageExportChooser
            )
        }
    }
}

@Composable
private fun PackageCardHeader(
    pkg: InstalledPackageSummary,
    isActivePackage: Boolean,
    progress: PackageProgressPresentation.Available?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(14.dp), color = LEColors.primarySoft, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = LEColors.primary,
                modifier = Modifier.padding(13.dp).size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                pkg.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold,
                color = LEColors.textPrimary
            )
            Text(
                "${formatVietnameseCount(pkg.contentCount)} bài học · " +
                    "${formatVietnameseCount(progress?.totalLearningItemCount ?: pkg.learningItemCount)} từ · v${pkg.version}",
                maxLines = 1, color = LEColors.textSecondary,
                fontSize = 14.sp
            )
        }
        progress?.let(::suspendedProgressLabel)?.let { suspendedLabel ->
            CompactStatusChip(suspendedLabel, LEColors.warningContainer, LEColors.warningText)
            Spacer(Modifier.width(8.dp))
        }
        if (isActivePackage) {
            CompactStatusChip("● ACTIVE", LEColors.successContainer, LEColors.successText)
        } else {
            PackageStateBadge(pkg.state)
        }
    }
}

private data class PackageMetric(val label: String, val value: Int, val icon: ImageVector, val color: Color)

@Composable
private fun PackageMetricRow(progress: PackageProgressPresentation.Available) {
    val icons = listOf(Icons.Default.List, Icons.Default.Circle, Icons.Default.AutoStories, Icons.Default.Schedule, Icons.Default.CheckCircleOutline, Icons.Default.Verified)
    val colors = listOf(LEColors.metricPurple, LEColors.metricNeutral, LEColors.metricOrange, LEColors.metricRed, LEColors.metricBlue, LEColors.metricGreen)
    val metrics = packageMetrics(progress).mapIndexed { index, metric ->
        PackageMetric(metric.label, metric.value, icons[index], colors[index])
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val standardWidth = maxWidth >= 900.dp
        Row(
            modifier = (if (standardWidth) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState()))
                .semantics { contentDescription = "Sáu chỉ số tiến độ học" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            metrics.forEachIndexed { index, metric ->
                if (index > 0) VerticalDivider(Modifier.height(64.dp), color = LEColors.borderSubtle)
                Column(
                    modifier = (if (standardWidth) Modifier.weight(1f) else Modifier.width(150.dp))
                        .height(PACKAGE_METRIC_ROW_HEIGHT)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(metric.icon, contentDescription = null, tint = metric.color.copy(alpha = 0.86f), modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(metric.label, maxLines = 1, fontSize = PACKAGE_METRIC_LABEL_FONT_SIZE, color = LEColors.textPrimary)
                    Text(formatVietnameseCount(metric.value), maxLines = 1, fontSize = PACKAGE_METRIC_VALUE_FONT_SIZE, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = metric.color)
                }
            }
        }
    }
}

@Composable
private fun PackageStartedProgress(progress: PackageProgressPresentation.Available) {
    Surface(color = LEColors.progressSurface, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text("Đã bắt đầu ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LEColors.textPrimary)
                    Text(formatVietnameseCount(progress.startedItemCount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LEColors.primary)
                    Text(" / ${formatVietnameseCount(progress.totalLearningItemCount)} từ", fontSize = 14.sp, color = LEColors.textSecondary)
                }
                Text(
                    formatVietnameseProgress(progress.startedItemCount, progress.totalLearningItemCount),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LEColors.primary
                )
            }
            LinearProgressIndicator(
                progress = { progress.startedRatio.toFloat().coerceIn(0f, 1f) },
                color = LEColors.primary,
                trackColor = LEColors.progressTrack,
                modifier = Modifier.fillMaxWidth().height(PACKAGE_PROGRESS_BAR_HEIGHT).clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun PackageLatestRating(ratings: PackageLatestRatingPresentation?) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Đánh giá gần nhất", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LEColors.textPrimary)
        if (ratings == null || ratings.ratedItemCount == 0) {
            Text(PACKAGE_RATING_UNAVAILABLE_LABEL, fontSize = 13.sp, color = LEColors.textMuted)
        } else {
            RatingChip(Icons.Default.Replay, "Again", ratings.againCount, LEColors.ratingAgainTint, LEColors.metricRed)
            RatingChip(Icons.Default.TrendingDown, "Hard", ratings.hardCount, LEColors.ratingHardTint, LEColors.metricOrange)
            RatingChip(Icons.Default.ThumbUp, "Good", ratings.goodCount, LEColors.ratingGoodTint, LEColors.metricBlue)
            RatingChip(Icons.Default.Bolt, "Easy", ratings.easyCount, LEColors.ratingEasyTint, LEColors.metricGreen)
        }
    }
}

@Composable
private fun RatingChip(icon: ImageVector, label: String, count: Int, background: Color, foreground: Color) {
    Surface(shape = RoundedCornerShape(18.dp), color = background) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
            Text("$label ${formatVietnameseCount(count)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = foreground)
        }
    }
}

@Composable
private fun PackageActionBar(
    pkg: InstalledPackageSummary,
    isActivePackage: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onSetActive: (() -> Unit)?,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onOpenLibrary: ((InstalledPackageId, String) -> Unit)?,
    onExportPackage: ((InstalledPackageId, String, Path) -> Unit)?,
    onRemovePackage: ((String, String) -> Unit)?,
    onResetProgress: (() -> Unit)?,
    packageExportChooser: (String) -> Path?
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onOpenLibrary != null && pkg.state == PackageState.ACTIVE) {
            Button(
                onClick = { onOpenLibrary(pkg.id, pkg.name) },
                colors = ButtonDefaults.buttonColors(containerColor = LEColors.primary),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.MenuBook, null, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Browse Lessons", maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (onExportPackage != null && (pkg.state == PackageState.ACTIVE || pkg.state == PackageState.ARCHIVED)) {
            PackageSecondaryAction(Icons.Default.FileDownload, "Export OPD3", onClick = {
                packageExportChooser(pkg.name)?.let { onExportPackage(pkg.id, pkg.name, it) }
            })
        }
        if (pkg.state == PackageState.ACTIVE && onSetActive != null && !isActivePackage) {
            PackageSecondaryAction(Icons.Default.RadioButtonChecked, "Set Active", onSetActive)
        }
        if (onMoveUp != null && canMoveUp) PackageSecondaryAction(Icons.Default.ArrowUpward, "Move Up", onMoveUp)
        if (onMoveDown != null && canMoveDown) PackageSecondaryAction(Icons.Default.ArrowDownward, "Move Down", onMoveDown)
        if (onResetProgress != null && pkg.state == PackageState.ACTIVE) {
            PackageSecondaryAction(Icons.Default.RestartAlt, "Đặt lại tiến độ", onResetProgress)
        }
        when (pkg.state) {
            PackageState.ACTIVE -> PackageSecondaryAction(Icons.Default.Archive, "Archive", onArchive)
            PackageState.ARCHIVED -> PackageSecondaryAction(Icons.Default.Unarchive, "Restore", onRestore)
            PackageState.REMOVED -> Unit
        }
        if (onRemovePackage != null) {
            TextButton(
                onClick = { onRemovePackage(pkg.packageId.value, pkg.name) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LEColors.danger,
                    containerColor = LEColors.dangerContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Remove Topic", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PackageSecondaryAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = LEColors.textPrimary,
            containerColor = LEColors.surfaceElevated.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Icon(icon, null, Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CompactStatusChip(text: String, background: Color, foreground: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = background) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
