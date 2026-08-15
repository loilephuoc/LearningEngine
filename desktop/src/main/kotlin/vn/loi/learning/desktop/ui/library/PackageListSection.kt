package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onCheckPackageIntegrity: ((String) -> Unit)? = null,
    integrityScanningPackageId: String? = null,
    integrityScanBusy: Boolean = false,
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
            // Keep the current active package at the top so the primary learning
            // context is always visible without scrolling. Only one package is expanded
            // at a time; changing the active package automatically expands the new active one.
            var expandedPackageId by remember(activePackageId) { mutableStateOf(activePackageId) }
            val displayedPackages = packages.sortedByDescending { it.id == activePackageId }

            displayedPackages.forEach { pkg ->
                val originalIndex = packages.indexOfFirst { it.id == pkg.id }
                val isActive = pkg.id == activePackageId
                val progress = packageProgress[pkg.id] ?: PackageProgressPresentation.Unavailable

                val isExpanded = pkg.id == expandedPackageId

                if (isExpanded) {
                    PackageCard(
                        pkg = pkg,
                        progress = progress,
                        isActivePackage = isActive,
                        // The active package is visually pinned to the first position.
                        // For non-active expanded packages, keep the stored-order controls.
                        canMoveUp = !isActive && originalIndex > 0,
                        canMoveDown = !isActive && originalIndex in 0 until packages.lastIndex,
                        onToggleExpanded = { expandedPackageId = null },
                        onArchive = { onArchivePackage(pkg.id, pkg.name) },
                        onRestore = { onRestorePackage(pkg.id, pkg.name) },
                        onSetActive = { onSetActivePackage?.invoke(pkg.id) },
                        onMoveUp = { onMoveUpPackage?.invoke(pkg.id) },
                        onMoveDown = { onMoveDownPackage?.invoke(pkg.id) },
                        onOpenLibrary = onOpenLibrary,
                        onExportPackage = onExportPackage,
                        onRemovePackage = onRemovePackage,
                        onResetProgress = { onResetPackageProgress?.invoke(pkg.id, pkg.name) },
                        onCheckIntegrity = { onCheckPackageIntegrity?.invoke(pkg.packageId.value) },
                        integrityBusy = integrityScanBusy || integrityScanningPackageId == pkg.packageId.value,
                        packageExportChooser = packageExportChooser
                    )
                } else {
                    CompactPackageCard(
                        pkg = pkg,
                        progress = progress,
                        canMoveUp = originalIndex > 0,
                        canMoveDown = originalIndex in 0 until packages.lastIndex,
                        onExpand = { expandedPackageId = pkg.id },
                        onArchive = { onArchivePackage(pkg.id, pkg.name) },
                        onRestore = { onRestorePackage(pkg.id, pkg.name) },
                        onSetActive = { onSetActivePackage?.invoke(pkg.id) },
                        onMoveUp = { onMoveUpPackage?.invoke(pkg.id) },
                        onMoveDown = { onMoveDownPackage?.invoke(pkg.id) },
                        onOpenLibrary = onOpenLibrary,
                        onExportPackage = onExportPackage,
                        onRemovePackage = onRemovePackage,
                        packageExportChooser = packageExportChooser
                    )
                }
            }
        }
    }
}


