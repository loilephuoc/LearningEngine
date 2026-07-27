package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.graphics.SolidColor
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.desktop.ui.designsystem.LEColors

class AudioInteractionPresentationTest {
    @Test
    fun `hover pressed focus and loop presentations follow deterministic precedence`() {
        val idle = resolveAudioInteractionPresentation(
            enabled = true, hovered = false, pressed = false, focused = false,
            activeLoop = false, baseColor = LEColors.surface
        )
        val hovered = resolveAudioInteractionPresentation(
            enabled = true, hovered = true, pressed = false, focused = false,
            activeLoop = false, baseColor = LEColors.surface
        )
        val pressed = resolveAudioInteractionPresentation(
            enabled = true, hovered = true, pressed = true, focused = false,
            activeLoop = false, baseColor = LEColors.surface
        )
        val focused = resolveAudioInteractionPresentation(
            enabled = true, hovered = false, pressed = false, focused = true,
            activeLoop = false, baseColor = LEColors.surface
        )
        val looping = resolveAudioInteractionPresentation(
            enabled = true, hovered = true, pressed = true, focused = true,
            activeLoop = true, baseColor = LEColors.surface
        )

        assertEquals(LEColors.surface, idle.containerColor)
        assertEquals(LEColors.audioHoverSurface, hovered.containerColor)
        assertEquals(LEColors.audioPressedSurface, pressed.containerColor)
        assertEquals(SolidColor(LEColors.borderFocus), focused.border.brush)
        assertEquals(LEColors.primarySoft, looping.containerColor)
        assertEquals(SolidColor(LEColors.primary), looping.border.brush)
        assertEquals(LEColors.primary, looping.iconColor)
    }

    @Test
    fun `disabled surface stays visually non-interactive`() {
        val disabled = resolveAudioInteractionPresentation(
            enabled = false, hovered = true, pressed = true, focused = true,
            activeLoop = true, baseColor = LEColors.surfaceSubtle
        )

        assertEquals(LEColors.surfaceSubtle, disabled.containerColor)
        assertEquals(SolidColor(LEColors.borderSubtle), disabled.border.brush)
        assertEquals(LEColors.textMuted, disabled.iconColor)
    }
}
