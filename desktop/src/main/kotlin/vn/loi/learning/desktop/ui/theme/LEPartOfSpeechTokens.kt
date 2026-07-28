package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class LEPosColorFamily {
    BLUE,
    GREEN,
    PURPLE,
    ORANGE,
    CYAN,
    TEAL,
    AMBER,
    ROSE,
    INDIGO,
    SKY,
    EMERALD,
    LIME,
    DEEP_GREEN,
    VIOLET,
    FUCHSIA,
    PINK,
    SLATE,
    BROWN,
    TURQUOISE,
    CORAL,
    NEUTRAL,
    DYNAMIC
}

@Immutable
data class LEPosBadgeStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

@Immutable
class LEPartOfSpeechTokens internal constructor(private val dark: Boolean) {
    fun resolve(family: LEPosColorFamily, visualSlot: Int): LEPosBadgeStyle {
        val hue = when (family) {
            LEPosColorFamily.BLUE -> 220f
            LEPosColorFamily.GREEN -> 142f
            LEPosColorFamily.PURPLE -> 270f
            LEPosColorFamily.ORANGE -> 28f
            LEPosColorFamily.CYAN -> 190f
            LEPosColorFamily.TEAL -> 174f
            LEPosColorFamily.AMBER -> 42f
            LEPosColorFamily.ROSE -> 350f
            LEPosColorFamily.INDIGO -> 240f
            LEPosColorFamily.SKY -> 200f
            LEPosColorFamily.EMERALD -> 158f
            LEPosColorFamily.LIME -> 90f
            LEPosColorFamily.DEEP_GREEN -> 125f
            LEPosColorFamily.VIOLET -> 285f
            LEPosColorFamily.FUCHSIA -> 310f
            LEPosColorFamily.PINK -> 330f
            LEPosColorFamily.SLATE -> 215f
            LEPosColorFamily.BROWN -> 22f
            LEPosColorFamily.TURQUOISE -> 180f
            LEPosColorFamily.CORAL -> 12f
            LEPosColorFamily.NEUTRAL -> 210f
            LEPosColorFamily.DYNAMIC -> ((visualSlot * 137.507764f) % 360f + 360f) % 360f
        }
        val saturation = when (family) {
            LEPosColorFamily.NEUTRAL, LEPosColorFamily.SLATE -> 0.18f
            LEPosColorFamily.BROWN -> 0.55f
            else -> 0.68f
        }
        return if (dark) {
            LEPosBadgeStyle(
                containerColor = Color.hsl(hue, saturation * 0.55f, 0.16f),
                contentColor = Color.hsl(hue, saturation * 0.62f, 0.84f),
                borderColor = Color.hsl(hue, saturation * 0.70f, 0.55f)
            )
        } else {
            LEPosBadgeStyle(
                containerColor = Color.hsl(hue, saturation * 0.62f, 0.94f),
                contentColor = Color.hsl(hue, saturation * 0.82f, 0.24f),
                borderColor = Color.hsl(hue, saturation * 0.75f, 0.50f)
            )
        }
    }
}

internal fun createLEPartOfSpeechTokens(dark: Boolean): LEPartOfSpeechTokens =
    LEPartOfSpeechTokens(dark)