@Composable
private fun CompactPackageCard(
    pkg: InstalledPackageSummary,
    progress: PackageProgressPresentation = PackageProgressPresentation.Unavailable,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onExpand: () -> Unit = {},
    onArchive: () -> Unit = {},
    onRestore: () -> Unit = {},
    onSetActive: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onOpenLibrary: ((InstalledPackageId, String) -> Unit)? = null,
    onExportPackage: ((InstalledPackageId, String, Path) -> Unit)? = null,
    onRemovePackage: ((String, String) -> Unit)? = null,
    packageExportChooser: (String) -> Path? = ::choosePackageExportDestination,
    modifier: Modifier = Modifier
) {
    val availableProgress = progress as? PackageProgressPresentation.Available

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LEColors.packageCardBackground),
        border = BorderStroke(1.dp, LEColors.packageCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LEColors.primarySoft,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = LEColors.primary,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onExpand)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pkg.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 17.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    PackageStateBadge(pkg.state)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Expand package",
                        tint = LEColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val totalWords = availableProgress?.totalLearningItemCount ?: pkg.learningItemCount
                val started = availableProgress?.startedItemCount
                val learned = availableProgress?.startedItemCount
                val summary = buildString {
                    append("${formatVietnameseCount(pkg.contentCount)} bài học · ${formatVietnameseCount(totalWords)} từ · v${pkg.version}")
                    if (started != null && learned != null) {
                        append("   ·   Đã bắt đầu ${formatVietnameseCount(started)}")
                        append("   ·   Đã học ${formatVietnameseCount(learned)}")
                    }
                }
                Text(
                    text = summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = LEColors.textSecondary,
                    fontSize = 12.sp
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onOpenLibrary != null && pkg.state == PackageState.ACTIVE) {
                    PackageCompactAction(
                        icon = Icons.Default.MenuBook,
                        label = "Browse",
                        primary = true,
                        onClick = { onOpenLibrary(pkg.id, pkg.name) }
                    )
                }
                if (pkg.state == PackageState.ACTIVE && onSetActive != null) {
                    PackageCompactSetActiveAction(onClick = onSetActive)
                }
                if (onExportPackage != null && (pkg.state == PackageState.ACTIVE || pkg.state == PackageState.ARCHIVED)) {
                    PackageCompactAction(Icons.Default.FileDownload, "Export", onClick = {
                        packageExportChooser(pkg.name)?.let { onExportPackage(pkg.id, pkg.name, it) }
                    })
                }
                if (onMoveUp != null && canMoveUp) PackageCompactAction(Icons.Default.ArrowUpward, "Up", onClick = onMoveUp)
                if (onMoveDown != null && canMoveDown) PackageCompactAction(Icons.Default.ArrowDownward, "Down", onClick = onMoveDown)
                when (pkg.state) {
                    PackageState.ACTIVE -> PackageCompactAction(Icons.Default.Archive, "Archive", onClick = onArchive)
                    PackageState.ARCHIVED -> PackageCompactAction(Icons.Default.Unarchive, "Restore", onClick = onRestore)
                    PackageState.REMOVED -> Unit
                }
                if (onRemovePackage != null) {
                    TextButton(
                        onClick = { onRemovePackage(pkg.packageId.value, pkg.name) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LEColors.danger,
                            containerColor = LEColors.dangerContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageCompactAction(
    icon: ImageVector,
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = if (primary) {
            ButtonDefaults.textButtonColors(
                contentColor = Color.White,
                containerColor = LEColors.primary
            )
        } else {
            ButtonDefaults.textButtonColors(
                contentColor = LEColors.textPrimary,
                containerColor = LEColors.surfaceElevated.copy(alpha = 0.65f)
            )
        },
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.height(34.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Icon(icon, null, Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PackageCard(
    pkg: InstalledPackageSummary,
    progress: PackageProgressPresentation = PackageProgressPresentation.Unavailable,
    isActivePackage: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onToggleExpanded: () -> Unit = {},
    onArchive: () -> Unit = {},
    onRestore: () -> Unit = {},
    onSetActive: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onOpenLibrary: ((InstalledPackageId, String) -> Unit)? = null,
    onExportPackage: ((InstalledPackageId, String, Path) -> Unit)? = null,
    onRemovePackage: ((String, String) -> Unit)? = null,
    onResetProgress: (() -> Unit)? = null,
    onCheckIntegrity: (() -> Unit)? = null,
    integrityBusy: Boolean = false,
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
                // Use the full card width so the ACTIVE badge can sit against the
                // package card's right edge instead of being capped by the old 1200.dp content width.
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PackageCardHeader(pkg, isActivePackage, availableProgress, onToggleExpanded)
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
            HorizontalDivider(color = LEColors.borderSubtle)
            PackageActionBar(
                pkg, isActivePackage, canMoveUp, canMoveDown, onArchive, onRestore,
                onSetActive, onMoveUp, onMoveDown, onOpenLibrary, onExportPackage,
                onRemovePackage, onResetProgress, packageExportChooser,
                onCheckIntegrity, integrityBusy
            )
        }
    }
}

@Composable
private fun PackageCardHeader(
    pkg: InstalledPackageSummary,
    isActivePackage: Boolean,
    progress: PackageProgressPresentation.Available?,
    onToggleExpanded: () -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(shape = RoundedCornerShape(14.dp), color = LEColors.primarySoft, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = LEColors.primary,
                modifier = Modifier.padding(13.dp).size(26.dp)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // fill=false keeps the rating chips immediately after the package name
                // instead of pushing them to the far-right edge of the card.
                Text(
                    pkg.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = LEColors.textPrimary
                )

                PackageHeaderRatingChips(progress?.latestRatings)

                Spacer(Modifier.weight(1f))

                progress?.let(::suspendedProgressLabel)?.let { suspendedLabel ->
                    CompactStatusChip(suspendedLabel, LEColors.warningContainer, LEColors.warningText)
                }

                // Keep the expand/collapse affordance before the status badge so ACTIVE
                // remains the final, right-most element just like the original layout.
                Icon(
                    Icons.Default.ExpandLess,
                    contentDescription = "Collapse package",
                    tint = LEColors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                if (isActivePackage) {
                    ActivePackageBadge()
                } else {
                    PackageStateBadge(pkg.state)
                }
            }

            Text(
                "${formatVietnameseCount(pkg.contentCount)} bài học · " +
                        "${formatVietnameseCount(progress?.totalLearningItemCount ?: pkg.learningItemCount)} từ · v${pkg.version}",
                maxLines = 1,
                color = LEColors.textSecondary,
                fontSize = 14.sp
            )
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
        val columns = when { maxWidth >= 1000.dp -> 6; maxWidth >= 620.dp -> 3; else -> 2 }
        FlowRow(
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = "Sáu chỉ số tiến độ học" },
            maxItemsInEachRow = columns
        ) {
            metrics.forEachIndexed { index, metric ->
                Column(
                    modifier = Modifier.weight(1f)
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
private fun PackageHeaderRatingChips(ratings: PackageLatestRatingPresentation?) {
    // Keep the four rating counters on the same line as the package name.
    // Showing zeroes when there is no rating history keeps the header stable and compact.
    val again = ratings?.againCount ?: 0
    val hard = ratings?.hardCount ?: 0
    val good = ratings?.goodCount ?: 0
    val easy = ratings?.easyCount ?: 0

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        RatingChip(Icons.Default.Replay, "Again", again, LEColors.ratingAgainTint, LEColors.metricRed, compact = true)
        RatingChip(Icons.Default.TrendingDown, "Hard", hard, LEColors.ratingHardTint, LEColors.metricOrange, compact = true)
        RatingChip(Icons.Default.ThumbUp, "Good", good, LEColors.ratingGoodTint, LEColors.metricBlue, compact = true)
        RatingChip(Icons.Default.Bolt, "Easy", easy, LEColors.ratingEasyTint, LEColors.metricGreen, compact = true)
    }
}

@Composable
private fun RatingChip(
    icon: ImageVector,
    label: String,
    count: Int,
    background: Color,
    foreground: Color,
    compact: Boolean = false
) {
    Surface(shape = RoundedCornerShape(18.dp), color = background) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp)
            )
            Text(
                "$label ${formatVietnameseCount(count)}",
                fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = foreground
            )
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
    packageExportChooser: (String) -> Path?,
    onCheckIntegrity: (() -> Unit)?,
    integrityBusy: Boolean
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
        if (onCheckIntegrity != null && pkg.state != PackageState.REMOVED) {
            PackageSecondaryAction(
                Icons.Default.CheckCircleOutline,
                if (integrityBusy) "Checking…" else "Check Integrity",
                onClick = onCheckIntegrity,
                enabled = !integrityBusy
            )
        }
        if (pkg.state == PackageState.ACTIVE && onSetActive != null && !isActivePackage) {
            PackageSetActiveAction(onClick = onSetActive)
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
private fun PackageSecondaryAction(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
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
private fun ActivePackageBadge() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LEColors.successContainer,
        border = BorderStroke(1.dp, LEColors.successText.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = LEColors.successText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "ACTIVE",
                color = LEColors.successText,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PackageSetActiveAction(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = LEColors.successContainer,
            contentColor = LEColors.successText
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Set Active", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PackageCompactSetActiveAction(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = LEColors.successContainer,
            contentColor = LEColors.successText
        ),
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.height(34.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text("Set Active", maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
