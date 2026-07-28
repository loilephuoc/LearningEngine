package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Display Density Modes for Learning Engine 2.0 (PLE-028A Chapter 18).
 */
enum class LEDensityMode {
    COMFORT,
    COMPACT,
    TOUCH
}

/**
 * Display Density Tokens for adjusting target sizes and padding.
 */
@Immutable
data class LEDensityTokens(
    val mode: LEDensityMode = LEDensityMode.COMFORT,
    val spacingMultiplier: Float = 1.0f,
    val minTouchTargetSize: Dp = 40.dp,
    val cardPadding: Dp = 16.dp
)

/** Factory function for creating [LEDensityTokens] based on [LEDensityMode] */
fun createLEDensityTokens(mode: LEDensityMode): LEDensityTokens = when (mode) {
    LEDensityMode.COMFORT -> LEDensityTokens(
        mode = LEDensityMode.COMFORT,
        spacingMultiplier = 1.0f,
        minTouchTargetSize = 40.dp,
        cardPadding = 16.dp
    )
    LEDensityMode.COMPACT -> LEDensityTokens(
        mode = LEDensityMode.COMPACT,
        spacingMultiplier = 0.75f,
        minTouchTargetSize = 32.dp,
        cardPadding = 12.dp
    )
    LEDensityMode.TOUCH -> LEDensityTokens(
        mode = LEDensityMode.TOUCH,
        spacingMultiplier = 1.25f,
        minTouchTargetSize = 48.dp,
        cardPadding = 24.dp
    )
}

/** Default singleton instance of [LEDensityTokens] */
val DefaultLEDensity = createLEDensityTokens(LEDensityMode.COMFORT)
