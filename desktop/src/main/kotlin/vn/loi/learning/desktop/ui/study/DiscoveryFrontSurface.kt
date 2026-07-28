package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import vn.loi.learning.desktop.ui.designsystem.LEBorder
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing

@Composable
fun DiscoveryFrontSurface(
    model: FocusedVocabularyAnswerModel,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    layout: StudyVisualLayout? = null,
    modifier: Modifier = Modifier
) {
    val meaning = model.vietnameseMeaning.ifBlank { "Không có nghĩa tiếng Việt." }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LESpacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "New content introduction. English answer hidden. Vietnamese meaning: $meaning."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
    ) {
        if (model.imagePath != null) {
            VocabularyImageBlock(
                imagePath = model.imagePath,
                imageDescription = strings.imageDescription,
                layout = layout
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = LERadius.md,
            color = LEColors.surfaceElevated,
            border = LEBorder.subtle
        ) {
            Column(
                modifier = Modifier.padding(LESpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                Text(
                    text = "NGHĨA TIẾNG VIỆT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                model.partOfSpeech?.let { StudyPosBadge(partOfSpeech = it) }
                model.meaningAudioPath?.let { path ->
                    CompactAudioReplayButton(
                        path = path,
                        audioController = audioController,
                        description = "Vietnamese meaning",
                        loops = false
                    )
                }
            }
        }
    }
}
