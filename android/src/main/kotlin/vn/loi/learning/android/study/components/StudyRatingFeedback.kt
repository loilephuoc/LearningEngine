package vn.loi.learning.android.study.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.OutgoingStudyFeedback
import vn.loi.learning.android.study.StudyRatingFeedbackPolicy
import vn.loi.learning.android.ui.LearningContentTypography
import vn.loi.learning.android.ui.LearningEngineImage
import vn.loi.learning.android.ui.LearningEngineShapes
import vn.loi.learning.android.ui.LearningImageFitBounds
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.android.ui.StudyRatingColors
import vn.loi.learning.domain.study.memory.model.ReviewRating

@Composable
internal fun StudyRatingFeedbackOverlay(
    feedback: OutgoingStudyFeedback,
    visible: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = when (feedback.selectedRating) {
        ReviewRating.AGAIN -> StudyRatingColors.again
        ReviewRating.HARD -> StudyRatingColors.hard
        ReviewRating.GOOD -> StudyRatingColors.good
        ReviewRating.EASY -> StudyRatingColors.easy
    }
    val pulseDuration = if (reducedMotion) 0 else StudyRatingFeedbackPolicy.pulseMillis
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(pulseDuration)) + scaleIn(tween(pulseDuration), initialScale = 0.985f) +
            slideInVertically(tween(pulseDuration)) { it / 24 },
        exit = fadeOut(tween(if (reducedMotion) 0 else StudyRatingFeedbackPolicy.exitMillis)) +
            scaleOut(tween(if (reducedMotion) 0 else StudyRatingFeedbackPolicy.exitMillis), targetScale = 0.99f) +
            slideOutVertically(tween(if (reducedMotion) 0 else StudyRatingFeedbackPolicy.exitMillis)) { -it / 16 },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            Modifier.fillMaxSize().padding(PaddingValues(horizontal = LearningSpacing.screen, vertical = LearningSpacing.large)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = LearningEngineShapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, colors.border),
                modifier = Modifier.fillMaxWidth().shadow(8.dp, LearningEngineShapes.large)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(LearningSpacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
                ) {
                    feedback.imagePath?.let { imagePath ->
                        LearningEngineImage(
                            imagePath = imagePath,
                            imageUnavailable = false,
                            fillCanvas = true,
                            adaptiveFitBounds = LearningImageFitBounds(120, 280),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).graphicsLayer {
                                scaleX = if (reducedMotion) 1f else 1.025f
                                scaleY = if (reducedMotion) 1f else 1.025f
                            }
                        )
                    }
                    Text(
                        feedback.englishAnswer,
                        style = LearningContentTypography.vocabulary,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
                    ) {
                        feedback.partOfSpeech?.let { PartOfSpeechBadge(it) }
                        feedback.pronunciation?.let {
                            Text(it, style = LearningContentTypography.pronunciation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    feedback.vietnameseAnswer?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = LearningContentTypography.meaning, textAlign = TextAlign.Center)
                    }
                    Surface(shape = LearningEngineShapes.medium, color = colors.background, border = BorderStroke(1.dp, colors.border)) {
                        Text(
                            feedback.selectedRating.name,
                            color = colors.content,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = LearningSpacing.large, vertical = LearningSpacing.small)
                        )
                    }
                }
            }
        }
    }
}
