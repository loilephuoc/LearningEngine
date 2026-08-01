package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun DiscoveryFrontSurface(
    model: FocusedVocabularyAnswerModel,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    layout: StudyVisualLayout,
    modifier: Modifier = Modifier
) {
    val meaning = model.vietnameseMeaning.ifBlank { "Không có nghĩa tiếng Việt." }
    val hero =
        StudySurfacePresentationResolver.resolve(
            StudySurfaceStage.DISCOVERY,
            StudySurfaceRole.HERO
        )
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
            StudyVocabularyImageBlock(
                imagePath = model.imagePath,
                imageDescription = strings.imageDescription,
                layout = layout,
                surfacePresentation = hero
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = LETheme.colors.surfaceMeaning,
                    shape = LETheme.shapes.radiusXL
                )
                .padding(LESpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
                Text(
                    text = "NGHĨA TIẾNG VIỆT",
                    style = LETheme.typography.fieldLabel,
                    color = LETheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = meaning,
                    style = LETheme.typography.meaningPrimary,
                    fontWeight = FontWeight.Bold,
                    color = LETheme.colors.textPrimary,
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
