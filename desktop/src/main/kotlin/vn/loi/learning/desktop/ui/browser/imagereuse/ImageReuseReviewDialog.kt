package vn.loi.learning.desktop.ui.browser.imagereuse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.application.contentmedia.MediaReferencePolicy
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.contentlibrary.ThumbnailResult
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

@Composable
fun ImageReuseReviewDialog(
    state: ImageReuseReviewDialogState,
    thumbnailLoader: LessonThumbnailLoader,
    contentMediaStorage: ContentMediaStorage? = null,
    onToggleSourcePackage: (String) -> Unit,
    onSelectAllSourcePackages: () -> Unit,
    onClearAllSourcePackages: () -> Unit,
    onScopeChanged: (ImageReuseScope) -> Unit,
    onStartScan: () -> Unit,
    onPreviousItem: () -> Unit,
    onUndoLastUse: () -> Unit,
    onSkipCandidate: () -> Unit,
    onSkipItem: () -> Unit,
    onApplyAndNext: () -> Unit,
    onClose: () -> Unit
) {
    if (!state.visible || state.stage == null) return

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.stage) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = {
            val stage = state.stage
            if (stage !is ImageReuseReviewStage.Review || !stage.isApplying) {
                onClose()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(min = 760.dp, max = 1400.dp)
                    .fillMaxHeight(0.92f)
                    .heightIn(min = 540.dp, max = 880.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, LEColors.borderSubtle, RoundedCornerShape(12.dp))
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val stage = state.stage
                        when {
                            keyEvent.key == Key.Escape -> {
                                if (stage !is ImageReuseReviewStage.Review || !stage.isApplying) {
                                    onClose()
                                    true
                                } else false
                            }
                            stage is ImageReuseReviewStage.Review && !stage.isApplying -> {
                                when {
                                    (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) && keyEvent.key == Key.Z -> {
                                        if (stage.canUndo) {
                                            onUndoLastUse()
                                            true
                                        } else false
                                    }
                                    keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter -> {
                                        onApplyAndNext()
                                        true
                                    }
                                    keyEvent.key == Key.Spacebar || keyEvent.key == Key.DirectionRight -> {
                                        onSkipCandidate()
                                        true
                                    }
                                    keyEvent.key == Key.DirectionLeft -> {
                                        if (stage.canGoPrevious) {
                                            onPreviousItem()
                                            true
                                        } else false
                                    }
                                    keyEvent.key == Key.N -> {
                                        onSkipItem()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            else -> false
                        }
                    },
                color = LEColors.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(LESpacing.lg)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = LEColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Image Reuse Review",
                                style = LETypography.paneTitle,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Target: ${state.targetPackageName}",
                            style = LETypography.secondaryMetadata,
                            color = LEColors.textSecondary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = LESpacing.md), color = LEColors.borderSubtle)

                    // Stage Body
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (val stage = state.stage) {
                            is ImageReuseReviewStage.Setup -> {
                                ImageReuseSetupView(
                                    stage = stage,
                                    targetPackageName = state.targetPackageName,
                                    onToggleSourcePackage = onToggleSourcePackage,
                                    onSelectAllSourcePackages = onSelectAllSourcePackages,
                                    onClearAllSourcePackages = onClearAllSourcePackages,
                                    onScopeChanged = onScopeChanged,
                                    onStartScan = onStartScan,
                                    onClose = onClose
                                )
                            }
                            is ImageReuseReviewStage.Review -> {
                                ImageReuseActiveReviewView(
                                    stage = stage,
                                    targetPackageName = state.targetPackageName,
                                    thumbnailLoader = thumbnailLoader,
                                    contentMediaStorage = contentMediaStorage,
                                    onPreviousItem = onPreviousItem,
                                    onUndoLastUse = onUndoLastUse,
                                    onSkipCandidate = onSkipCandidate,
                                    onSkipItem = onSkipItem,
                                    onApplyAndNext = onApplyAndNext,
                                    onClose = onClose
                                )
                            }
                            is ImageReuseReviewStage.Complete -> {
                                ImageReuseCompleteView(
                                    stage = stage,
                                    targetPackageName = state.targetPackageName,
                                    onClose = onClose
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageReuseSetupView(
    stage: ImageReuseReviewStage.Setup,
    targetPackageName: String,
    onToggleSourcePackage: (String) -> Unit,
    onSelectAllSourcePackages: () -> Unit,
    onClearAllSourcePackages: () -> Unit,
    onScopeChanged: (ImageReuseScope) -> Unit,
    onStartScan: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        // Target Package Info
        Surface(
            color = LEColors.surfaceElevated,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(LESpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = LEColors.primary, modifier = Modifier.size(18.dp))
                Column {
                    Text("Target Package (Mutable)", style = LETypography.caption, color = LEColors.textMuted)
                    Text(targetPackageName, style = LETypography.fieldValueEmphasized)
                }
            }
        }

        // Scope Selection
        Text("Scan Scope", style = LETypography.sectionTitle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
        ) {
            ImageReuseScope.entries.forEach { scopeOption ->
                val isSelected = stage.scope == scopeOption
                Surface(
                    color = if (isSelected) LEColors.primary.copy(alpha = 0.1f) else LEColors.surfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, LEColors.primary) else null,
                    modifier = Modifier.weight(1f).clickable { onScopeChanged(scopeOption) }
                ) {
                    Row(
                        modifier = Modifier.padding(LESpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onScopeChanged(scopeOption) },
                            colors = RadioButtonDefaults.colors(selectedColor = LEColors.primary)
                        )
                        Column {
                            Text(scopeOption.label, style = LETypography.fieldValueEmphasized)
                            Text(scopeOption.description, style = LETypography.caption, color = LEColors.textSecondary)
                        }
                    }
                }
            }
        }

        // Source Packages Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Source Packages (Read-Only)", style = LETypography.sectionTitle)
            Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                LESecondaryButton(text = "Select All", onClick = onSelectAllSourcePackages)
                LESecondaryButton(text = "Clear All", onClick = onClearAllSourcePackages)
            }
        }

        if (stage.availableSourcePackages.isEmpty()) {
            Text(
                "No other installed vocabulary packages found to reuse images from.",
                style = LETypography.secondaryMetadata,
                color = LEColors.textMuted
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .border(1.dp, LEColors.borderSubtle, RoundedCornerShape(8.dp))
                    .padding(LESpacing.sm)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                stage.availableSourcePackages.forEach { pkg ->
                    val isChecked = pkg.id in stage.selectedSourcePackageIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onToggleSourcePackage(pkg.id) }
                            .padding(horizontal = LESpacing.sm, vertical = LESpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleSourcePackage(pkg.id) },
                            colors = CheckboxDefaults.colors(checkedColor = LEColors.primary)
                        )
                        Text(pkg.name, style = LETypography.fieldValue, modifier = Modifier.weight(1f))
                        if (pkg.version.isNotBlank()) {
                            Text("v${pkg.version}", style = LETypography.caption, color = LEColors.textMuted)
                        }
                    }
                }
            }
        }

        stage.scanError?.let { error ->
            Text(error, style = LETypography.secondaryMetadata, color = LEColors.danger)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LESecondaryButton(text = "Cancel", onClick = onClose)
            Spacer(modifier = Modifier.width(LESpacing.sm))
            LEPrimaryButton(
                text = if (stage.isScanning) "Scanning..." else "Scan / Start Review",
                onClick = onStartScan,
                enabled = !stage.isScanning && stage.selectedSourcePackageIds.isNotEmpty()
            )
        }
    }
}

