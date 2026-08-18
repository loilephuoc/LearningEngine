package vn.loi.learning.desktop.ui.browser.imagereuse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.application.contentmedia.MediaReferencePolicy
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.contentlibrary.ThumbnailResult
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*
import vn.loi.learning.desktop.ui.studio.DragDropUtils

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
    onPreviousItem: (ImageReuseTargetDraft?) -> Unit,
    onUndoLastUse: () -> Unit,
    onSkipCandidate: (ImageReuseTargetDraft?) -> Unit,
    onSkipItem: (ImageReuseTargetDraft?) -> Unit,
    onApplyAndNext: (ImageReuseTargetDraft?) -> Unit,
    onReplaceTargetImage: (File) -> Unit = {},
    onRemoveTargetImage: () -> Unit = {},
    onClose: (ImageReuseTargetDraft?) -> Unit
) {
    if (!state.visible || state.stage == null) return

    val focusRequester = remember { FocusRequester() }
    var isEditorFocused by remember { mutableStateOf(false) }
    var currentDraftProvider by remember { mutableStateOf<(() -> ImageReuseTargetDraft)?>(null) }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = {
            val stage = state.stage
            if (stage !is ImageReuseReviewStage.Review || !stage.isApplying) {
                onClose(currentDraftProvider?.invoke())
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
                        // Focus Guard: If user is editing a text field, let BasicTextField handle all keys (letters, Space, Backspace, IME, Enter)
                        if (isEditorFocused) {
                            return@onPreviewKeyEvent false
                        }

                        val stage = state.stage
                        when {
                            keyEvent.key == Key.Escape -> {
                                if (stage !is ImageReuseReviewStage.Review || !stage.isApplying) {
                                    onClose(currentDraftProvider?.invoke())
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
                                        onApplyAndNext(currentDraftProvider?.invoke())
                                        true
                                    }
                                    keyEvent.key == Key.Spacebar || keyEvent.key == Key.DirectionRight -> {
                                        onSkipCandidate(currentDraftProvider?.invoke())
                                        true
                                    }
                                    keyEvent.key == Key.DirectionLeft -> {
                                        if (stage.canGoPrevious) {
                                            onPreviousItem(currentDraftProvider?.invoke())
                                            true
                                        } else false
                                    }
                                    keyEvent.key == Key.N -> {
                                        onSkipItem(currentDraftProvider?.invoke())
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

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (val stage = state.stage) {
                            is ImageReuseReviewStage.Setup -> {
                                currentDraftProvider = null
                                ImageReuseSetupView(
                                    stage = stage,
                                    targetPackageName = state.targetPackageName,
                                    onToggleSourcePackage = onToggleSourcePackage,
                                    onSelectAllSourcePackages = onSelectAllSourcePackages,
                                    onClearAllSourcePackages = onClearAllSourcePackages,
                                    onScopeChanged = onScopeChanged,
                                    onStartScan = onStartScan,
                                    onClose = { onClose(null) }
                                )
                            }
                            is ImageReuseReviewStage.Review -> {
                                ImageReuseActiveReviewView(
                                    stage = stage,
                                    targetPackageName = state.targetPackageName,
                                    thumbnailLoader = thumbnailLoader,
                                    contentMediaStorage = contentMediaStorage,
                                    onEditorFocusChanged = { isEditorFocused = it },
                                    onRegisterDraftProvider = { provider -> currentDraftProvider = provider },
                                    onPreviousItem = onPreviousItem,
                                    onUndoLastUse = onUndoLastUse,
                                    onSkipCandidate = onSkipCandidate,
                                    onSkipItem = onSkipItem,
                                    onApplyAndNext = onApplyAndNext,
                                    onReplaceTargetImage = onReplaceTargetImage,
                                    onRemoveTargetImage = onRemoveTargetImage,
                                    onClose = onClose
                                )
                            }
                            is ImageReuseReviewStage.Complete -> {
                                currentDraftProvider = null
                                ImageReuseCompleteView(
                                    stage = stage,
                                    targetPackageName = state.targetPackageName,
                                    onClose = { onClose(null) }
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

        Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
            Text("Review Scope", style = LETypography.fieldLabel, color = LEColors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                ImageReuseScope.values().forEach { scope ->
                    val isSelected = stage.scope == scope
                    Surface(
                        color = if (isSelected) LEColors.primary.copy(alpha = 0.12f) else LEColors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) LEColors.primary else LEColors.borderSubtle
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onScopeChanged(scope) }
                    ) {
                        Row(
                            modifier = Modifier.padding(LESpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onScopeChanged(scope) },
                                colors = RadioButtonDefaults.colors(selectedColor = LEColors.primary)
                            )
                            Column {
                                Text(scope.label, style = LETypography.fieldValueEmphasized)
                                Text(scope.description, style = LETypography.caption, color = LEColors.textMuted)
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Source Packages to Scan (Read-Only)", style = LETypography.fieldLabel, color = LEColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    TextButton(onClick = onSelectAllSourcePackages) {
                        Text("Select All", style = LETypography.caption, color = LEColors.primary)
                    }
                    TextButton(onClick = onClearAllSourcePackages) {
                        Text("Clear All", style = LETypography.caption, color = LEColors.textMuted)
                    }
                }
            }

            if (stage.availableSourcePackages.isEmpty()) {
                Surface(
                    color = LEColors.surfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(LESpacing.lg), contentAlignment = Alignment.Center) {
                        Text(
                            "No other installed packages available for image sourcing.",
                            style = LETypography.secondaryMetadata,
                            color = LEColors.textMuted
                        )
                    }
                }
            } else {
                Surface(
                    color = LEColors.surfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(LESpacing.sm)) {
                        stage.availableSourcePackages.forEach { sourcePkg ->
                            val isSelected = sourcePkg.id in stage.selectedSourcePackageIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onToggleSourcePackage(sourcePkg.id) }
                                    .padding(horizontal = LESpacing.sm, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSourcePackage(sourcePkg.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = LEColors.primary)
                                )
                                Text(sourcePkg.name, style = LETypography.fieldValue, modifier = Modifier.weight(1f))
                                if (sourcePkg.version.isNotBlank()) {
                                    Text(
                                        text = "v${sourcePkg.version}",
                                        style = LETypography.caption,
                                        color = LEColors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        stage.scanError?.let { err ->
            Text(text = err, style = LETypography.secondaryMetadata, color = LEColors.danger)
        }

        Spacer(modifier = Modifier.weight(1f))

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

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ImageReuseActiveReviewView(
    stage: ImageReuseReviewStage.Review,
    targetPackageName: String,
    thumbnailLoader: LessonThumbnailLoader,
    contentMediaStorage: ContentMediaStorage?,
    onEditorFocusChanged: (Boolean) -> Unit,
    onRegisterDraftProvider: (() -> ImageReuseTargetDraft) -> Unit,
    onPreviousItem: (ImageReuseTargetDraft?) -> Unit,
    onUndoLastUse: () -> Unit,
    onSkipCandidate: (ImageReuseTargetDraft?) -> Unit,
    onSkipItem: (ImageReuseTargetDraft?) -> Unit,
    onApplyAndNext: (ImageReuseTargetDraft?) -> Unit,
    onReplaceTargetImage: (File) -> Unit,
    onRemoveTargetImage: () -> Unit,
    onClose: (ImageReuseTargetDraft?) -> Unit
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
            LEPrimaryButton(text = "Close", onClick = { onClose(null) })
        }
        return
    }

    // Stable local editor draft initialized ONCE per targetContentId
    var localDraft by remember(target.targetContentId) {
        mutableStateOf(stage.effectiveTargetDraft)
    }

    // Register local draft provider to dialog container
    LaunchedEffect(localDraft) {
        onRegisterDraftProvider { localDraft }
    }

    // Sync external draft image intent updates (e.g. from replaceTargetImage callback)
    LaunchedEffect(stage.targetDraft) {
        stage.targetDraft?.let { externalDraft ->
            if (externalDraft.targetContentId == target.targetContentId && externalDraft.imageIntent !is TargetImageIntent.Unchanged) {
                localDraft = localDraft.copy(imageIntent = externalDraft.imageIntent)
            }
        }
    }

    var isTargetDragOver by remember { mutableStateOf(false) }
    var focusedFieldsCount by remember { mutableStateOf(0) }

    LaunchedEffect(focusedFieldsCount) {
        onEditorFocusChanged(focusedFieldsCount > 0)
    }

    val isDirty = localDraft.isDirty(target)
    val primaryButtonText = when {
        stage.isApplying -> "Saving..."
        isDirty -> "Save & Next"
        else -> "Use Source Image & Next"
    }

    val effectiveTargetImageRef = when (val intent = localDraft.imageIntent) {
        is TargetImageIntent.Unchanged -> target.currentImageRef
        is TargetImageIntent.Replace -> intent.imageRef
        is TargetImageIntent.Remove -> null
        is TargetImageIntent.ReuseSource -> intent.imageRef
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
    ) {
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

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = LESpacing.sm, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TARGET (Editable)", style = LETypography.caption, color = LEColors.primary, fontWeight = FontWeight.Bold)
                            Text(targetPackageName, style = LETypography.secondaryMetadata, color = LEColors.textMuted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        if (isDirty) {
                            Surface(
                                color = LEColors.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Edited",
                                    style = LETypography.caption,
                                    color = LEColors.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
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
                        Text("SOURCE CANDIDATE (Read-Only)", style = LETypography.caption, color = LEColors.primary, fontWeight = FontWeight.Bold)
                        Text(candidate.sourcePackageName, style = LETypography.secondaryMetadata, color = LEColors.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EditableComparisonRow(
                    label = "Question",
                    targetValue = localDraft.question,
                    onTargetValueChange = { localDraft = localDraft.copy(question = it) },
                    onFocusChanged = { focused ->
                        focusedFieldsCount = if (focused) focusedFieldsCount + 1 else maxOf(0, focusedFieldsCount - 1)
                    },
                    sourceValue = candidate.question
                )
                EditableComparisonRow(
                    label = "Answer",
                    targetValue = localDraft.answer,
                    onTargetValueChange = { localDraft = localDraft.copy(answer = it) },
                    onFocusChanged = { focused ->
                        focusedFieldsCount = if (focused) focusedFieldsCount + 1 else maxOf(0, focusedFieldsCount - 1)
                    },
                    sourceValue = candidate.answer
                )
                EditableComparisonRow(
                    label = "Translation",
                    targetValue = localDraft.translation,
                    onTargetValueChange = { localDraft = localDraft.copy(translation = it) },
                    onFocusChanged = { focused ->
                        focusedFieldsCount = if (focused) focusedFieldsCount + 1 else maxOf(0, focusedFieldsCount - 1)
                    },
                    sourceValue = candidate.translation
                )
                EditableComparisonRow(
                    label = "Example",
                    targetValue = localDraft.exampleText,
                    onTargetValueChange = { localDraft = localDraft.copy(exampleText = it) },
                    onFocusChanged = { focused ->
                        focusedFieldsCount = if (focused) focusedFieldsCount + 1 else maxOf(0, focusedFieldsCount - 1)
                    },
                    sourceValue = candidate.exampleText
                )
                EditableComparisonRow(
                    label = "POS",
                    targetValue = localDraft.partOfSpeech,
                    onTargetValueChange = { localDraft = localDraft.copy(partOfSpeech = it) },
                    onFocusChanged = { focused ->
                        focusedFieldsCount = if (focused) focusedFieldsCount + 1 else maxOf(0, focusedFieldsCount - 1)
                    },
                    sourceValue = candidate.partOfSpeech
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                Surface(
                    color = LEColors.surfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isTargetDragOver) 2.dp else 1.dp,
                        color = if (isTargetDragOver) LEColors.primary else LEColors.borderSubtle
                    ),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(LESpacing.xs)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isTargetDragOver) LEColors.primary.copy(alpha = 0.08f) else LEColors.surface)
                                .padding(LESpacing.xs)
                                .dragAndDropTarget(
                                    shouldStartDragAndDrop = { true },
                                    target = remember {
                                        object : DragAndDropTarget {
                                            override fun onStarted(event: DragAndDropEvent) { isTargetDragOver = true }
                                            override fun onEntered(event: DragAndDropEvent) { isTargetDragOver = true }
                                            override fun onExited(event: DragAndDropEvent) { isTargetDragOver = false }
                                            override fun onEnded(event: DragAndDropEvent) { isTargetDragOver = false }
                                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                                isTargetDragOver = false
                                                val transferable = event.awtTransferable
                                                val files = DragDropUtils.extractFiles(transferable)
                                                val imageFile = files.firstOrNull { DragDropUtils.isSupportedImage(it) }
                                                if (imageFile != null) {
                                                    onReplaceTargetImage(imageFile)
                                                    return true
                                                }
                                                return false
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (localDraft.imageIntent is TargetImageIntent.Remove) {
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
                                        text = "No image (Removed)",
                                        style = LETypography.caption,
                                        color = LEColors.danger
                                    )
                                }
                            } else {
                                ReviewImagePreview(
                                    packageName = targetPackageName,
                                    reference = effectiveTargetImageRef,
                                    contentMediaStorage = contentMediaStorage,
                                    thumbnailLoader = thumbnailLoader,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            when (val intent = localDraft.imageIntent) {
                                is TargetImageIntent.Replace -> {
                                    Surface(
                                        color = LEColors.primary,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                    ) {
                                        Text(
                                            text = "Replaced",
                                            style = LETypography.caption,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                is TargetImageIntent.Remove -> {
                                    Surface(
                                        color = LEColors.danger,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                    ) {
                                        Text(
                                            text = "Removed",
                                            style = LETypography.caption,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                is TargetImageIntent.ReuseSource -> {
                                    Surface(
                                        color = LEColors.primary,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                    ) {
                                        Text(
                                            text = "Using Source",
                                            style = LETypography.caption,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                TargetImageIntent.Unchanged -> {}
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                        ) {
                            LESecondaryButton(
                                text = "Replace",
                                onClick = {
                                    pickFile("Select Image", DragDropUtils.IMAGE_EXTENSIONS.toList()) { file ->
                                        onReplaceTargetImage(file)
                                    }
                                },
                                icon = LEIcons.Replace,
                                modifier = Modifier.weight(1f)
                            )
                            LESecondaryButton(
                                text = "Remove",
                                onClick = {
                                    localDraft = localDraft.copy(imageIntent = TargetImageIntent.Remove)
                                    onRemoveTargetImage()
                                },
                                icon = LEIcons.Remove,
                                modifier = Modifier.weight(1f)
                            )
                        }
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

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            if (availableWidth >= 1150.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDirty)
                            "Enter: Save & Next  ·  Space/→: Skip Candidate  ·  N: Skip Item  ·  ←: Prev Item  ·  Ctrl+Z: Undo  ·  Esc: Close"
                        else
                            "Enter: Use & Next  ·  Space/→: Skip Candidate  ·  N: Skip Item  ·  ←: Prev Item  ·  Ctrl+Z: Undo  ·  Esc: Close",
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
                            onClick = { onPreviousItem(localDraft) },
                            enabled = stage.canGoPrevious
                        )
                        LESecondaryButton(
                            text = "Undo Last Use",
                            onClick = onUndoLastUse,
                            enabled = stage.canUndo
                        )
                        LESecondaryButton(
                            text = "Close",
                            onClick = { onClose(localDraft) },
                            enabled = !stage.isApplying
                        )
                        Spacer(modifier = Modifier.width(LESpacing.xs))
                        LESecondaryButton(
                            text = "Skip Candidate",
                            onClick = { onSkipCandidate(localDraft) },
                            enabled = !stage.isApplying
                        )
                        LESecondaryButton(
                            text = "Skip This Item",
                            onClick = { onSkipItem(localDraft) },
                            enabled = !stage.isApplying
                        )
                        LEPrimaryButton(
                            text = primaryButtonText,
                            onClick = { onApplyAndNext(localDraft) },
                            enabled = !stage.isApplying
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    if (availableWidth >= 760.dp) {
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
                                    onClick = { onPreviousItem(localDraft) },
                                    enabled = stage.canGoPrevious
                                )
                                LESecondaryButton(
                                    text = "Undo Last Use",
                                    onClick = onUndoLastUse,
                                    enabled = stage.canUndo
                                )
                                LESecondaryButton(
                                    text = "Close",
                                    onClick = { onClose(localDraft) },
                                    enabled = !stage.isApplying
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LESecondaryButton(
                                    text = "Skip Candidate",
                                    onClick = { onSkipCandidate(localDraft) },
                                    enabled = !stage.isApplying
                                )
                                LESecondaryButton(
                                    text = "Skip This Item",
                                    onClick = { onSkipItem(localDraft) },
                                    enabled = !stage.isApplying
                                )
                                LEPrimaryButton(
                                    text = primaryButtonText,
                                    onClick = { onApplyAndNext(localDraft) },
                                    enabled = !stage.isApplying
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LESecondaryButton(
                                text = "Previous Item",
                                onClick = { onPreviousItem(localDraft) },
                                enabled = stage.canGoPrevious
                            )
                            LESecondaryButton(
                                text = "Undo Last Use",
                                onClick = onUndoLastUse,
                                enabled = stage.canUndo
                            )
                            LESecondaryButton(
                                text = "Close",
                                onClick = { onClose(localDraft) },
                                enabled = !stage.isApplying
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LESecondaryButton(
                                text = "Skip Candidate",
                                onClick = { onSkipCandidate(localDraft) },
                                enabled = !stage.isApplying
                            )
                            LESecondaryButton(
                                text = "Skip This Item",
                                onClick = { onSkipItem(localDraft) },
                                enabled = !stage.isApplying
                            )
                            LEPrimaryButton(
                                text = primaryButtonText,
                                onClick = { onApplyAndNext(localDraft) },
                                enabled = !stage.isApplying
                            )
                        }
                    }

                    Text(
                        text = if (isDirty)
                            "Enter: Save & Next  ·  Space/→: Skip Candidate  ·  N: Skip Item  ·  ←: Prev Item  ·  Ctrl+Z: Undo  ·  Esc: Close"
                        else
                            "Enter: Use & Next  ·  Space/→: Skip Candidate  ·  N: Skip Item  ·  ←: Prev Item  ·  Ctrl+Z: Undo  ·  Esc: Close",
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
private fun EditableComparisonRow(
    label: String,
    targetValue: String,
    onTargetValueChange: (String) -> Unit,
    sourceValue: String,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val isQuestion = label == "Question"
    val displaySource = sourceValue.ifBlank { "—" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                BasicTextField(
                    value = targetValue,
                    onValueChange = onTargetValueChange,
                    singleLine = true,
                    maxLines = 1,
                    textStyle = if (isQuestion) LETypography.fieldValueEmphasized.copy(color = LEColors.primaryText, fontWeight = FontWeight.Bold)
                    else LETypography.fieldValue.copy(color = LEColors.primaryText),
                    cursorBrush = SolidColor(LEColors.primary),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                )
            }
        }

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
                    color = LEColors.textPrimary
                )
            }
        }
    }
}

private fun pickFile(title: String, allowedExtensions: List<String>, onFileSelected: (File) -> Unit) {
    try {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isVisible = true
        val dir = dialog.directory
        val fileName = dialog.file
        if (dir != null && fileName != null) {
            val selected = File(dir, fileName)
            if (selected.exists() && selected.isFile) {
                val ext = selected.extension.lowercase()
                if (allowedExtensions.isEmpty() || ext in allowedExtensions) {
                    onFileSelected(selected)
                }
            }
        }
    } catch (_: Exception) {}
}
