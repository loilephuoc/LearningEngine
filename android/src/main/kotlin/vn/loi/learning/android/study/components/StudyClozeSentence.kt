package vn.loi.learning.android.study.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.ui.LearningEngineAudioTextRow

@Composable
internal fun StudyClozeSentence(
    prefix: String,
    answer: String,
    suffix: String,
    revealed: Boolean,
    feedback: StudyFeedbackVisualState,
    audioPath: String?,
    isPlaying: Boolean,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = vn.loi.learning.android.ui.isReducedMotionEnabled()
    val answerColor = when (feedback) {
        StudyFeedbackVisualState.CORRECT -> MaterialTheme.colorScheme.tertiaryContainer
        StudyFeedbackVisualState.INCORRECT -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val annotated = buildAnnotatedString {
        append(prefix)
        withStyle(
            SpanStyle(
                background = answerColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (revealed) null else TextDecoration.Underline
            )
        ) {
            append(if (revealed) answer else "        ")
        }
        append(suffix)
    }
    val description = if (revealed) prefix + answer + suffix else "$prefix missing answer $suffix"
    Surface(
        shape = StudyShapes.semanticSurface,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
            .animateContentSize(tween(clozeMotionDurationMillis(reducedMotion)))
            .semantics { if (revealed) liveRegion = LiveRegionMode.Polite }
    ) {
        LearningEngineAudioTextRow(
            annotatedText = annotated,
            style = StudyTypography.contextSentence,
            audioPath = audioPath,
            isPlaying = isPlaying,
            isLooping = true,
            onToggleAudio = onToggleAudio,
            headingSemantics = true,
            contentDescriptionOverride = description,
            modifier = Modifier.padding(StudySpacing.group)
        )
    }
}
