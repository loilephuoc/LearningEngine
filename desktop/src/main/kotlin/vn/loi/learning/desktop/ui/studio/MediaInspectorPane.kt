package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader

@Composable
fun MediaInspectorPane(
    uiState: PackageContentBrowserUiState,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?,
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
) {
    val selectedItem = uiState.selectedItemInView
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Media & Details Inspector",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Divider()

            if (selectedItem == null) {
                Text("Select an item to view its details.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Surface
            }

            // Image Asset Inspector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Image Asset", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (selectedItem.imageRef != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        LessonThumbnail(selectedItem.imageRef, thumbnailLoader)
                    }
                    Text("File: ${selectedItem.imageRef}", style = MaterialTheme.typography.bodySmall)
                    Text("Availability: Present", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {}, enabled = false) { Text("Replace") }
                } else {
                    Text("No image available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {}, enabled = false) { Text("Add Image") }
                }
            }

            Divider()

            // Audio Assets Inspector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Audio Assets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                AudioRow("Question", selectedItem.questionAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
                AudioRow("Answer", selectedItem.answerAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
                AudioRow("Example", selectedItem.exampleAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
                AudioRow("Translation", selectedItem.translationAudioRef, uiState.activePlayingAudioRef, onPlayAudio, onStopAudio)
            }

            Divider()

            // Quality & Validation Panel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quality Checks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                QualityCheckRow("Image Present", selectedItem.imageRef != null)
                QualityCheckRow("Question Audio Present", selectedItem.questionAudioRef != null)
                QualityCheckRow("Answer Audio Present", selectedItem.answerAudioRef != null)
                QualityCheckRow("IPA Non-Empty", selectedItem.pronunciation.isNotBlank())
                QualityCheckRow("Example Non-Empty", !selectedItem.exampleText.isNullOrBlank())
                QualityCheckRow("Translation Non-Empty", !selectedItem.exampleTranslation.isNullOrBlank())
                QualityCheckRow("Duplicate Content", null) // placeholder
            }
        }
    }
}

@Composable
private fun AudioRow(
    label: String,
    audioRef: String?,
    activePlayingAudioRef: String?,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (audioRef != null) {
                Text(audioRef, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            } else {
                Text("Missing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        if (audioRef != null) {
            val isPlaying = activePlayingAudioRef == audioRef
            IconButton(onClick = { if (isPlaying) onStopAudio?.invoke() else onPlayAudio?.invoke(audioRef) }) {
                Text(if (isPlaying) "⏸" else "▶")
            }
        }
    }
}

@Composable
private fun QualityCheckRow(label: String, isPass: Boolean?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        when (isPass) {
            true -> Text("✅ Pass", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            false -> Text("❌ Fail", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            null -> Text("Not evaluated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
