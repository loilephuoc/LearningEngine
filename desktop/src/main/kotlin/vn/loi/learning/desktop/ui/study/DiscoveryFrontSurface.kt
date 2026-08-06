package vn.loi.learning.desktop.ui.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import vn.loi.learning.desktop.ui.designsystem.LEIcons
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
    val meaning =
        model.vietnameseMeaning.ifBlank {
            "Không có nghĩa tiếng Việt."
        }
    val posDescription =
        model.partOfSpeech
            ?.takeIf { it.isNotBlank() }
            ?.let { " Part of speech: $it." }
            .orEmpty()
    val hero =
        StudySurfacePresentationResolver.resolve(
            StudySurfaceStage.DISCOVERY,
            StudySurfaceRole.HERO
        )
    val hasMeaningAudio = model.meaningAudioPath != null
    val hasHeroImage = model.imagePath != null

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = signaturePresentation.sectionSpacingDp.dp)
                .semantics {
                    contentDescription =
                        "Discovery. Vietnamese meaning: $meaning.$posDescription"
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        model.imagePath?.let { imagePath ->
                StudyVocabularyImageBlock(
                    imagePath = imagePath,
                    imageDescription = strings.imageDescription,
                    audioPath = null,
                    audioController = null,
                    loops = false,
                    layout = layout,
                    surfacePresentation = hero
                )
            }

        model.meaningAudioPath?.let { audioPath ->
            DiscoveryMeaningAudioButton(
                path = audioPath,
                audioController = audioController,
                modifier = Modifier.offset(y = if (hasHeroImage) (-38).dp else 0.dp)
            )
        }

        StudyMeaningPosGroup(
            partOfSpeech = model.partOfSpeech,
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .offset(y = if (hasMeaningAudio && hasHeroImage) (-32).dp else 0.dp)
                    .padding(
                        horizontal = LESpacing.md,
                        vertical = LESpacing.xs
                    ),
            centered = true
        ) {
            val meaningStyle = LETheme.typography.meaningPrimary
            Text(
                text = meaning,
                style =
                    meaningStyle.copy(
                        fontSize = meaningStyle.fontSize * 1.19f,
                        lineHeight = meaningStyle.lineHeight * 1.12f
                    ),
                fontWeight = FontWeight.Bold,
                color = LETheme.colors.accentPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DiscoveryMeaningAudioButton(
    path: Path,
    audioController: LearningContentAudioController,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "discovery-audio-pulse")
    val pulseScale by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.11f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1_050,
                            easing = FastOutSlowInEasing
                        ),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "discovery-audio-pulse-scale"
        )
    val pulseAlpha by
        transition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.42f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1_050,
                            easing = FastOutSlowInEasing
                        ),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "discovery-audio-pulse-alpha"
        )

    Box(
        modifier = modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier =
                Modifier
                    .size(82.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    },
            shape = CircleShape,
            color = LETheme.colors.accentPrimary
        ) {}

        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = LETheme.colors.surfacePrimary,
            contentColor = LETheme.colors.accentPrimary,
            border =
                BorderStroke(
                    width = 2.dp,
                    color = LETheme.colors.accentPrimary
                ),
            shadowElevation = 14.dp
        ) {
            IconButton(
                onClick = { audioController.playOnce(path) },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = "Nghe nghĩa tiếng Việt"
                        }
            ) {
                Icon(
                    imageVector = LEIcons.VietnameseAudio,
                    contentDescription = null,
                    tint = LETheme.colors.accentPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
