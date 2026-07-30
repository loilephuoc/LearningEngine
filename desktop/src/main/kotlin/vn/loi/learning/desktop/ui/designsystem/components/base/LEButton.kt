package vn.loi.learning.desktop.ui.designsystem.components.base

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextDecoration
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun LEButton(
    label: String,
    onClick: () -> Unit,
    variant: LEButtonVariant = LEButtonVariant.PRIMARY,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    showPreviousValueIndicator: Boolean = false,
    supportingLabel: String? = null,
    compact: Boolean = false
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val style = resolveButtonStyle(
        colors = LETheme.colors,
        borders = LETheme.borders,
        density = LETheme.density,
        variant = variant,
        enabled = enabled && !loading,
        hovered = hovered,
        pressed = pressed,
        focused = focused
    )
    val containerColor by animateColorAsState(
        targetValue = style.containerColor,
        animationSpec = tween(LETheme.motion.durationVeryFast)
    )
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactions,
        shape = LETheme.shapes.radiusM,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = style.contentColor,
            disabledContainerColor = style.containerColor,
            disabledContentColor = style.contentColor
        ),
        contentPadding = PaddingValues(
            horizontal = if (compact) LETheme.spacing.space3 else LETheme.spacing.space5,
            vertical = if (compact) LETheme.spacing.space1 else LETheme.spacing.space3
        ),
        modifier = modifier
            .defaultMinSize(
                minWidth = style.minimumTargetSize,
                minHeight = style.minimumTargetSize
            )
            .border(style.focusWidth, style.focusColor, LETheme.shapes.radiusM)
    ) {
        if (loading) {
            CircularProgressIndicator(color = style.contentColor)
        } else {
            val labelStyle =
                if (variant.usesRatingActionTypography()) LETheme.typography.ratingAction
                else LETheme.typography.statusText
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = labelStyle.copy(
                        color = style.contentColor,
                        textDecoration =
                            if (showPreviousValueIndicator) TextDecoration.Underline else null
                    )
                )
                supportingLabel?.let {
                    Text(
                        text = it,
                        style = LETheme.typography.statusText.copy(color = style.contentColor)
                    )
                }
            }
        }
    }
}
