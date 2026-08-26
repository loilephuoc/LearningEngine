package vn.loi.learning.desktop.ui.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.io.File
import kotlinx.coroutines.launch
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

/** Supported image extensions for drop validation. */
private val IMAGE_EXTS = setOf("png", "jpg", "jpeg", "webp")

/** Supported audio extensions for drop validation. */
private val AUDIO_EXTS = setOf("mp3", "wav", "aiff")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.fileDropTarget(
    dropIntentKey: String,
    allowedExtensions: Set<String>,
    rejectedSlotMessage: String,
    onFileDropped: (File) -> Unit,
    onDragOverChanged: (Boolean) -> Unit,
    onError: (String) -> Unit
): Modifier {
    val currentDropIntentKey by rememberUpdatedState(dropIntentKey)
    val currentOnFileDropped by rememberUpdatedState(onFileDropped)
    val currentOnDragOverChanged by rememberUpdatedState(onDragOverChanged)
    val currentOnError by rememberUpdatedState(onError)
    val coroutineScope = rememberCoroutineScope()
    val imageExtractor = remember { BrowserImageDropExtractor() }
    val target = remember(allowedExtensions) {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) { currentOnDragOverChanged(true) }
            override fun onEntered(event: DragAndDropEvent) { currentOnDragOverChanged(true) }
            override fun onExited(event: DragAndDropEvent) { currentOnDragOverChanged(false) }
            override fun onEnded(event: DragAndDropEvent) { currentOnDragOverChanged(false) }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnDragOverChanged(false)
                if (allowedExtensions == IMAGE_EXTS) {
                    val intendedKey = currentDropIntentKey
                    val snapshot = try {
                        imageExtractor.snapshot(event.awtTransferable)
                    } catch (failure: BrowserImageDropException) {
                        currentOnError(failure.message ?: "Could not read image from browser drag.")
                        return false
                    }
                    coroutineScope.launch {
                        try {
                            imageExtractor.extract(snapshot).use { extracted ->
                                if (currentDropIntentKey != intendedKey) {
                                    currentOnError("Image import was cancelled because the selected item changed.")
                                    return@use
                                }
                                currentOnFileDropped(extracted.file)
                            }
                        } catch (failure: BrowserImageDropException) {
                            currentOnError(failure.message ?: "Could not read image from browser drag.")
                        }
                    }
                    return true
                }
                return try {
                    val transferable = event.awtTransferable
                    val files = DragDropUtils.extractFiles(transferable)
                    val file = files.firstOrNull()
                    if (file != null && file.exists() && file.isFile) {
                        val ext = file.extension.lowercase()
                        if (ext in allowedExtensions) {
                            currentOnFileDropped(file)
                            true
                        } else {
                            currentOnError(if (ext in (DragDropUtils.IMAGE_EXTENSIONS + DragDropUtils.AUDIO_EXTENSIONS)) rejectedSlotMessage else "Unsupported file type: .$ext")
                            false
                        }
                    } else false
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { true },
        target = target
    )
}

