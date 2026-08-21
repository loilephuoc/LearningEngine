package vn.loi.learning.android.study.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.R
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.android.ui.StudyRatingColors
import vn.loi.learning.domain.study.memory.model.ReviewRating

@Composable
internal fun StudyRatingBar(
    onRating: (ReviewRating) -> Unit,
    selectedRating: ReviewRating? = null,
    underlinedRating: ReviewRating? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        RatingButton(stringResource(R.string.rating_again), ReviewRating.AGAIN, selectedRating == ReviewRating.AGAIN, underlinedRating == ReviewRating.AGAIN, enabled, onRating, Modifier.weight(1f))
        RatingButton(stringResource(R.string.rating_hard), ReviewRating.HARD, selectedRating == ReviewRating.HARD, underlinedRating == ReviewRating.HARD, enabled, onRating, Modifier.weight(1f))
        RatingButton(stringResource(R.string.rating_good), ReviewRating.GOOD, selectedRating == ReviewRating.GOOD, underlinedRating == ReviewRating.GOOD, enabled, onRating, Modifier.weight(1f))
        RatingButton(stringResource(R.string.rating_easy), ReviewRating.EASY, selectedRating == ReviewRating.EASY, underlinedRating == ReviewRating.EASY, enabled, onRating, Modifier.weight(1f))
    }
}

@Composable
private fun RatingButton(
    label: String,
    rating: ReviewRating,
    selected: Boolean,
    underlined: Boolean,
    enabled: Boolean,
    onRating: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = when (rating) {
        ReviewRating.AGAIN -> StudyRatingColors.again
        ReviewRating.HARD -> StudyRatingColors.hard
        ReviewRating.GOOD -> StudyRatingColors.good
        ReviewRating.EASY -> StudyRatingColors.easy
    }
    val colors = ButtonDefaults.filledTonalButtonColors(
        containerColor = if (selected) palette.border else palette.background,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else palette.content
    )
    FilledTonalButton(
        onClick = { onRating(rating) },
        enabled = enabled,
        colors = colors,
        border = BorderStroke(if (selected) 2.dp else 1.dp, palette.border),
        contentPadding = PaddingValues(horizontal = LearningSpacing.extraSmall),
        modifier = modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics {
            contentDescription = label
            stateDescription = if (selected) "Selected" else "Not selected"
        }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            textDecoration = if (underlined) TextDecoration.Underline else TextDecoration.None,
            maxLines = 1
        )
    }
}

@Composable
internal fun StudyActionDock(
    hasWordAudio: Boolean,
    hasExampleAudio: Boolean,
    hasImage: Boolean,
    isWordPlaying: Boolean,
    isExamplePlaying: Boolean,
    onWordAudio: () -> Unit,
    onReplay: () -> Unit,
    onExampleAudio: () -> Unit,
    onFullscreenImage: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!hasWordAudio && !hasExampleAudio && !hasImage) return
    Row(
        modifier = modifier.semantics { contentDescription = "Study actions" },
        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.large, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasWordAudio) {
            IconButton(onClick = onWordAudio, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isWordPlaying) "Stop word audio" else "Play word audio",
                    tint = if (isWordPlaying) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onReplay, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Default.Replay, contentDescription = "Restart word audio")
            }
        }
        if (hasExampleAudio) {
            IconButton(onClick = onExampleAudio, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = if (isExamplePlaying) "Stop example audio" else "Play example audio",
                    tint = if (isExamplePlaying) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
        if (hasImage) {
            IconButton(onClick = onFullscreenImage, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Open image fullscreen")
            }
        }
    }
}
