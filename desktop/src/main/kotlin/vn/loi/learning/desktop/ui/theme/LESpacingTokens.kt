package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometric 4dp/8dp Spacing Scale Tokens for Learning Engine 2.0 (PLE-028A Chapter 11).
 * Eliminates magic number offsets and enforces consistent visual rhythm across Viewports.
 */
@Immutable
data class LESpacingTokens(
    val space0: Dp = 0.dp,
    val space1: Dp = 2.dp,   // Micro
    val space2: Dp = 4.dp,   // Tight
    val space3: Dp = 8.dp,   // Inline
    val space4: Dp = 12.dp,  // Component
    val space5: Dp = 16.dp,  // Standard Container
    val space6: Dp = 24.dp,  // Section
    val space7: Dp = 32.dp,  // Large Section
    val space8: Dp = 48.dp,  // Page Gutter
    val space9: Dp = 64.dp   // Wide Page Gutter
) {
    // Semantic & Legacy Aliases
    val xxs: Dp get() = space1
    val xs: Dp get() = space2
    val sm: Dp get() = space3
    val md: Dp get() = space4
    val lg: Dp get() = space5
    val xl: Dp get() = space6
    val xxl: Dp get() = space7
    val xxxl: Dp get() = space8
}

/** Default singleton instance of [LESpacingTokens] */
val DefaultLESpacing = LESpacingTokens()
