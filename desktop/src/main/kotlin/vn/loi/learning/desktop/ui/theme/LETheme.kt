package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// CompositionLocals for Design System Tokens
val LocalLEColors = staticCompositionLocalOf { LightLEColors }
val LocalLETypography = staticCompositionLocalOf { createLETypography(LightLEColors) }
val LocalLESpacing = staticCompositionLocalOf { DefaultLESpacing }
val LocalLEShapes = staticCompositionLocalOf { DefaultLEShapes }
val LocalLEMotion = staticCompositionLocalOf { DefaultLEMotion }
val LocalLEElevation = staticCompositionLocalOf { DefaultLEElevation }
val LocalLEIcons = staticCompositionLocalOf { DefaultLEIcons }
val LocalLEDensity = staticCompositionLocalOf { DefaultLEDensity }
val LocalLEBorders = staticCompositionLocalOf { createLEBorderTokens(LightLEColors) }

/**
 * Single Entry Point for Learning Engine 2.0 Design Tokens (PLE-028A Contract).
 * UI Components MUST consume tokens strictly through [LETheme] properties.
 */
object LETheme {
    val colors: LEColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLEColors.current

    val typography: LETypography
        @Composable
        @ReadOnlyComposable
        get() = LocalLETypography.current

    val spacing: LESpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLESpacing.current

    val shapes: LEShapesTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLEShapes.current

    val motion: LEMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLEMotion.current

    val elevation: LEElevationTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLEElevation.current

    val icons: LEIconsTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLEIcons.current

    val density: LEDensityTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLEDensity.current

    val borders: LEBorderTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLEBorders.current
}