@Composable
private fun ImageReuseActiveReviewView(
    stage: ImageReuseReviewStage.Review,
    targetPackageName: String,
    thumbnailLoader: LessonThumbnailLoader,
    contentMediaStorage: ContentMediaStorage?,
    onPreviousItem: () -> Unit,
    onUndoLastUse: () -> Unit,
    onSkipCandidate: () -> Unit,
    onSkipItem: () -> Unit,
    onApplyAndNext: () -> Unit,
    onClose: () -> Unit
) {
    val target = stage.currentTarget
    val candidate = stage.currentCandidate

    if (target == null || candidate == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No remaining review items.", style = LETypography.fieldValue)
            Spacer(modifier = Modifier.height(LESpacing.md))
            LEPrimaryButton(text = "Close", onClick = onClose)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
    ) {
        // Progress Bar & Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Target ${stage.currentTargetIndex + 1} of ${stage.totalTargets}  ·  Candidate ${stage.currentCandidateIndex + 1} of ${stage.totalCandidatesForCurrentTarget}",
                style = LETypography.fieldValueEmphasized,
                color = LEColors.primary
            )
            Text(
                text = "Reused: ${stage.appliedCount}",
                style = LETypography.secondaryMetadata,
                color = LEColors.textSecondary
            )
        }

        stage.applyError?.let { err ->
            Text("Error applying image: $err", style = LETypography.secondaryMetadata, color = LEColors.danger)
        }

        // Split Comparison Area: Shared horizontal row comparison grid guaranteeing row-for-row alignment
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            // Headers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                Surface(
                    color = LEColors.surfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = 4.dp)) {
                        Text("TARGET", style = LETypography.caption, color = LEColors.primary, fontWeight = FontWeight.Bold)
                        Text(targetPackageName, style = LETypography.secondaryMetadata, color = LEColors.textMuted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }

                Surface(
                    color = LEColors.surfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, LEColors.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = LESpacing.sm, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SOURCE CANDIDATE", style = LETypography.caption, color = LEColors.primary, fontWeight = FontWeight.Bold)
                        Text(candidate.sourcePackageName, style = LETypography.secondaryMetadata, color = LEColors.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }

            // Shared Metadata Rows: Question, Answer, Translation, Example, POS
            val comparisonRows = ImageReuseComparisonProjection.createRows(target, candidate)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                comparisonRows.forEach { row ->
                    ComparisonRowItem(
                        label = row.fieldName,
                        targetValue = row.targetValue,
                        sourceValue = row.sourceValue
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Large Equal-Sized Image Previews Row
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                Surface(
                    color = LEColors.surfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(LEColors.surface)
                            .padding(LESpacing.xs),
                        contentAlignment = Alignment.Center
                    ) {
                        ReviewImagePreview(
                            packageName = targetPackageName,
                            reference = target.currentImageRef,
                            contentMediaStorage = contentMediaStorage,
                            thumbnailLoader = thumbnailLoader,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Surface(
                    color = LEColors.surfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, LEColors.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(LEColors.surface)
                            .padding(LESpacing.xs),
                        contentAlignment = Alignment.Center
                    ) {
                        ReviewImagePreview(
                            packageName = candidate.sourcePackageName,
                            reference = candidate.imageRef,
                            contentMediaStorage = contentMediaStorage,
                            thumbnailLoader = thumbnailLoader,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Bottom Shortcut Guidance & Actions
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            if (availableWidth >= 1150.dp) {
                // Wide layout: Shortcut guidance on the left, all 6 buttons in 1 row on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enter: Use & Next  ·  Space/→: Skip Candidate  ·  N: Skip Item  ·  ←: Prev Item  ·  Ctrl+Z: Undo  ·  Esc: Close",
                        style = LETypography.caption,
                        color = LEColors.textMuted,
                        modifier = Modifier.weight(1f, fill = false).padding(end = LESpacing.sm)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LESecondaryButton(
                            text = "Previous Item",
                            onClick = onPreviousItem,
                            enabled = stage.canGoPrevious
                        )
                        LESecondaryButton(
                            text = "Undo Last Use",
                            onClick = onUndoLastUse,
                            enabled = stage.canUndo
                        )
                        LESecondaryButton(
                            text = "Close",
                            onClick = onClose,
                            enabled = !stage.isApplying
                        )
                        Spacer(modifier = Modifier.width(LESpacing.xs))
                        LESecondaryButton(
                            text = "Skip Candidate",
                            onClick = onSkipCandidate,
                            enabled = !stage.isApplying
                        )
                        LESecondaryButton(
                            text = "Skip This Item",
                            onClick = onSkipItem,
                            enabled = !stage.isApplying
                        )
                        LEPrimaryButton(
                            text = if (stage.isApplying) "Saving..." else "Use Image & Next",
                            onClick = onApplyAndNext,
                            enabled = !stage.isApplying
                        )
                    }
                }
            } else {
                // Stacked layout: action buttons in row(s) on top, shortcut help on bottom so they never compete
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    if (availableWidth >= 760.dp) {
                        // 1 row for all 6 buttons: Secondary navigation on the left, Primary actions on the right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LESecondaryButton(
                                    text = "Previous Item",
                                    onClick = onPreviousItem,
                                    enabled = stage.canGoPrevious
                                )
                                LESecondaryButton(
                                    text = "Undo Last Use",
                                    onClick = onUndoLastUse,
                                    enabled = stage.canUndo
                                )
                                LESecondaryButton(
                                    text = "Close",
                                    onClick = onClose,
                                    enabled = !stage.isApplying
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LESecondaryButton(
                                    text = "Skip Candidate",
                                    onClick = onSkipCandidate,
                                    enabled = !stage.isApplying
                                )
                                LESecondaryButton(
                                    text = "Skip This Item",
                                    onClick = onSkipItem,
                                    enabled = !stage.isApplying
                                )
                                LEPrimaryButton(
                                    text = if (stage.isApplying) "Saving..." else "Use Image & Next",
                                    onClick = onApplyAndNext,
                                    enabled = !stage.isApplying
                                )
                            }
                        }
                    } else {
                        // Narrow width: 2 rows of buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LESecondaryButton(
                                text = "Previous Item",
                                onClick = onPreviousItem,
                                enabled = stage.canGoPrevious
                            )
                            Spacer(modifier = Modifier.width(LESpacing.xs))
                            LESecondaryButton(
                                text = "Undo Last Use",
                                onClick = onUndoLastUse,
                                enabled = stage.canUndo
                            )
                            Spacer(modifier = Modifier.width(LESpacing.xs))
                            LESecondaryButton(
                                text = "Close",
                                onClick = onClose,
                                enabled = !stage.isApplying
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LESecondaryButton(
                                text = "Skip Candidate",
                                onClick = onSkipCandidate,
                                enabled = !stage.isApplying
                            )
                            Spacer(modifier = Modifier.width(LESpacing.xs))
                            LESecondaryButton(
                                text = "Skip This Item",
                                onClick = onSkipItem,
                                enabled = !stage.isApplying
                            )
                            Spacer(modifier = Modifier.width(LESpacing.xs))
                            LEPrimaryButton(
                                text = if (stage.isApplying) "Saving..." else "Use Image & Next",
                                onClick = onApplyAndNext,
                                enabled = !stage.isApplying
                            )
                        }
                    }

                    Text(
                        text = "Enter: Use & Next  ·  Space/→: Skip Candidate  ·  N: Skip Item  ·  ←: Prev Item  ·  Ctrl+Z: Undo  ·  Esc: Close",
                        style = LETypography.caption,
                        color = LEColors.textMuted
                    )
                }
            }
        }
    }
}

