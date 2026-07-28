package vn.loi.learning.desktop.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

/**
 * Desktop Motion & Transition Tokens for Learning Engine 2.0 (PLE-028A Chapter 14).
 * Enforces crisp, responsive micro-interactions without intrusive 3D animation clutter.
 */
@Immutable
data class LEMotionTokens(
    val durationInstant: Int = 0,
    val durationVeryFast: Int = 80,
    val durationFast: Int = 120,
    val durationNormal: Int = 200,
    val durationSlow: Int = 300,
    val durationVerySlow: Int = 400,

    // Contextual interaction durations
    val hoverDuration: Int = 80,
    val ratingDuration: Int = 120,
    val revealDuration: Int = 200,
    val undoDuration: Int = 200,
    val modalDuration: Int = 300,

    // Easing curves
    val easingStandard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val easingDecelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
    val easingAccelerate: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
)

/** Default singleton instance of [LEMotionTokens] */
val DefaultLEMotion = LEMotionTokens()