@Composable
fun MediaInspectorPane(
    uiState: PackageContentBrowserUiState,
    playbackCoordinator: PlaybackCoordinator? = null,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    onUpdateDraftImageRef: ((String?) -> Unit)? = null,
    onUpdateDraftQuestionAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftAnswerAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftExampleAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftTranslationAudioRef: ((String?) -> Unit)? = null,
    onImportMediaFile: ((File, String) -> Unit)? = null,
    onOpenFullscreenImage: (() -> Unit)? = null,
    onShowImageInFolder: ((String) -> Unit)? = null,
    resolveImageFileName: ((String) -> String?)? = null,
    onRequestGenerateTts: ((vn.loi.learning.desktop.tts.TtsField) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCreating = uiState.isCreatingNewItem
    val activeDraft = if (isCreating || uiState.editingContentId != null) uiState.draftEdits else null
    val persistedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere

    val currentImageRef = if (activeDraft != null) activeDraft.imageRef else persistedItem?.imageRef
    val currentQuestionAudioRef = if (activeDraft != null) activeDraft.questionAudioRef else persistedItem?.questionAudioRef
    val currentAnswerAudioRef = if (activeDraft != null) activeDraft.answerAudioRef else persistedItem?.answerAudioRef
    val currentExampleAudioRef = if (activeDraft != null) activeDraft.exampleAudioRef else persistedItem?.exampleAudioRef
    val currentTranslationAudioRef = if (activeDraft != null) activeDraft.translationAudioRef else persistedItem?.translationAudioRef

    val hasImage = !currentImageRef.isNullOrBlank()

    val scrollState = rememberScrollState()
    var isAiSuggestionsExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = LEColors.surface,
        tonalElevation = LEElevation.flat
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LESpacing.md)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(LESpacing.lg)
        ) {
            // SECTION 1: MEDIA MANAGER
            Text(
                text = "MEDIA MANAGER",
                style = LETypography.paneTitle,
                color = LEColors.textSecondary
            )

            // Image Asset Card (PLE-020: larger preview + real drag & drop)
            ImageAssetCard(
                dropIntentKey = "${uiState.installedPackageId.value}|${uiState.editingContentId}|${uiState.isCreatingNewItem}",
                imageRef = currentImageRef,
                thumbnailLoader = thumbnailLoader,
                onImportMediaFile = onImportMediaFile,
                onUpdateDraftImageRef = onUpdateDraftImageRef,
                onOpenFullscreenImage = onOpenFullscreenImage,
                onShowImageInFolder = onShowImageInFolder,
                resolveImageFileName = resolveImageFileName
            )

            // Audio Asset Cards (PLE-020: M3 button roles + real drag & drop)
            AudioAssetSlotCard(
                label = "Question Audio",
                slotName = "question",
                ttsField = vn.loi.learning.desktop.tts.TtsField.QUESTION,
                audioRef = currentQuestionAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftQuestionAudioRef,
                onImportMediaFile = onImportMediaFile,
                onRequestGenerateTts = onRequestGenerateTts
            )

            AudioAssetSlotCard(
                label = "Answer Audio",
                slotName = "answer",
                ttsField = vn.loi.learning.desktop.tts.TtsField.ANSWER,
                audioRef = currentAnswerAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftAnswerAudioRef,
                onImportMediaFile = onImportMediaFile,
                onRequestGenerateTts = onRequestGenerateTts
            )

            AudioAssetSlotCard(
                label = "Example Audio",
                slotName = "example",
                ttsField = vn.loi.learning.desktop.tts.TtsField.EXAMPLE,
                audioRef = currentExampleAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftExampleAudioRef,
                onImportMediaFile = onImportMediaFile,
                onRequestGenerateTts = onRequestGenerateTts
            )

            AudioAssetSlotCard(
                label = "Translation Audio",
                slotName = "translation",
                ttsField = vn.loi.learning.desktop.tts.TtsField.TRANSLATION,
                audioRef = currentTranslationAudioRef,
                playbackCoordinator = playbackCoordinator,
                onPlayAudio = onPlayAudio,
                onStopAudio = onStopAudio,
                onUpdateDraftRef = onUpdateDraftTranslationAudioRef,
                onImportMediaFile = onImportMediaFile,
                onRequestGenerateTts = onRequestGenerateTts
            )

            HorizontalDivider(color = LEColors.borderSubtle)

            // SECTION 2: QUALITY & AI REVIEW
            Text(
                text = "QUALITY & AI REVIEW",
                style = LETypography.paneTitle,
                color = LEColors.textSecondary
            )

            LEInspectorCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    QualityItemRow(label = "Image", variant = if (hasImage) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Question Audio", variant = if (!currentQuestionAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Answer Audio", variant = if (!currentAnswerAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Example Audio", variant = if (!currentExampleAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "Translation Audio", variant = if (!currentTranslationAudioRef.isNullOrBlank()) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "IPA Format", variant = if (persistedItem?.pronunciation?.isNotBlank() == true) StatusBadgeVariant.Valid else StatusBadgeVariant.Missing)
                    QualityItemRow(label = "POS", variant = StatusBadgeVariant.Valid, customText = "Valid")
                    QualityItemRow(label = "Example Length", variant = StatusBadgeVariant.Valid, customText = "Good")
                    QualityItemRow(label = "Translation Length", variant = StatusBadgeVariant.Valid, customText = "Good")
                    HorizontalDivider(color = LEColors.borderSubtle, modifier = Modifier.padding(vertical = LESpacing.xs))
                    QualityItemRow(label = "Duplicate Check", variant = StatusBadgeVariant.NotEvaluated)
                    QualityItemRow(label = "Orphan Media", variant = StatusBadgeVariant.NotEvaluated)
                    QualityItemRow(label = "Unused Media", variant = StatusBadgeVariant.NotEvaluated)
                }
            }

            // AI SUGGESTIONS ACCORDION
            LECard(
                backgroundColor = LEColors.primarySoft.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAiSuggestionsExpanded = !isAiSuggestionsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "^ AI Suggestions (3)",
                            style = LETypography.sectionTitle,
                            color = LEColors.primaryText
                        )
                    }

                    AnimatedVisibility(visible = isAiSuggestionsExpanded) {
                        Column(
                            modifier = Modifier.padding(top = LESpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                        ) {
                            AiSuggestionCard(text = "Example could be more natural.")
                            AiSuggestionCard(text = "Translation could be improved.")
                            AiSuggestionCard(text = "Missing article \"an\" before \"account\".")

                            Spacer(modifier = Modifier.height(LESpacing.xs))

                            LEPrimaryButton(
                                text = "Apply All Suggestions",
                                onClick = {},
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------
// PLE-020: Image Asset Card with real drag & drop + larger preview
// -----------------------------------------------------------------------
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ImageAssetCard(
    dropIntentKey: String,
    imageRef: String?,
    thumbnailLoader: LessonThumbnailLoader,
    onImportMediaFile: ((File, String) -> Unit)?,
    onUpdateDraftImageRef: ((String?) -> Unit)?,
    onOpenFullscreenImage: (() -> Unit)?,
    onShowImageInFolder: ((String) -> Unit)?,
    resolveImageFileName: ((String) -> String?)?
) {
    val hasImage = !imageRef.isNullOrBlank()
    var isDragOver by remember { mutableStateOf(false) }
    var dropError by remember { mutableStateOf<String?>(null) }
    var dropSuccess by remember { mutableStateOf(false) }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isDragOver) 1f else 0.4f,
        animationSpec = tween(150),
        label = "imageBorderAlpha"
    )

    LaunchedEffect(dropSuccess) {
        if (dropSuccess) {
            kotlinx.coroutines.delay(1200)
            dropSuccess = false
        }
    }
    LaunchedEffect(dropError) {
        if (dropError != null) {
            kotlinx.coroutines.delay(2500)
            dropError = null
        }
    }

    val borderColor = when {
        dropError != null -> LEColors.danger
        dropSuccess -> Color(0xFF22C55E)
        isDragOver -> LEColors.primary
        else -> LEColors.borderSubtle
    }

    LEInspectorCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = borderAlpha), LERadius.sm)
            .fileDropTarget(
                dropIntentKey = dropIntentKey,
                allowedExtensions = IMAGE_EXTS,
                rejectedSlotMessage = "Audio file dropped on image slot. Use an audio slot instead.",
                onFileDropped = { file ->
                    if (onImportMediaFile != null) onImportMediaFile(file, "image")
                    else onUpdateDraftImageRef?.invoke(file.name)
                    dropSuccess = true
                },
                onDragOverChanged = { isDragOver = it },
                onError = { dropError = it }
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Image", style = LETypography.sectionTitle)
            LEStatusBadge(variant = if (hasImage) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
        }

        Spacer(modifier = Modifier.height(LESpacing.sm))

        if (hasImage) {
            // PLE-020: Larger 120dp preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(LERadius.sm)
                        .background(LEColors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    LessonThumbnail(
                        reference = imageRef ?: "",
                        loader = thumbnailLoader
                    )
                }

                // Prefer the physical file name resolved from media storage.
                // This keeps the UI correct when a legacy reference still says .png
                // but the actual media file has been converted to .jpg.
                val imageFileNameWithExtension =
                    imageRef
                        ?.let { reference -> resolveImageFileName?.invoke(reference) }
                        ?.takeIf { it.isNotBlank() }
                        ?: (imageRef ?: "image.jpg")
                            .substringAfterLast('/')
                            .substringAfterLast('\\')
                val imageFileName =
                    imageFileNameWithExtension.substringBeforeLast(
                        delimiter = '.',
                        missingDelimiterValue = imageFileNameWithExtension
                    )
                val imageExtension =
                    imageFileNameWithExtension.substringAfterLast('.', missingDelimiterValue = "").lowercase()
                val isNonJpgImage = imageExtension != "jpg"
                var copiedImageFileName by remember(imageFileName) { mutableStateOf(false) }

                LaunchedEffect(copiedImageFileName) {
                    if (copiedImageFileName) {
                        kotlinx.coroutines.delay(1200)
                        copiedImageFileName = false
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = imageFileNameWithExtension,
                                style = LETypography.caption,
                                color = if (isNonJpgImage) Color(0xFFC2410C) else LEColors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isNonJpgImage) {
                                Surface(
                                    color = Color(0xFFFFEDD5),
                                    shape = LERadius.xs
                                ) {
                                    Text(
                                        text = if (imageExtension.isBlank()) "NON-JPG" else imageExtension.uppercase(),
                                        style = LETypography.caption,
                                        color = Color(0xFFC2410C),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                Toolkit.getDefaultToolkit()
                                    .systemClipboard
                                    .setContents(StringSelection(imageFileName), null)
                                copiedImageFileName = true
                            },
                            contentPadding = PaddingValues(horizontal = LESpacing.sm, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (copiedImageFileName) "Copied" else "Copy",
                                style = LETypography.caption,
                                color = if (copiedImageFileName) Color(0xFF16A34A) else LEColors.primary
                            )
                        }
                    }
                    Text("Status: Attached", style = LETypography.caption, color = LEColors.textMuted)
                    Text("Asset: Resolved", style = LETypography.caption, color = LEColors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(LESpacing.sm))

            // PLE-020: M3 roles — Replace=Primary, Open=Outlined, Remove=Danger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                LEPrimaryButton(
                    text = "Replace",
                    onClick = {
                        pickFile("Select Image", IMAGE_EXTS.toList()) { file ->
                            if (onImportMediaFile != null) onImportMediaFile(file, "image")
                            else onUpdateDraftImageRef?.invoke(file.name)
                        }
                    },
                    icon = LEIcons.Replace,
                    modifier = Modifier.weight(1f)
                )
                LESecondaryButton(
                    text = "Open",
                    onClick = { onOpenFullscreenImage?.invoke() },
                    icon = LEIcons.Open,
                    modifier = Modifier.weight(1f)
                )
                LEDangerButton(
                    text = "Remove",
                    onClick = { onUpdateDraftImageRef?.invoke(null) },
                    icon = LEIcons.Remove,
                    modifier = Modifier.weight(1f)
                )
            }

            onShowImageInFolder?.let { showInFolder ->
                Spacer(modifier = Modifier.height(LESpacing.xs))
                LESecondaryButton(
                    text = "Show in Folder",
                    onClick = {
                        imageRef
                            ?.takeIf { it.isNotBlank() }
                            ?.let { reference -> showInFolder(reference) }
                    },
                    icon = LEIcons.Open,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // PLE-020: Empty state with real drag & drop zone
            Column(verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                LEPrimaryButton(
                    text = "Browse Image File",
                    onClick = {
                        pickFile("Select Image", IMAGE_EXTS.toList()) { file ->
                            if (onImportMediaFile != null) onImportMediaFile(file, "image")
                            else onUpdateDraftImageRef?.invoke(file.name)
                        }
                    },
                    icon = LEIcons.New,
                    modifier = Modifier.fillMaxWidth()
                )

                // PLE-020: Real drag & drop target
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(LERadius.sm)
                        .background(
                            when {
                                dropError != null -> LEColors.danger.copy(alpha = 0.08f)
                                dropSuccess -> Color(0xFF22C55E).copy(alpha = 0.08f)
                                isDragOver -> LEColors.primary.copy(alpha = 0.08f)
                                else -> LEColors.surfaceElevated
                            }
                        )
                        .border(1.dp, borderColor.copy(alpha = borderAlpha), LERadius.sm)
                        .fileDropTarget(
                            dropIntentKey = dropIntentKey,
                            allowedExtensions = IMAGE_EXTS,
                            rejectedSlotMessage = "Audio file dropped on image slot. Use an audio slot instead.",
                            onFileDropped = { file ->
                                if (onImportMediaFile != null) onImportMediaFile(file, "image")
                                else onUpdateDraftImageRef?.invoke(file.name)
                                dropSuccess = true
                            },
                            onDragOverChanged = { isDragOver = it },
                            onError = { dropError = it }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = LEIcons.Image,
                            contentDescription = null,
                            tint = when {
                                dropError != null -> LEColors.danger
                                isDragOver -> LEColors.primary
                                else -> LEColors.textMuted
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(LESpacing.xs))
                        Text(
                            text = when {
                                dropError != null -> dropError!!
                                dropSuccess -> "✓ Image imported"
                                isDragOver -> "Drop to import image"
                                else -> "Drop image here"
                            },
                            style = LETypography.caption,
                            color = when {
                                dropError != null -> LEColors.danger
                                dropSuccess -> Color(0xFF22C55E)
                                isDragOver -> LEColors.primary
                                else -> LEColors.textSecondary
                            },
                            fontWeight = if (isDragOver || dropSuccess) FontWeight.Medium else FontWeight.Normal
                        )
                        if (dropError == null && !dropSuccess) {
                            Text(
                                text = "JPG, PNG, WEBP",
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

// -----------------------------------------------------------------------
// PLE-020: Audio Asset Slot with real drag & drop + M3 button roles
// -----------------------------------------------------------------------
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AudioAssetSlotCard(
    label: String,
    slotName: String,
    ttsField: vn.loi.learning.desktop.tts.TtsField? = null,
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?,
    onUpdateDraftRef: ((String?) -> Unit)?,
    onImportMediaFile: ((File, String) -> Unit)?,
    onRequestGenerateTts: ((vn.loi.learning.desktop.tts.TtsField) -> Unit)? = null
) {
    val isPresent = !audioRef.isNullOrBlank()
    val isPlaying = if (isPresent && audioRef != null && playbackCoordinator != null) {
        playbackCoordinator.getButtonState(audioRef) is AudioButtonState.Playing
    } else false

    var isDragOver by remember { mutableStateOf(false) }
    var dropError by remember { mutableStateOf<String?>(null) }
    var dropSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(dropSuccess) {
        if (dropSuccess) {
            kotlinx.coroutines.delay(1200)
            dropSuccess = false
        }
    }
    LaunchedEffect(dropError) {
        if (dropError != null) {
            kotlinx.coroutines.delay(2500)
            dropError = null
        }
    }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isDragOver) 1f else 0.4f,
        animationSpec = tween(150),
        label = "audioBorderAlpha_$slotName"
    )
    val borderColor = when {
        dropError != null -> LEColors.danger
        dropSuccess -> Color(0xFF22C55E)
        isDragOver -> LEColors.primary
        else -> LEColors.borderSubtle
    }

    LEInspectorCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = borderAlpha), LERadius.sm)
            .fileDropTarget(
                dropIntentKey = slotName,
                allowedExtensions = AUDIO_EXTS,
                rejectedSlotMessage = "Image dropped on audio slot. Use the Image card instead.",
                onFileDropped = { file ->
                    if (onImportMediaFile != null) onImportMediaFile(file, slotName)
                    else onUpdateDraftRef?.invoke(file.name)
                    dropSuccess = true
                },
                onDragOverChanged = { isDragOver = it },
                onError = { dropError = it }
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = LETypography.sectionTitle)
            LEStatusBadge(variant = if (isPresent) StatusBadgeVariant.Present else StatusBadgeVariant.Missing)
        }

        Spacer(modifier = Modifier.height(LESpacing.sm))

        if (isPresent && audioRef != null) {
            Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = audioRef.substringAfterLast('/').substringAfterLast('\\'),
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Ready",
                        style = LETypography.caption,
                        color = LEColors.textMuted,
                        modifier = Modifier.padding(start = LESpacing.xs)
                    )
                }
                Text(
                    text = "Duration: N/A",
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(LESpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                LEPrimaryButton(
                    text = "Replace",
                    onClick = {
                        pickFile("Select Audio File", AUDIO_EXTS.toList()) { file ->
                            if (onImportMediaFile != null) onImportMediaFile(file, slotName)
                            else onUpdateDraftRef?.invoke(file.name)
                        }
                    },
                    icon = LEIcons.Replace,
                    modifier = Modifier.weight(1f)
                )
                LESecondaryButton(
                    text = if (isPlaying) "Stop" else "Preview",
                    onClick = {
                        if (isPlaying) {
                            playbackCoordinator?.stop() ?: onStopAudio?.invoke()
                        } else {
                            playbackCoordinator?.play(audioRef) ?: onPlayAudio?.invoke(audioRef)
                        }
                    },
                    icon = if (isPlaying) LEIcons.Stop else LEIcons.Play,
                    modifier = Modifier.weight(1f)
                )
                LEDangerButton(
                    text = "Remove",
                    onClick = { onUpdateDraftRef?.invoke(null) },
                    icon = LEIcons.Remove,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    LEPrimaryButton(
                        text = "Browse Audio",
                        onClick = {
                            pickFile("Select Audio File", AUDIO_EXTS.toList()) { file ->
                                if (onImportMediaFile != null) onImportMediaFile(file, slotName)
                                else onUpdateDraftRef?.invoke(file.name)
                            }
                        },
                        icon = LEIcons.New,
                        modifier = Modifier.weight(1f)
                    )
                    if (ttsField != null && onRequestGenerateTts != null) {
                        LESecondaryButton(
                            text = "TTS",
                            onClick = { onRequestGenerateTts(ttsField) },
                            icon = LEIcons.Audio,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // PLE-020: Real audio drag & drop zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(LERadius.sm)
                        .background(
                            when {
                                dropError != null -> LEColors.danger.copy(alpha = 0.08f)
                                dropSuccess -> Color(0xFF22C55E).copy(alpha = 0.08f)
                                isDragOver -> LEColors.primary.copy(alpha = 0.08f)
                                else -> LEColors.surfaceElevated
                            }
                        )
                        .border(1.dp, borderColor.copy(alpha = borderAlpha), LERadius.sm)
                        .fileDropTarget(
                            dropIntentKey = slotName,
                            allowedExtensions = AUDIO_EXTS,
                            rejectedSlotMessage = "Image dropped on audio slot. Use the Image card instead.",
                            onFileDropped = { file ->
                                if (onImportMediaFile != null) onImportMediaFile(file, slotName)
                                else onUpdateDraftRef?.invoke(file.name)
                                dropSuccess = true
                            },
                            onDragOverChanged = { isDragOver = it },
                            onError = { dropError = it }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                dropError != null -> dropError!!
                                dropSuccess -> "✓ Audio imported"
                                isDragOver -> "Drop to import audio"
                                else -> "Drop $label here"
                            },
                            style = LETypography.caption,
                            color = when {
                                dropError != null -> LEColors.danger
                                dropSuccess -> Color(0xFF22C55E)
                                isDragOver -> LEColors.primary
                                else -> LEColors.textSecondary
                            },
                            fontWeight = if (isDragOver || dropSuccess) FontWeight.Medium else FontWeight.Normal
                        )
                        if (dropError == null && !dropSuccess) {
                            Text(
                                text = "MP3, WAV, AIFF",
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

@Composable
private fun QualityItemRow(
    label: String,
    variant: StatusBadgeVariant,
    customText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = LETypography.secondaryMetadata, color = LEColors.textPrimary)
        LEStatusBadge(variant = variant, customText = customText)
    }
}

@Composable
private fun AiSuggestionCard(text: String) {
    Surface(
        color = LEColors.warningContainer.copy(alpha = 0.5f),
        shape = LERadius.xs,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = LETypography.caption,
            color = LEColors.textPrimary,
            modifier = Modifier.padding(LESpacing.xs)
        )
    }
}
