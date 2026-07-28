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
    ERROR,
    ANSWER,
    MEANING,
    EXAMPLE,
    SCHEDULER,
    RATING_DOCK
}

enum class LEButtonVariant {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
    QUIET,
    RATING_AGAIN,
    RATING_HARD,
    RATING_GOOD,
    RATING_EASY
}

internal fun LEButtonVariant.usesRatingActionTypography(): Boolean =
    this == LEButtonVariant.RATING_AGAIN ||
        this == LEButtonVariant.RATING_HARD ||
        this == LEButtonVariant.RATING_GOOD ||
        this == LEButtonVariant.RATING_EASY

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
    LESurfaceVariant.ANSWER -> LESurfaceStyle(colors.surfacePrimary, colors.textPrimary)
    LESurfaceVariant.MEANING -> LESurfaceStyle(colors.surfaceMeaning, colors.textPrimary)
    LESurfaceVariant.EXAMPLE -> LESurfaceStyle(colors.surfaceExample, colors.textPrimary)
    LESurfaceVariant.SCHEDULER -> LESurfaceStyle(colors.surfaceScheduler, colors.textPrimary)
    LESurfaceVariant.RATING_DOCK -> LESurfaceStyle(colors.surfaceToolbar, colors.textPrimary)
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
        LEButtonVariant.QUIET -> colors.surfacePrimary
        LEButtonVariant.RATING_AGAIN -> colors.dangerContainer
        LEButtonVariant.RATING_HARD -> colors.warningContainer
        LEButtonVariant.RATING_GOOD -> colors.successContainer
        LEButtonVariant.RATING_EASY -> colors.infoContainer
    }
    val restingContent = when (variant) {
        LEButtonVariant.PRIMARY, LEButtonVariant.DESTRUCTIVE -> colors.surfacePrimary
        LEButtonVariant.SECONDARY -> colors.textPrimary
        LEButtonVariant.QUIET -> colors.textSecondary
        LEButtonVariant.RATING_AGAIN -> colors.dangerText
        LEButtonVariant.RATING_HARD -> colors.warningText
        LEButtonVariant.RATING_GOOD -> colors.successText
        LEButtonVariant.RATING_EASY -> colors.info
    }
    val activeContainer = when {
        !enabled -> colors.surfaceSecondary
        pressed || hovered -> when (variant) {
            LEButtonVariant.PRIMARY -> colors.accentHover
            LEButtonVariant.SECONDARY -> colors.surfaceToolbar
            LEButtonVariant.DESTRUCTIVE -> colors.dangerText
            LEButtonVariant.QUIET -> colors.surfaceSecondary
            LEButtonVariant.RATING_AGAIN -> colors.danger
            LEButtonVariant.RATING_HARD -> colors.warning
            LEButtonVariant.RATING_GOOD -> colors.success
            LEButtonVariant.RATING_EASY -> colors.info
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
