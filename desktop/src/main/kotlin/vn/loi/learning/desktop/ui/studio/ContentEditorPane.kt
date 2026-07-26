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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader

@Composable
fun ContentEditorPane(
    uiState: PackageContentBrowserUiState,
    playbackCoordinator: PlaybackCoordinator? = null,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
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
    val selectedItem = uiState.selectedItemInView
    val draft = if (uiState.editingContentId != null) uiState.draftEdits else null
    val isEditing = uiState.editingContentId != null && uiState.editingContentId == uiState.selectedContentId
    val scrollState = rememberScrollState()

    var imageZoomLevel by remember { mutableStateOf(100) }
    var isFullscreenImageOpen by remember { mutableStateOf(false) }

    if (selectedItem == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select a content item from the Explorer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose an item on the left pane to view or edit its fields and media.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Editor Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEditing) {
                        Button(
                            onClick = { onSaveEdit?.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save (Ctrl+S)")
                        }
                        OutlinedButton(onClick = { onDiscardEdit?.invoke() }) {
                            Text("Discard (Esc)")
                        }
                    } else {
                        Button(onClick = { onEditContent?.invoke() }) {
                            Text("Edit Item")
                        }
                    }

                    if (onRequestDelete != null && !isEditing) {
                        TextButton(
                            onClick = { onRequestDelete.invoke() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {}, enabled = false) { Text("Duplicate") }
                    OutlinedButton(onClick = {}, enabled = false) { Text("AI Assistant") }
                    OutlinedButton(onClick = {}, enabled = false) { Text("History") }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Scrollable Form & Image Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing && draft != null) {
                LaunchedEffect(isEditing, draft.contentId) {
                    questionFocusRequester.requestFocus()
                }

                // 1. QUESTION
                EditorFieldWithAudio(
                    label = "Question",
                    value = draft.questionText,
                    onValueChange = { onUpdateDraftQuestion?.invoke(it) },
                    audioRef = selectedItem.questionAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio,
                    focusRequester = questionFocusRequester,
                    nextFocusRequester = answerFocusRequester
                )

                // 2. ANSWER
                EditorFieldWithAudio(
                    label = "Answer",
                    value = draft.answerText,
                    onValueChange = { onUpdateDraftAnswer?.invoke(it) },
                    audioRef = selectedItem.answerAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio,
                    focusRequester = answerFocusRequester,
                    nextFocusRequester = ipaFocusRequester
                )

                // 3. IPA & 4. POS (Side by Side)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = draft.pronunciation,
                        onValueChange = { onUpdateDraftPronunciation?.invoke(it) },
                        label = { Text("IPA") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(ipaFocusRequester)
                            .focusProperties { next = posFocusRequester },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draft.partOfSpeech,
                        onValueChange = { onUpdateDraftPartOfSpeech?.invoke(it) },
                        label = { Text("POS (Part of Speech)") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(posFocusRequester)
                            .focusProperties { next = exampleFocusRequester },
                        singleLine = true
                    )
                }

                // 5. EXAMPLE (English)
                EditorFieldWithAudio(
                    label = "Example (English)",
                    value = draft.exampleText,
                    onValueChange = { onUpdateDraftExampleText?.invoke(it) },
                    audioRef = selectedItem.exampleAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio,
                    focusRequester = exampleFocusRequester,
                    nextFocusRequester = translationFocusRequester,
                    minLines = 2,
                    maxLines = 4
                )

                // 6. TRANSLATION (Vietnamese) - SEPARATE FIELD
                EditorFieldWithAudio(
                    label = "Translation (Vietnamese)",
                    value = draft.exampleTranslation,
                    onValueChange = { onUpdateDraftExampleTranslation?.invoke(it) },
                    audioRef = selectedItem.translationAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio,
                    focusRequester = translationFocusRequester
                )
            } else {
                // View Mode
                // 1. QUESTION
                ViewFieldWithAudio(
                    label = "Question",
                    text = selectedItem.questionText,
                    isTitle = true,
                    audioRef = selectedItem.questionAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )

                // 2. ANSWER
                ViewFieldWithAudio(
                    label = "Answer",
                    text = selectedItem.answerText,
                    isTitle = false,
                    audioRef = selectedItem.answerAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )

                // 3. IPA & 4. POS
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("IPA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (selectedItem.pronunciation.isNotBlank()) "[${selectedItem.pronunciation}]" else "-",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("POS (Part of Speech)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (selectedItem.partOfSpeech.isNotBlank()) selectedItem.partOfSpeech else "-",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 5. EXAMPLE (English)
                ViewFieldWithAudio(
                    label = "Example (English)",
                    text = selectedItem.exampleText ?: "-",
                    isTitle = false,
                    audioRef = selectedItem.exampleAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )

                // 6. TRANSLATION (Vietnamese)
                ViewFieldWithAudio(
                    label = "Translation (Vietnamese)",
                    text = selectedItem.exampleTranslation ?: "-",
                    isTitle = false,
                    audioRef = selectedItem.translationAudioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onPlayAudio,
                    onFallbackStop = onStopAudio
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 7. IMAGE VIEWER (Major area of editor)
            Text(
                text = "Image Asset",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedItem.imageRef != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Image Display Box with major height & aspect ratio preservation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 380.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            LessonThumbnail(
                                reference = selectedItem.imageRef,
                                loader = thumbnailLoader
                            )
                        }

                        // Zoom & Preview Toolbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { if (imageZoomLevel > 50) imageZoomLevel -= 25 }) {
                                    Text("-")
                                }
                                Text(
                                    text = "$imageZoomLevel%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                OutlinedButton(onClick = { if (imageZoomLevel < 250) imageZoomLevel += 25 }) {
                                    Text("+")
                                }
                                TextButton(onClick = { imageZoomLevel = 100 }) {
                                    Text("Reset")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { imageZoomLevel = 100 }) { Text("Fit W") }
                                TextButton(onClick = { imageZoomLevel = 100 }) { Text("Fit H") }
                                Button(onClick = { isFullscreenImageOpen = true }) {
                                    Text("Fullscreen")
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Image Attached",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "This content item does not reference an image file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Image Dialog
    if (isFullscreenImageOpen && selectedItem.imageRef != null) {
        Dialog(onDismissRequest = { isFullscreenImageOpen = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fullscreen Preview: ${selectedItem.imageRef}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { isFullscreenImageOpen = false }) {
                            Text("Close (Esc)")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        LessonThumbnail(
                            reference = selectedItem.imageRef,
                            loader = thumbnailLoader
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorFieldWithAudio(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onFallbackPlay: ((String) -> Unit)?,
    onFallbackStop: (() -> Unit)?,
    focusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier
                .weight(1f)
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                .let { if (nextFocusRequester != null) it.focusProperties { next = nextFocusRequester } else it },
            singleLine = maxLines == 1,
            minLines = minLines,
            maxLines = maxLines
        )
        AudioStateButton(
            audioRef = audioRef,
            playbackCoordinator = playbackCoordinator,
            onFallbackPlay = onFallbackPlay,
            onFallbackStop = onFallbackStop
        )
    }
}

@Composable
private fun ViewFieldWithAudio(
    label: String,
    text: String,
    isTitle: Boolean,
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onFallbackPlay: ((String) -> Unit)?,
    onFallbackStop: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                style = if (isTitle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isTitle) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            AudioStateButton(
                audioRef = audioRef,
                playbackCoordinator = playbackCoordinator,
                onFallbackPlay = onFallbackPlay,
                onFallbackStop = onFallbackStop
            )
        }
    }
}

@Composable
fun AudioStateButton(
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onFallbackPlay: ((String) -> Unit)? = null,
    onFallbackStop: (() -> Unit)? = null
) {
    if (audioRef.isNullOrBlank()) {
        OutlinedButton(onClick = {}, enabled = false) {
            Text("No Audio")
        }
        return
    }

    val state = playbackCoordinator?.getButtonState(audioRef) ?: AudioButtonState.Play

    when (state) {
        AudioButtonState.Unavailable -> {
            OutlinedButton(onClick = {}, enabled = false) {
                Text("No Audio")
            }
        }
        AudioButtonState.Loading -> {
            Button(onClick = {}, enabled = false) {
                Text("Loading...")
            }
        }
        AudioButtonState.Playing -> {
            Button(
                onClick = {
                    if (playbackCoordinator != null) {
                        playbackCoordinator.stop()
                    } else {
                        onFallbackStop?.invoke()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("⏸ Stop")
            }
        }
        is AudioButtonState.Error -> {
            OutlinedButton(
                onClick = {
                    if (playbackCoordinator != null) {
                        playbackCoordinator.play(audioRef)
                    } else {
                        onFallbackPlay?.invoke(audioRef)
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cannot play audio")
            }
        }
        AudioButtonState.Play -> {
            Button(
                onClick = {
                    if (playbackCoordinator != null) {
                        playbackCoordinator.play(audioRef)
                    } else {
                        onFallbackPlay?.invoke(audioRef)
                    }
                }
            ) {
                Text("▶ Play")
            }
        }
    }
}
