package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.contentlibrary.ThumbnailResult
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

@Composable
fun ContentEditorPane(
    uiState: PackageContentBrowserUiState,
    isCreatingNewItem: Boolean = false,
    playbackCoordinator: PlaybackCoordinator? = null,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    contentMediaStorage: ContentMediaStorage? = null,
    onEditContent: (() -> Unit)? = null,
    onSaveEdit: (() -> Unit)? = null,
    onDiscardEdit: (() -> Unit)? = null,
    onUpdateDraftQuestion: ((String) -> Unit)? = null,
    onUpdateDraftAnswer: ((String) -> Unit)? = null,
    onUpdateDraftPronunciation: ((String) -> Unit)? = null,
    onUpdateDraftPartOfSpeech: ((String) -> Unit)? = null,
    onUpdateDraftExampleText: ((String) -> Unit)? = null,
    onUpdateDraftExampleTranslation: ((String) -> Unit)? = null,
    onRequestDelete: (() -> Unit)? = null,
    onConfirmDelete: (() -> Unit)? = null,
    onDismissDelete: (() -> Unit)? = null,
    onConfirmSaveAndProceed: (() -> Unit)? = null,
    onConfirmDiscardAndProceed: (() -> Unit)? = null,
    onCancelUnsavedDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCreating = uiState.isCreatingNewItem || isCreatingNewItem
    val activeDraft = if (isCreating || uiState.editingContentId != null) uiState.draftEdits else null
    val persistedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere
    val selectedItem = persistedItem
    val draft = activeDraft
    val isEditing = (uiState.editingContentId != null && uiState.editingContentId == uiState.selectedContentId) || isCreating

    val activeQuestionAudioRef = if (activeDraft != null) activeDraft.questionAudioRef else persistedItem?.questionAudioRef
    val activeAnswerAudioRef = if (activeDraft != null) activeDraft.answerAudioRef else persistedItem?.answerAudioRef
    val activeExampleAudioRef = if (activeDraft != null) activeDraft.exampleAudioRef else persistedItem?.exampleAudioRef
    val activeTranslationAudioRef = if (activeDraft != null) activeDraft.translationAudioRef else persistedItem?.translationAudioRef
    val activeImageRef = if (activeDraft != null) activeDraft.imageRef else persistedItem?.imageRef
    val scrollState = rememberScrollState()

    var imageZoomLevel by remember { mutableStateOf(100) }
    var isFullscreenImageOpen by remember { mutableStateOf(false) }

    if (selectedItem == null && !isCreatingNewItem) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(LESpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                Text(
                    text = "Select a content item from the Explorer",
                    style = LETypography.paneTitle
                )
                Text(
                    text = "Choose an item on the left pane to view or edit its fields and media.",
                    style = LETypography.secondaryMetadata
                )
            }
        }
        return
    }

    // Focus requesters for ordered keyboard navigation
    val questionFocusRequester = remember { FocusRequester() }
    val answerFocusRequester = remember { FocusRequester() }
    val ipaFocusRequester = remember { FocusRequester() }
    val posFocusRequester = remember { FocusRequester() }
    val exampleFocusRequester = remember { FocusRequester() }
    val translationFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LESpacing.lg, vertical = LESpacing.md)
    ) {
        // Top Editor Header Info (Matching Approved Mockup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LESpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                Icon(
                    imageVector = LEIcons.Settings,
                    contentDescription = null,
                    tint = LEColors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isCreatingNewItem) "Creating New Content Item"
                    else if (isEditing) "Editing Item #${selectedItem?.index ?: 0} of ${uiState.allItems.size}"
                    else "Viewing Item #${selectedItem?.index ?: 0} of ${uiState.allItems.size}",
                    style = LETypography.fieldValueEmphasized
                )
                LEStatusBadge(
                    variant = StatusBadgeVariant.Present,
                    customText = if (isEditing) "Editing" else "Active"
                )
            }
        }

        // Scrollable Form & Image Hero Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(LESpacing.md)
        ) {
            if (isEditing && (draft != null || isCreatingNewItem)) {
                LaunchedEffect(isEditing) {
                    questionFocusRequester.requestFocus()
                }

                val currentQuestion = draft?.questionText ?: ""
                val currentAnswer = draft?.answerText ?: ""
                val currentPronunciation = draft?.pronunciation ?: ""
                val currentPos = draft?.partOfSpeech ?: "WORD"
                val currentExample = draft?.exampleText ?: ""
                val currentTranslation = draft?.exampleTranslation ?: ""

                // 1 & 2. QUESTION & ANSWER (Two-column row as per approved layout)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditorFieldCard(
                        label = "Question",
                        value = currentQuestion,
                        onValueChange = { onUpdateDraftQuestion?.invoke(it) },
                        isRequired = true,
                        audioRef = activeQuestionAudioRef,
                        playbackCoordinator = playbackCoordinator,
                        onFallbackPlay = onPlayAudio,
                        onFallbackStop = onStopAudio,
                        focusRequester = questionFocusRequester,
                        nextFocusRequester = answerFocusRequester,
                        modifier = Modifier.weight(1f)
                    )

                    EditorFieldCard(
                        label = "Answer",
                        value = currentAnswer,
                        onValueChange = { onUpdateDraftAnswer?.invoke(it) },
                        isRequired = true,
                        audioRef = activeAnswerAudioRef,
                        playbackCoordinator = playbackCoordinator,
                        onFallbackPlay = onPlayAudio,
                        onFallbackStop = onStopAudio,
                        focusRequester = answerFocusRequester,
                        nextFocusRequester = ipaFocusRequester,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. IPA & 4. POS (Side by Side)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditorFieldCard(
                        label = "IPA",
                        value = currentPronunciation,
                        onValueChange = { onUpdateDraftPronunciation?.invoke(it) },
                        focusRequester = ipaFocusRequester,
                        nextFocusRequester = posFocusRequester,
                        modifier = Modifier.weight(1f)
                    )

                    PosDropdownSelector(
                        selectedPos = currentPos,
                        onPosSelected = { onUpdateDraftPartOfSpeech?.invoke(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 5. EXAMPLE (English)
                EditorFieldCard(
                    label = "Example (English)",
                    value = currentExample,
                    onValueChange = { onUpdateDraftExampleText?.invoke(it) },
                    audioRef = activeExampleAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio,
                    focusRequester = exampleFocusRequester,
                    nextFocusRequester = translationFocusRequester,
                    minLines = 2
                )

                // 6. TRANSLATION (Vietnamese)
                EditorFieldCard(
                    label = "Translation (Vietnamese)",
                    value = currentTranslation,
                    onValueChange = { onUpdateDraftExampleTranslation?.invoke(it) },
                    audioRef = activeTranslationAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio,
                    focusRequester = translationFocusRequester,
                    minLines = 2
                )
            } else if (persistedItem != null) {
                // View Mode Fields using LEFieldCard
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LEFieldCard(
                        label = "Question",
                        value = persistedItem.questionText,
                        isRequired = true,
                        audioRef = activeQuestionAudioRef,
                        playbackCoordinator = playbackCoordinator,
                        onFallbackPlay = onPlayAudio,
                        onFallbackStop = onStopAudio,
                        isTitle = true,
                        modifier = Modifier.weight(1f)
                    )

                    LEFieldCard(
                        label = "Answer",
                        value = persistedItem.answerText,
                        isRequired = true,
                        audioRef = activeAnswerAudioRef,
                        playbackCoordinator = playbackCoordinator,
                        onFallbackPlay = onPlayAudio,
                        onFallbackStop = onStopAudio,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LEFieldCard(
                        label = "IPA",
                        value = if (persistedItem.pronunciation.isNotBlank()) "[${persistedItem.pronunciation}]" else "-",
                        modifier = Modifier.weight(1f)
                    )

                    LEFieldCard(
                        label = "POS (Part of Speech)",
                        value = persistedItem.partOfSpeech.ifBlank { "WORD" },
                        modifier = Modifier.weight(1f)
                    )
                }

                LEFieldCard(
                    label = "Example (English)",
                    value = persistedItem.exampleText ?: "-",
                    audioRef = activeExampleAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )

                LEFieldCard(
                    label = "Translation (Vietnamese)",
                    value = persistedItem.exampleTranslation ?: "-",
                    audioRef = activeTranslationAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )
            }

            // 7. IMAGE HERO BANNER CONTAINER (Matching Approved Mockup)
            Text(
                text = "Image",
                style = LETypography.fieldLabel,
                color = LEColors.textSecondary
            )

            LECard(modifier = Modifier.fillMaxWidth()) {
                val imageRef = activeImageRef
                if (imageRef != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 460.dp)
                            .border(LEBorder.subtle, LERadius.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroImageViewer(
                            reference = imageRef,
                            contentMediaStorage = contentMediaStorage,
                            thumbnailLoader = thumbnailLoader,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Image Controls Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LESpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LESecondaryButton(
                            text = "Open Fullscreen",
                            onClick = { isFullscreenImageOpen = true },
                            icon = LEIcons.Fullscreen
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LEIconButton(
                                icon = LEIcons.ZoomOut,
                                onClick = { if (imageZoomLevel > 50) imageZoomLevel -= 25 },
                                contentDescription = "Zoom out"
                            )
                            Text(
                                text = "$imageZoomLevel%",
                                style = LETypography.statusText,
                                modifier = Modifier.padding(horizontal = LESpacing.xs)
                            )
                            LEIconButton(
                                icon = LEIcons.ZoomIn,
                                onClick = { if (imageZoomLevel < 250) imageZoomLevel += 25 },
                                contentDescription = "Zoom in"
                            )
                            LESecondaryButton(text = "Fit Width", onClick = { imageZoomLevel = 100 })
                            LESecondaryButton(text = "Fit Height", onClick = { imageZoomLevel = 100 })
                            Text(
                                text = "1024 × 682",
                                style = LETypography.caption,
                                color = LEColors.textMuted,
                                modifier = Modifier.padding(start = LESpacing.sm)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Image Attached",
                                style = LETypography.fieldValue,
                                color = LEColors.textMuted
                            )
                            LEDragDropTarget(
                                label = "Drop image here or Browse",
                                hintText = "JPG or PNG up to 5MB"
                            )
                        }
                    }
                }
            }

            // Bottom Status Info Footer
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = LESpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last modified: 2 minutes ago",
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                    Text("Status:", style = LETypography.caption, color = LEColors.textMuted)
                    LEStatusBadge(variant = StatusBadgeVariant.Present, customText = "Ready")
                }
            }
        }
    }

    // Fullscreen Image Preview Dialog
    if (isFullscreenImageOpen && selectedItem?.imageRef != null) {
        Dialog(onDismissRequest = { isFullscreenImageOpen = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LESpacing.xxl),
                shape = LERadius.lg,
                color = LEColors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(LESpacing.lg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fullscreen Preview: ${selectedItem.imageRef}",
                            style = LETypography.paneTitle
                        )
                        LESecondaryButton(text = "Close (Esc)", onClick = { isFullscreenImageOpen = false })
                    }
                    Spacer(modifier = Modifier.height(LESpacing.md))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroImageViewer(
                            reference = selectedItem.imageRef,
                            contentMediaStorage = contentMediaStorage,
                            thumbnailLoader = thumbnailLoader,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroImageViewer(
    reference: String?,
    contentMediaStorage: ContentMediaStorage?,
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
) {
    val result by produceState<ThumbnailResult>(ThumbnailResult.Loading, reference, contentMediaStorage) {
        val storage = contentMediaStorage
        value = if (reference.isNullOrBlank()) {
            ThumbnailResult.Unavailable
        } else if (storage != null) {
            withContext(Dispatchers.IO) {
                val path = storage.resolve(reference)
                if (path == null || !java.nio.file.Files.exists(path)) {
                    ThumbnailResult.Unavailable
                } else {
                    try {
                        val bytes = java.nio.file.Files.readAllBytes(path)
                        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                        ThumbnailResult.Ready(skiaImage.toComposeImageBitmap())
                    } catch (_: Exception) {
                        ThumbnailResult.Unavailable
                    }
                }
            }
        } else {
            withContext(Dispatchers.IO) { thumbnailLoader.load(reference) }
        }
    }

    Box(
        modifier = modifier.background(LEColors.surface),
        contentAlignment = Alignment.Center
    ) {
        when (val current = result) {
            ThumbnailResult.Loading -> Text("Loading image...", style = LETypography.secondaryMetadata, color = LEColors.textMuted)
            ThumbnailResult.Unavailable -> Text("No image", style = LETypography.secondaryMetadata, color = LEColors.textMuted)
            is ThumbnailResult.Ready -> Image(
                bitmap = current.bitmap,
                contentDescription = "Content image hero",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun EditorFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    audioRef: String? = null,
    playbackCoordinator: PlaybackCoordinator? = null,
    onFallbackPlay: ((String) -> Unit)? = null,
    onFallbackStop: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    minLines: Int = 1
) {
    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.md, vertical = LESpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = LESpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = LETypography.fieldLabel, color = LEColors.textSecondary)
                    if (isRequired) {
                        Text(text = " *", style = LETypography.fieldLabel, color = LEColors.danger)
                    }
                }
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = minLines == 1,
                    minLines = minLines,
                    maxLines = if (minLines > 1) 4 else 1,
                    textStyle = LETypography.fieldValue,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LEColors.surface,
                        unfocusedContainerColor = LEColors.surface,
                        disabledContainerColor = LEColors.surface,
                        focusedIndicatorColor = LEColors.borderFocus,
                        unfocusedIndicatorColor = LEColors.borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                        .let { if (nextFocusRequester != null) it.focusProperties { next = nextFocusRequester } else it }
                )
            }

            if (audioRef != null) {
                AudioStateButton(
                    audioRef = audioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onFallbackPlay,
                    onFallbackStop = onFallbackStop
                )
            }
        }
    }
}

@Composable
private fun PosDropdownSelector(
    selectedPos: String,
    onPosSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val posOptions = listOf("WORD", "NOUN", "VERB", "ADJECTIVE", "ADVERB", "PHRASE")

    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier.fillMaxWidth()
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = LESpacing.md, vertical = LESpacing.sm)
            ) {
                Text(text = "POS (Part of Speech)", style = LETypography.fieldLabel, color = LEColors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedPos, style = LETypography.fieldValue)
                    Text("v", style = LETypography.caption, color = LEColors.textMuted)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                posOptions.forEach { pos ->
                    DropdownMenuItem(
                        text = { Text(pos, style = LETypography.fieldValue) },
                        onClick = {
                            onPosSelected(pos)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