private sealed interface ReviewImageState {
    data object Loading : ReviewImageState
    data class Success(val bitmap: ImageBitmap, val width: Int, val height: Int) : ReviewImageState
    data object Unavailable : ReviewImageState
}

@Composable
private fun ReviewImagePreview(
    packageName: String?,
    reference: String?,
    contentMediaStorage: ContentMediaStorage?,
    thumbnailLoader: LessonThumbnailLoader? = null,
    modifier: Modifier = Modifier
) {
    val state by produceState<ReviewImageState>(ReviewImageState.Loading, packageName, reference, contentMediaStorage) {
        value = if (reference.isNullOrBlank() || MediaReferencePolicy.isNoImageSentinel(reference)) {
            ReviewImageState.Unavailable
        } else {
            withContext(Dispatchers.IO) {
                val path = PackageMediaResolver.resolve(packageName, reference, contentMediaStorage)
                if (path != null && Files.exists(path) && Files.isRegularFile(path)) {
                    try {
                        val bytes = Files.readAllBytes(path)
                        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                        ReviewImageState.Success(
                            bitmap = skiaImage.toComposeImageBitmap(),
                            width = skiaImage.width,
                            height = skiaImage.height
                        )
                    } catch (_: Exception) {
                        ReviewImageState.Unavailable
                    }
                } else if (thumbnailLoader != null) {
                    when (val res = thumbnailLoader.load(reference, packageName)) {
                        is ThumbnailResult.Ready -> ReviewImageState.Success(
                            bitmap = res.bitmap,
                            width = res.bitmap.width,
                            height = res.bitmap.height
                        )
                        else -> ReviewImageState.Unavailable
                    }
                } else {
                    ReviewImageState.Unavailable
                }
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is ReviewImageState.Loading -> {
                CircularProgressIndicator(
                    color = LEColors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            is ReviewImageState.Success -> {
                Image(
                    bitmap = s.bitmap,
                    contentDescription = "Review image preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ReviewImageState.Unavailable -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = LEColors.textMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "No image",
                        style = LETypography.caption,
                        color = LEColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageReuseCompleteView(
    stage: ImageReuseReviewStage.Complete,
    targetPackageName: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF22C55E),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(LESpacing.md))
        Text("Image Reuse Review Complete", style = LETypography.paneTitle, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(LESpacing.xs))
        Text(
            text = "Reviewed ${stage.totalReviewedTargets} target items in $targetPackageName.",
            style = LETypography.fieldValue
        )
        Text(
            text = "Successfully reused and copied ${stage.totalAppliedCount} images.",
            style = LETypography.fieldValueEmphasized,
            color = LEColors.primary
        )
        Spacer(modifier = Modifier.height(LESpacing.lg))
        LEPrimaryButton(text = "Done / Close", onClick = onClose)
    }
}

@Composable
private fun ComparisonRowItem(
    label: String,
    targetValue: String,
    sourceValue: String
) {
    val isQuestion = label == "Question"
    val displayTarget = targetValue.ifBlank { "—" }
    val displaySource = sourceValue.ifBlank { "—" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Target Cell
        Surface(
            color = LEColors.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = if (isQuestion) 5.dp else 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: ",
                    style = LETypography.caption,
                    fontWeight = FontWeight.Bold,
                    color = LEColors.textMuted
                )
                Text(
                    text = displayTarget,
                    style = if (isQuestion) LETypography.fieldValueEmphasized else LETypography.fieldValue,
                    fontWeight = if (isQuestion) FontWeight.Bold else FontWeight.Normal,
                    color = if (displayTarget == "—") LEColors.textMuted else LEColors.textPrimary
                )
            }
        }

        // Source Cell
        Surface(
            color = LEColors.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.primary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = LESpacing.sm, vertical = if (isQuestion) 5.dp else 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: ",
                    style = LETypography.caption,
                    fontWeight = FontWeight.Bold,
                    color = LEColors.textMuted
                )
                Text(
                    text = displaySource,
                    style = if (isQuestion) LETypography.fieldValueEmphasized else LETypography.fieldValue,
                    fontWeight = if (isQuestion) FontWeight.Bold else FontWeight.Normal,
                    color = if (displaySource == "—") LEColors.textMuted else LEColors.textPrimary
                )
            }
        }
    }
}
