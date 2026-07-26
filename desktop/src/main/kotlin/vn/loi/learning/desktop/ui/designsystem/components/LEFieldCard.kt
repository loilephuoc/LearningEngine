package vn.loi.learning.desktop.ui.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.studio.AudioStateButton
import vn.loi.learning.desktop.ui.studio.PlaybackCoordinator

@Composable
fun LEFieldCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    audioRef: String? = null,
    playbackCoordinator: PlaybackCoordinator? = null,
    onFallbackPlay: ((String) -> Unit)? = null,
    onFallbackStop: (() -> Unit)? = null,
    isTitle: Boolean = false
) {
    Card(
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = LESpacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = LETypography.fieldLabel,
                        color = LEColors.textSecondary
                    )
                    if (isRequired) {
                        Text(
                            text = " *",
                            style = LETypography.fieldLabel,
                            color = LEColors.danger
                        )
                    }
                }
                Spacer(modifier = Modifier.height(LESpacing.xs))
                Text(
                    text = value.ifBlank { "-" },
                    style = if (isTitle) LETypography.fieldValueEmphasized else LETypography.fieldValue,
                    color = if (value.isBlank()) LEColors.textMuted else LEColors.textPrimary
                )
            }

            if (audioRef != null) {
                AudioStateButton(
                    audioRef = audioRef,
                    playbackCoordinator = playbackCoordinator,
                    onFallbackPlay = onFallbackPlay,
                    onFallbackStop = onFallbackStop
                )
            }
        }
    }
}
