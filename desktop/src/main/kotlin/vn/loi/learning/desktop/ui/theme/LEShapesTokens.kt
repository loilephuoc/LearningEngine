package vn.loi.learning.desktop.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner Radius Scale Tokens for Learning Engine 2.0 (PLE-028A Chapter 12).
 * Governs component rounding from Micro Controls to Overlay Modals.
 */
@Immutable
data class LEShapesTokens(
    val radiusNone: Shape = RoundedCornerShape(0.dp),
    val radiusXS: Shape = RoundedCornerShape(4.dp),
    val radiusS: Shape = RoundedCornerShape(6.dp),
    val radiusM: Shape = RoundedCornerShape(8.dp),
    val radiusL: Shape = RoundedCornerShape(12.dp),
    val radiusXL: Shape = RoundedCornerShape(16.dp),
    val radius2XL: Shape = RoundedCornerShape(24.dp),
    val radiusPill: Shape = RoundedCornerShape(999.dp),
    val radiusCircle: Shape = CircleShape,

    // Dp representations
    val radiusNoneDp: Dp = 0.dp,
    val radiusXSDp: Dp = 4.dp,
    val radiusSDp: Dp = 6.dp,
    val radiusMDp: Dp = 8.dp,
    val radiusLDp: Dp = 12.dp,
    val radiusXLDp: Dp = 16.dp,
    val radius2XLDp: Dp = 24.dp
) {
    // Legacy & Semantic Aliases
    val xs: Shape get() = radiusXS
    val sm: Shape get() = radiusS
    val md: Shape get() = radiusM
    val lg: Shape get() = radiusL
    val xl: Shape get() = radiusXL
}

/** Default singleton instance of [LEShapesTokens] */
val DefaultLEShapes = LEShapesTokens()
