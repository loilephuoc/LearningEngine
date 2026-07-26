package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader

@Composable
fun ContentEditorPane(
    uiState: PackageContentBrowserUiState,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?,
    thumbnailLoader: LessonThumbnailLoader,
    onEditContent: (() -> Unit)?,
    onSaveEdit: (() -> Unit)?,
    onDiscardEdit: (() -> Unit)?,
    onUpdateDraftQuestion: ((String) -> Unit)?,
    onUpdateDraftAnswer: ((String) -> Unit)?,
    onUpdateDraftPronunciation: ((String) -> Unit)?,
    onUpdateDraftPartOfSpeech: ((String) -> Unit)?,
    onUpdateDraftExampleText: ((String) -> Unit)?,
    onUpdateDraftExampleTranslation: ((String) -> Unit)?,
    onRequestDelete: (() -> Unit)?,
    onConfirmDelete: (() -> Unit)?,
    onDismissDelete: (() -> Unit)?,
    onConfirmSaveAndProceed: (() -> Unit)?,
    onConfirmDiscardAndProceed: (() -> Unit)?,
    onCancelUnsavedDialog: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val selectedItem = uiState.selectedItemInView
    val draft = if (uiState.editingContentId != null) uiState.draftEdits else null
    val isEditing = uiState.editingContentId != null && uiState.editingContentId == uiState.selectedContentId
    val scrollState = rememberScrollState()

    if (selectedItem == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a content row to edit", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sticky Toolbar
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEditing) {
                        Button(onClick = { onSaveEdit?.invoke() }) { Text("Save") }
                        OutlinedButton(onClick = { onDiscardEdit?.invoke() }) { Text("Discard") }
                    } else {
                        OutlinedButton(onClick = { onEditContent?.invoke() }) { Text("Edit") }
                    }
                    if (onRequestDelete != null && !isEditing) {
                        TextButton(
                            onClick = { onRequestDelete.invoke() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {}, enabled = false) { Text("Duplicate") }
                    OutlinedButton(onClick = {}, enabled = false) { Text("AI Assistant") }
                    OutlinedButton(onClick = {}, enabled = false) { Text("History") }
                }
            }
        }

        // Editor Scroll Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing && draft != null) {
                val questionFocusRequester = remember { FocusRequester() }
                LaunchedEffect(isEditing, draft.contentId) {
                    if (isEditing) questionFocusRequester.requestFocus()
                }

                EditorFieldWithAudio(
                    label = "Question",
                    value = draft.questionText,
                    onValueChange = { onUpdateDraftQuestion?.invoke(it) },
                    audioRef = selectedItem.questionAudioRef,
                    activePlayingAudioRef = uiState.activePlayingAudioRef,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio,
                    modifier = Modifier.focusRequester(questionFocusRequester)
                )

                EditorFieldWithAudio(
                    label = "Answer",
                    value = draft.answerText,
                    onValueChange = { onUpdateDraftAnswer?.invoke(it) },
                    audioRef = selectedItem.answerAudioRef,
                    activePlayingAudioRef = uiState.activePlayingAudioRef,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = draft.pronunciation,
                        onValueChange = { onUpdateDraftPronunciation?.invoke(it) },
                        label = { Text("IPA") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draft.partOfSpeech,
                        onValueChange = { onUpdateDraftPartOfSpeech?.invoke(it) },
                        label = { Text("POS") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                EditorFieldWithAudio(
                    label = "Example (English)",
                    value = draft.exampleText ?: "",
                    onValueChange = { onUpdateDraftExampleText?.invoke(it) },
                    audioRef = selectedItem.exampleAudioRef,
                    activePlayingAudioRef = uiState.activePlayingAudioRef,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio,
                    minLines = 2,
                    maxLines = 4
                )

                EditorFieldWithAudio(
                    label = "Translation (Vietnamese)",
                    value = draft.exampleTranslation ?: "",
                    onValueChange = { onUpdateDraftExampleTranslation?.invoke(it) },
                    audioRef = selectedItem.translationAudioRef,
                    activePlayingAudioRef = uiState.activePlayingAudioRef,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio
                )
            } else {
                // View Mode
                Text("Question", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedItem.questionText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AudioButton(selectedItem.questionAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
                }
                
                Text("Answer", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedItem.answerText, style = MaterialTheme.typography.bodyLarge)
                    AudioButton(selectedItem.answerAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("IPA", style = MaterialTheme.typography.labelSmall)
                        Text(if (selectedItem.pronunciation.isNotBlank()) "[${selectedItem.pronunciation}]" else "-")
                    }
                    Column {
                        Text("POS", style = MaterialTheme.typography.labelSmall)
                        Text(if (selectedItem.partOfSpeech.isNotBlank()) selectedItem.partOfSpeech else "-")
                    }
                }
                
                Text("Example (English)", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedItem.exampleText ?: "-", style = MaterialTheme.typography.bodyMedium)
                    AudioButton(selectedItem.exampleAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
                }
                
                Text("Translation (Vietnamese)", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedItem.exampleTranslation ?: "-", style = MaterialTheme.typography.bodyMedium)
                    AudioButton(selectedItem.translationAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
                }
            }

            // Image Viewer (30-40% height)
            if (selectedItem.imageRef != null) {
                Text("Image", style = MaterialTheme.typography.labelSmall)
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 350.dp), contentAlignment = Alignment.Center) {
                    LessonThumbnail(selectedItem.imageRef, thumbnailLoader)
                }
                // Image zoom controls (disabled placeholders)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {}, enabled = false) { Text("Fullscreen") }
                    TextButton(onClick = {}, enabled = false) { Text("-") }
                    Text("100%")
                    TextButton(onClick = {}, enabled = false) { Text("+") }
                    TextButton(onClick = {}, enabled = false) { Text("Fit W") }
                    TextButton(onClick = {}, enabled = false) { Text("Fit H") }
                }
            } else {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("No Image Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    activePlayingAudioRef: String?,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier.weight(1f),
            singleLine = maxLines == 1,
            minLines = minLines,
            maxLines = maxLines
        )
        AudioButton(audioRef, activePlayingAudioRef, onPlayAudio, onStopAudio)
    }
}

@Composable
private fun AudioButton(
    audioRef: String?,
    activePlayingAudioRef: String?,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?
) {
    if (audioRef.isNullOrBlank()) {
        OutlinedButton(onClick = {}, enabled = false) { Text("No Audio") }
    } else {
        val isPlaying = activePlayingAudioRef == audioRef
        Button(
            onClick = {
                if (isPlaying) onStopAudio?.invoke() else onPlayAudio?.invoke(audioRef)
            }
        ) {
            Text(if (isPlaying) "Stop" else "Play")
        }
    }
}
