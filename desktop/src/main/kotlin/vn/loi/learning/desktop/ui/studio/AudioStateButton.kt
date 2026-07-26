package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LEIcons

@Composable
fun AudioStateButton(
    audioRef: String?,
    playbackCoordinator: PlaybackCoordinator?,
    onFallbackPlay: ((String) -> Unit)?,
    onFallbackStop: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val buttonState = playbackCoordinator?.getButtonState(audioRef) ?: AudioButtonState.Play
    val isPlaying = buttonState is AudioButtonState.Playing
    val isAvailable = buttonState !is AudioButtonState.Unavailable

    IconButton(
        onClick = {
            if (audioRef.isNullOrBlank()) return@IconButton
            if (isPlaying) {
                playbackCoordinator?.stop() ?: onFallbackStop?.invoke()
            } else {
                playbackCoordinator?.play(audioRef) ?: onFallbackPlay?.invoke(audioRef)
            }
        },
        enabled = isAvailable,
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) LEIcons.Stop else LEIcons.Play,
            contentDescription = if (isPlaying) "Stop Audio" else "Play Audio",
            tint = if (isPlaying) LEColors.primary else if (isAvailable) LEColors.textPrimary else LEColors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
