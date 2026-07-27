package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.designsystem.LEColors

internal data class AudioInteractionPresentation(
    val containerColor: Color,
    val border: BorderStroke,
    val iconColor: Color
)

internal fun resolveAudioInteractionPresentation(
    enabled: Boolean,
    hovered: Boolean,
    pressed: Boolean,
    focused: Boolean,
    activeLoop: Boolean,
    baseColor: Color
): AudioInteractionPresentation {
    if (!enabled) {
        return AudioInteractionPresentation(baseColor, BorderStroke(1.dp, LEColors.borderSubtle), LEColors.textMuted)
    }
    val container = when {
        activeLoop -> LEColors.primarySoft
        pressed -> LEColors.audioPressedSurface
        hovered -> LEColors.audioHoverSurface
        else -> baseColor
    }
    val borderColor = when {
        activeLoop -> LEColors.primary
        focused -> LEColors.borderFocus
        hovered || pressed -> LEColors.borderMedium
        else -> LEColors.borderSubtle
    }
    return AudioInteractionPresentation(
        containerColor = container,
        border = BorderStroke(if (activeLoop || focused) 2.dp else 1.dp, borderColor),
        iconColor = if (activeLoop) LEColors.primary else LEColors.textSecondary
    )
}

@Composable
internal fun rememberAudioInteractionPresentation(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    activeLoop: Boolean = false,
    baseColor: Color = LEColors.surface
): AudioInteractionPresentation {
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    return resolveAudioInteractionPresentation(enabled, hovered, pressed, focused, activeLoop, baseColor)
}

internal fun Modifier.audioPressable(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    description: String,
    state: String? = null,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    return semantics {
        role = Role.Button
        contentDescription = description
        state?.let { stateDescription = it }
    }
        .hoverable(interactionSource)
        .clickable(interactionSource = interactionSource, onClick = onClick)
        .onKeyEvent { event ->
            if (
                event.type == KeyEventType.KeyUp &&
                (event.key == Key.Enter || event.key == Key.Spacebar)
            ) {
                onClick()
                true
            } else {
                false
            }
        }
        .focusable(interactionSource = interactionSource)
}
