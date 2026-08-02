package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
internal fun DiscoveryFrontSurface(
    model: FocusedVocabularyAnswerModel,
    strings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    layout: StudyVisualLayout,
    signaturePresentation: SignatureStudyPresentation,
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
            .padding(vertical = signaturePresentation.sectionSpacingDp.dp)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Discovery. English word: ${model.englishWord}. Vietnamese meaning: $meaning."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(signaturePresentation.sectionSpacingDp.dp)
    ) {
        VocabularyIdentitySurface(
            word = model.englishWord,
            ipa = model.ipa,
            partOfSpeech = model.partOfSpeech,
            audioPath = model.primaryAudioPath,
            audioController = audioController,
            strings = strings,
            layout = layout,
            stage = StudySurfaceStage.DISCOVERY,
            modifier = Modifier.fillMaxWidth()
        )
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
                .padding(LESpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
                Text(
                    text = meaning,
                    style = LETheme.typography.meaningPrimary,
                    fontWeight = FontWeight.Bold,
                    color = LETheme.colors.accentPrimary,
                    textAlign = TextAlign.Center
                )
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
