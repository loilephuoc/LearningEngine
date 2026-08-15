package vn.loi.learning.android.study.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.loi.learning.android.study.PartOfSpeechPresentation
import vn.loi.learning.android.ui.LearningContentTypography
import vn.loi.learning.android.ui.LearningEngineShapes
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.android.ui.StudyContentSpacing
import vn.loi.learning.android.ui.StudyExampleColors
import vn.loi.learning.android.ui.StudySwipeFeedbackColors
import vn.loi.learning.android.ui.isReducedMotionEnabled

internal enum class StudyTextInteraction {
    AUDIO,
    PASSIVE
}

@Composable
internal fun StudyAnswerSection(
    englishAnswer: String,
    pronunciation: String?,
    partOfSpeech: PartOfSpeechPresentation?,
    vietnameseAnswer: String?,
    englishExample: String?,
    vietnameseExample: String?,
    answerAudioPath: String?,
    englishExampleAudioPath: String?,
    isPlayingAnswer: Boolean,
    isPlayingVietnamese: Boolean,
    isPlayingEnglishExample: Boolean,
    isPlayingVietnameseExample: Boolean,
    onAnswerAudio: () -> Unit,
    onEnglishExampleAudio: () -> Unit,
    modifier: Modifier = Modifier,
    answerHero: Boolean = false,
    allowStandaloneVietnameseExample: Boolean = false,
    swipeSuccessGlowActive: Boolean = false,
    interactionEnabled: Boolean = true
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StudyContentSpacing.lexicalGroup)
        ) {
            StudyAudioTextTarget(
                englishAnswer,
                if (answerHero) MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 41.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                        offset = Offset.Zero,
                        blurRadius = 18f
                    )
                ) else LearningContentTypography.vocabulary,
                answerAudioPath, isPlayingAnswer, true,
                onAnswerAudio, centered = true, strongEmphasis = true, headingSemantics = true,
                swipeSuccessGlowActive = swipeSuccessGlowActive,
                interactionEnabled = interactionEnabled
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                partOfSpeech?.let { PartOfSpeechBadge(it) }
                pronunciation?.let {
                    Text(it, style = LearningContentTypography.pronunciation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        vietnameseAnswer?.takeIf(String::isNotBlank)?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = StudyContentSpacing.lexicalToMeaning),
                contentAlignment = Alignment.Center
            ) {
                StudyAudioTextTarget(
                    it,
                    LearningContentTypography.meaning.copy(fontWeight = FontWeight.SemiBold),
                    null,
                    isPlayingVietnamese,
                    false,
                    null,
                    centered = true,
                    maxLines = 3,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    headingSemantics = false,
                    interaction = StudyTextInteraction.PASSIVE
                )
            }
        }

        if (!englishExample.isNullOrBlank() ||
            allowStandaloneVietnameseExample && !vietnameseExample.isNullOrBlank()
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = StudyContentSpacing.meaningToExamples),
                verticalArrangement = Arrangement.spacedBy(StudyContentSpacing.examplePair)
            ) {
                englishExample?.takeIf(String::isNotBlank)?.let {
                    StudyExampleSurface(
                        it,
                        LearningContentTypography.example.copy(fontWeight = FontWeight.SemiBold),
                        StudyExampleColors.english.background,
                        StudyExampleColors.english.border,
                        StudyExampleColors.english.content,
                        englishExampleAudioPath,
                        isPlayingEnglishExample,
                        true,
                        onEnglishExampleAudio,
                        "English example",
                        interactionEnabled
                    )
                }
                vietnameseExample?.takeIf(String::isNotBlank)?.let {
                    StudyExampleSurface(
                        it,
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        StudyExampleColors.vietnamese.background,
                        StudyExampleColors.vietnamese.border,
                        StudyExampleColors.vietnamese.content,
                        null,
                        isPlayingVietnameseExample,
                        false,
                        null,
                        null,
                        interactionEnabled = false
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyExampleSurface(
    text: String,
    style: TextStyle,
    background: Color,
    border: Color,
    content: Color,
    audioPath: String?,
    isPlaying: Boolean,
    isLooping: Boolean,
    onAudio: (() -> Unit)?,
    accessibilityLabel: String?,
    interactionEnabled: Boolean
) {
    Surface(
        shape = LearningEngineShapes.medium,
        color = background,
        border = BorderStroke(if (isPlaying) 2.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        StudyAudioTextTarget(
            text, style, audioPath, isPlaying, isLooping, onAudio, centered = false,
            contentColor = content, accessibilityLabel = accessibilityLabel,
            boundedAudioTarget = !isLooping,
            interactionEnabled = interactionEnabled,
            interaction = if (onAudio == null) StudyTextInteraction.PASSIVE else StudyTextInteraction.AUDIO
        )
    }
}

@Composable
internal fun StudyAudioTextTarget(
    text: String,
    style: TextStyle,
    audioPath: String?,
    isPlaying: Boolean,
    isLooping: Boolean,
    onToggleAudio: (() -> Unit)?,
    centered: Boolean,
    maxLines: Int = Int.MAX_VALUE,
    strongEmphasis: Boolean = false,
    headingSemantics: Boolean = false,
    contentColor: Color? = null,
    accessibilityLabel: String? = null,
    boundedAudioTarget: Boolean = false,
    swipeSuccessGlowActive: Boolean = false,
    interactionEnabled: Boolean = true,
    interaction: StudyTextInteraction = StudyTextInteraction.AUDIO
) {
    val reducedMotion = isReducedMotionEnabled()
    val swipeGlowAlpha = if (swipeSuccessGlowActive && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "Quick Review headword swipe-success glow")
        val alpha by transition.animateFloat(
            initialValue = 0.60f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = 1_000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "Quick Review headword glow intensity"
        )
        alpha
    } else 1f
    val swipeSuccessScale = if (swipeSuccessGlowActive && !reducedMotion) {
        1f + ((swipeGlowAlpha - 0.60f) / 0.40f).coerceIn(0f, 1f) * 0.04f
    } else 1f
    val breathing = rememberInfiniteTransition(label = "learning audio emphasis")
    val breathingScale by breathing.animateFloat(
        1f, 1.045f,
        infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Reverse),
        label = "active learning target scale"
    )
    val target: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textAlign = if (centered) TextAlign.Center else null
            val textColor = when {
                swipeSuccessGlowActive -> StudySwipeFeedbackColors.quickReviewHeadwordForeground
                contentColor != null -> contentColor
                isPlaying || strongEmphasis -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = swipeSuccessScale
                    scaleY = swipeSuccessScale
                },
                contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
            ) {
                if (swipeSuccessGlowActive) {
                    val orange = StudySwipeFeedbackColors.quickReviewHeadword
                    listOf(
                        Shadow(orange.copy(alpha = 0.25f * swipeGlowAlpha), Offset.Zero, 24f),
                        Shadow(orange.copy(alpha = 0.46f * swipeGlowAlpha), Offset.Zero, 14f),
                        Shadow(orange.copy(alpha = 0.74f * swipeGlowAlpha), Offset.Zero, 6f)
                    ).forEach { glow ->
                        Text(
                            text = text,
                            style = style.copy(shadow = glow),
                            color = Color.Transparent,
                            textAlign = textAlign,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }
                }
                Text(
                    text, style = style, color = textColor, textAlign = textAlign,
                    maxLines = maxLines, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { if (headingSemantics) heading() }
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart) {
        if (interaction == StudyTextInteraction.AUDIO && !audioPath.isNullOrBlank() && onToggleAudio != null) {
            Surface(
                onClick = onToggleAudio,
                enabled = interactionEnabled,
                shape = LearningEngineShapes.large,
                color = if (strongEmphasis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f) else Color.Transparent,
                modifier = (if (centered || boundedAudioTarget) Modifier else Modifier.fillMaxWidth())
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .graphicsLayer {
                        val scale = if (strongEmphasis && isPlaying && isLooping && !reducedMotion) breathingScale else 1f
                        scaleX = scale
                        scaleY = scale
                    }.semantics(mergeDescendants = true) {
                        role = Role.Button
                        stateDescription = if (isPlaying) "Playing" else "Idle"
                        contentDescription = "${accessibilityLabel?.let { "$it: " }.orEmpty()}$text. " +
                                if (isPlaying) "Audio playing, tap to stop" else "Tap to play audio"
                    }
            ) { target() }
        } else target()
    }
}
