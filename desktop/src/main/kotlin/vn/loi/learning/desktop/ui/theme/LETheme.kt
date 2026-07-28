package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// CompositionLocals for Design System Tokens
internal val LocalLEColors = staticCompositionLocalOf { LightLEColors }
internal val LocalLETypography = staticCompositionLocalOf { createLETypography(LightLEColors) }
internal val LocalLESpacing = staticCompositionLocalOf { DefaultLESpacing }
internal val LocalLEShapes = staticCompositionLocalOf { DefaultLEShapes }
internal val LocalLEMotion = staticCompositionLocalOf { DefaultLEMotion }
internal val LocalLEElevation = staticCompositionLocalOf { DefaultLEElevation }
internal val LocalLEIcons = staticCompositionLocalOf { DefaultLEIcons }
internal val LocalLEDensity = staticCompositionLocalOf { DefaultLEDensity }
internal val LocalLEBorders = staticCompositionLocalOf { createLEBorderTokens(LightLEColors) }
internal val LocalLEPartOfSpeech = staticCompositionLocalOf { createLEPartOfSpeechTokens(false) }

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

    val partOfSpeech: LEPartOfSpeechTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLEPartOfSpeech.current
}
