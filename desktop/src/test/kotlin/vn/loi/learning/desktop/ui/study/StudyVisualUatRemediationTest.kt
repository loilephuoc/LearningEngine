package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.LightLEColors
import vn.loi.learning.desktop.ui.theme.createLEBorderTokens
import vn.loi.learning.desktop.ui.theme.createLEPartOfSpeechTokens
import vn.loi.learning.desktop.ui.theme.LEPosColorFamily

class StudyVisualUatRemediationTest {
    @Test
    fun `POS badge has semantic container content and border in light and dark`() {
        listOf(false, true).forEach { dark ->
            val style = createLEPartOfSpeechTokens(dark).resolve(LEPosColorFamily.BLUE, 0)
            assertNotEquals(style.containerColor, style.contentColor)
            assertNotEquals(style.containerColor, style.borderColor)
        }
    }

    @Test
    fun `POS content and existing uppercase presentation are preserved`() {
        val tokens = createLEPartOfSpeechTokens(false)
        assertNotEquals(
            tokens.resolve(LEPosColorFamily.BLUE, 0),
            tokens.resolve(LEPosColorFamily.GREEN, 1)
        )
    }

    @Test
    fun `meaning exposes existing POS and omits absent values`() {
        assertEquals("noun", resolveStudyMeaningPos("noun"))
        assertNull(resolveStudyMeaningPos(null))
        assertNull(resolveStudyMeaningPos(""))
        assertNull(resolveStudyMeaningPos("   "))
    }

    @Test
    fun `ready status uses subdued readable semantic text in both themes`() {
        assertEquals(LightLEColors.textSecondary, resolveStudyReadyStatusColor(LightLEColors))
        assertEquals(DarkLEColors.textSecondary, resolveStudyReadyStatusColor(DarkLEColors))
        assertNotEquals(DarkLEColors.accentPrimary, resolveStudyReadyStatusColor(DarkLEColors))
    }

    @Test
    fun `answer hover retains readable light palette`() {
        assertAnswerHoverPalette(LightLEColors)
    }

    @Test
    fun `answer hover retains readable dark palette`() {
        assertAnswerHoverPalette(DarkLEColors)
    }

    @Test
    fun `answer focus and active audio use semantic border without changing container`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            val borders = createLEBorderTokens(colors)
            val focused = resolveStudyAnswerInteractionStyle(
                colors, borders, enabled = true, hovered = false, pressed = false,
                focused = true, activeLoop = false
            )
            val active = resolveStudyAnswerInteractionStyle(
                colors, borders, enabled = true, hovered = true, pressed = false,
                focused = false, activeLoop = true
            )
            assertEquals(colors.surfacePrimary, focused.containerColor)
            assertEquals(colors.borderFocus, focused.borderColor)
            assertEquals(borders.thick, focused.borderWidth)
            assertEquals(colors.surfacePrimary, active.containerColor)
            assertEquals(colors.accentPrimary, active.primaryContentColor)
        }
    }

    @Test
    fun `full screen image is width capped and vertically budgeted`() {
        val standard = StudyVisualLayoutResolver.resolve(1023, 1080, commonTraits)
        val wide = StudyVisualLayoutResolver.resolve(1920, 1080, commonTraits)
        assertEquals(620, standard.imageMaxWidthDp)
        assertEquals(200, standard.imageMaxHeightDp)
        assertEquals(620, wide.imageMaxWidthDp)
        assertEquals(200, wide.imageMaxHeightDp)
        assertEquals(88, wide.ratingDockReservedHeightDp)
    }

    @Test
    fun `narrow and minimum viewport retain usable image and reserve grid dock`() {
        val narrow = StudyVisualLayoutResolver.resolve(560, 800, commonTraits)
        val minimum = StudyVisualLayoutResolver.resolve(320, 640, commonTraits)
        assertTrue(narrow.imageMaxWidthDp >= 240)
        assertTrue(narrow.imageMaxHeightDp >= 120)
        assertTrue(minimum.imageMaxWidthDp >= 240)
        assertTrue(minimum.imageMaxHeightDp >= 120)
        assertEquals(120, minimum.ratingDockReservedHeightDp)
        assertTrue(minimum.preserveRatingReachability)
    }

    @Test
    fun `meaning wraps POS and image keeps Fit without child viewport authority`() {
        val answerSource = studySource("FocusedAnswerSurface.kt")
        assertTrue(answerSource.contains("FlowRow("))
        assertTrue(answerSource.contains("StudyMeaningPosGroup(partOfSpeech = partOfSpeech)"))
        assertTrue(answerSource.contains("text = meaning"))
        assertTrue(answerSource.contains("ContentScale.Fit"))
        assertFalse(answerSource.contains("MaterialTheme.colorScheme"))
        assertFalse(answerSource.contains("Color(0x"))

        val visualSource = studySource("StudyVisualThemePresentation.kt")
        assertFalse(visualSource.contains("BoxWithConstraints"))
        assertFalse(visualSource.contains("viewportWidth"))
        assertFalse(visualSource.contains("vn.loi.learning.domain"))
        assertFalse(visualSource.contains("vn.loi.learning.infrastructure"))
        assertFalse(visualSource.contains("LearningContentAudioController"))
    }

    private fun assertAnswerHoverPalette(colors: vn.loi.learning.desktop.ui.theme.LEColors) {
        val style = resolveStudyAnswerInteractionStyle(
            colors = colors,
            borders = createLEBorderTokens(colors),
            enabled = true,
            hovered = true,
            pressed = false,
            focused = false,
            activeLoop = false
        )
        assertEquals(colors.surfacePrimary, style.containerColor)
        assertEquals(colors.textPrimary, style.primaryContentColor)
        assertNotEquals(style.containerColor, style.primaryContentColor)
        assertNotEquals(style.containerColor, colors.textSecondary)
        val posContent = createLEPartOfSpeechTokens(colors == DarkLEColors)
            .resolve(LEPosColorFamily.BLUE, 0).contentColor
        assertNotEquals(style.containerColor, posContent)
    }

    private fun studySource(name: String): String = studySourceDirectory().resolve(name).readText()

    private fun studySourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
    }

    private companion object {
        val commonTraits = StudyVisualContentTraits(
            hasImage = true,
            hasPronunciation = true,
            hasPartOfSpeech = true,
            hasExamples = true,
            hasSchedulerFeedback = true
        )
    }
}
