package vn.loi.learning.desktop.ui.designsystem.components.base

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import vn.loi.learning.desktop.ui.theme.LEBorderTokens
import vn.loi.learning.desktop.ui.theme.LEColors
import vn.loi.learning.desktop.ui.theme.LEDensityTokens

enum class LESurfaceVariant {
    PRIMARY,
    SECONDARY,
    ERROR
}

enum class LEButtonVariant {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE
}

@Immutable
data class LESurfaceStyle(
    val containerColor: Color,
    val contentColor: Color
)

@Immutable
data class LEButtonStyle(
    val containerColor: Color,
    val contentColor: Color,
    val minimumTargetSize: Dp,
    val focusWidth: Dp,
    val focusColor: Color
)

internal fun resolveSurfaceStyle(
    colors: LEColors,
    variant: LESurfaceVariant
): LESurfaceStyle = when (variant) {
    LESurfaceVariant.PRIMARY -> LESurfaceStyle(colors.surfacePrimary, colors.textPrimary)
    LESurfaceVariant.SECONDARY -> LESurfaceStyle(colors.surfaceSecondary, colors.textPrimary)
    LESurfaceVariant.ERROR -> LESurfaceStyle(colors.dangerContainer, colors.dangerText)
}

internal fun resolveButtonStyle(
    colors: LEColors,
    borders: LEBorderTokens,
    density: LEDensityTokens,
    variant: LEButtonVariant,
    enabled: Boolean,
    hovered: Boolean,
    pressed: Boolean,
    focused: Boolean
): LEButtonStyle {
    val restingContainer = when (variant) {
        LEButtonVariant.PRIMARY -> colors.accentPrimary
        LEButtonVariant.SECONDARY -> colors.surfaceSecondary
        LEButtonVariant.DESTRUCTIVE -> colors.danger
    }
    val restingContent = when (variant) {
        LEButtonVariant.PRIMARY, LEButtonVariant.DESTRUCTIVE -> colors.surfacePrimary
        LEButtonVariant.SECONDARY -> colors.textPrimary
    }
    val activeContainer = when {
        !enabled -> colors.surfaceSecondary
        pressed || hovered -> when (variant) {
            LEButtonVariant.PRIMARY -> colors.accentHover
            LEButtonVariant.SECONDARY -> colors.surfaceToolbar
            LEButtonVariant.DESTRUCTIVE -> colors.dangerText
        }
        else -> restingContainer
    }
    return LEButtonStyle(
        containerColor = activeContainer,
        contentColor = if (enabled) restingContent else colors.textDisabled,
        minimumTargetSize = density.minTouchTargetSize,
        focusWidth = if (focused && enabled) borders.thick else borders.thin * 0f,
        focusColor = colors.borderFocus
    )
}
