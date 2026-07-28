package vn.loi.learning.desktop.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation & Border Tokens for Learning Engine 2.0 (PLE-028A Chapter 13).
 * Governs visual Z-index depth and border definitions across Light and Dark themes.
 */
@Immutable
data class LEElevationTokens(
    val elevation0: Dp = 0.dp,   // Canvas (Layer 0)
    val elevation1: Dp = 1.dp,   // Surface / Card (Layer 1)
    val elevation2: Dp = 2.dp,   // Hovered / Floating Dock (Layer 2)
    val elevation3: Dp = 8.dp,   // Dropdown / Tooltip (Layer 3)
    val elevation4: Dp = 16.dp   // Modal / Scrim Overlay (Layer 4)
)

/** Default singleton instance of [LEElevationTokens] */
val DefaultLEElevation = LEElevationTokens()

/**
 * Border Tokens providing dynamic [BorderStroke] bound to active [LEColors].
 */
@Immutable
data class LEBorderTokens(
    val thin: Dp = 1.dp,
    val medium: Dp = 1.5.dp,
    val thick: Dp = 2.dp,

    val subtle: BorderStroke,
    val default: BorderStroke,
    val focus: BorderStroke,
    val dashed: BorderStroke
)

/** Factory function creating [LEBorderTokens] bound to active [LEColors]. */
fun createLEBorderTokens(colors: LEColors): LEBorderTokens = LEBorderTokens(
    thin = 1.dp,
    medium = 1.5.dp,
    thick = 2.dp,
    subtle = BorderStroke(1.dp, colors.borderSubtle),
    default = BorderStroke(1.dp, colors.borderMedium),
    focus = BorderStroke(1.5.dp, colors.borderFocus),
    dashed = BorderStroke(1.dp, colors.dragDropBorder)
)
