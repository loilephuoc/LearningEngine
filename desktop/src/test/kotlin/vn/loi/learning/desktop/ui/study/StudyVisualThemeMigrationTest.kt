package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.LEDensityMode
import vn.loi.learning.desktop.ui.theme.LightLEColors
import vn.loi.learning.desktop.ui.theme.createLEBorderTokens
import vn.loi.learning.desktop.ui.theme.createLEDensityTokens
import vn.loi.learning.desktop.ui.designsystem.components.base.resolveButtonStyle
import vn.loi.learning.desktop.ui.designsystem.components.base.resolveSurfaceStyle

class StudyVisualThemeMigrationTest {
    @Test
    fun `study canvas and text tokens remain readable in both themes`() {
        assertEquals(LightLEColors.windowBackground, LightLEColors.surfaceScheduler)
        assertNotEquals(LightLEColors.windowBackground, LightLEColors.surfaceMeaning)
        assertNotEquals(LightLEColors.textPrimary, LightLEColors.windowBackground)
        assertNotEquals(LightLEColors.textSecondary, LightLEColors.windowBackground)
        assertNotEquals(LightLEColors.textMuted, LightLEColors.windowBackground)

        assertNotEquals(DarkLEColors.windowBackground, DarkLEColors.surfacePrimary)
        assertNotEquals(DarkLEColors.textPrimary, DarkLEColors.windowBackground)
        assertNotEquals(DarkLEColors.textSecondary, DarkLEColors.windowBackground)
        assertNotEquals(DarkLEColors.textMuted, DarkLEColors.windowBackground)
    }

    @Test
    fun `study surface hierarchy resolves semantic roles in light and dark`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            assertEquals(colors.surfacePrimary, resolveSurfaceStyle(colors, StudySurfaceRoles.answer).containerColor)
            assertEquals(colors.surfaceMeaning, resolveSurfaceStyle(colors, StudySurfaceRoles.meaning).containerColor)
            assertEquals(colors.surfaceExample, resolveSurfaceStyle(colors, StudySurfaceRoles.example).containerColor)
            assertEquals(colors.surfaceScheduler, resolveSurfaceStyle(colors, StudySurfaceRoles.scheduler).containerColor)
            assertEquals(colors.surfaceToolbar, resolveSurfaceStyle(colors, StudySurfaceRoles.ratingDock).containerColor)
        }
    }

    @Test
    fun `rating variants and canonical order are deterministic`() {
        assertEquals(
            listOf(
                StudyActionControl.REVIEW_AGAIN,
                StudyActionControl.REVIEW_HARD,
                StudyActionControl.REVIEW_GOOD,
                StudyActionControl.REVIEW_EASY
            ),
            studyRatingOrder
        )
        assertEquals(LEButtonVariant.RATING_AGAIN, resolveStudyRatingVariant(studyRatingOrder[0]))
        assertEquals(LEButtonVariant.RATING_HARD, resolveStudyRatingVariant(studyRatingOrder[1]))
        assertEquals(LEButtonVariant.RATING_GOOD, resolveStudyRatingVariant(studyRatingOrder[2]))
        assertEquals(LEButtonVariant.RATING_EASY, resolveStudyRatingVariant(studyRatingOrder[3]))
    }

    @Test
    fun `rating buttons preserve readable enabled disabled and focused styles`() {
        listOf(
            LEButtonVariant.RATING_AGAIN,
            LEButtonVariant.RATING_HARD,
            LEButtonVariant.RATING_GOOD,
            LEButtonVariant.RATING_EASY
        ).forEach { variant ->
            listOf(LightLEColors, DarkLEColors).forEach { colors ->
                val resting = buttonStyle(colors, variant, enabled = true)
                val disabled = buttonStyle(colors, variant, enabled = false)
                val focused = buttonStyle(colors, variant, enabled = true, focused = true)
                assertNotEquals(resting.containerColor, resting.contentColor)
                assertNotEquals(disabled.containerColor, disabled.contentColor)
                assertEquals(createLEBorderTokens(colors).thick, focused.focusWidth)
                assertEquals(colors.borderFocus, focused.focusColor)
            }
        }
    }

    @Test
    fun `word metadata meaning and example use approved typography roles`() {
        val source = studySource("FocusedAnswerSurface.kt")
        assertTrue(source.contains("LETheme.typography.displayWord") || source.contains("identityWordFontSizeSp"))
        assertTrue(source.contains("LETheme.typography.metadataIpa"))
        assertTrue(source.contains("LETheme.typography.meaningPrimary"))
        assertTrue(source.contains("CompactMeaningLayout()"))
        assertTrue(source.contains("LETheme.typography.sectionTitle"))
        assertTrue(source.contains("LETheme.colors.textSecondary"))
        val resolver = studySource("StudyVisualThemePresentation.kt")
        assertTrue(resolver.contains("colors.textPrimary"))
        assertTrue(resolver.contains("colors.textSecondary"))
    }

    @Test
    fun `migrated study surfaces and actions consume semantic base components`() {
        val screen = studySource("StudyScreen.kt")
        val answer = studySource("FocusedAnswerSurface.kt")
        val scheduler = studySource("CompactSchedulerFeedback.kt")
        assertTrue(screen.contains("LESurface("))
        assertTrue(screen.contains("LEButton("))
        assertTrue(screen.contains("StudySurfaceRoles.answer"))
        assertTrue(screen.contains("StudySurfaceRoles.ratingDock"))
        assertTrue(answer.contains("StudySurfaceRoles.meaning"))
        assertTrue(answer.contains("StudySurfaceRoles.example"))
        assertTrue(scheduler.contains("StudySurfaceRoles.scheduler"))
    }

    @Test
    fun `callbacks enabled rules shortcuts and audio ownership remain external`() {
        val screen = studySource("StudyScreen.kt")
        assertTrue(screen.contains("StudyActionControl.REVIEW_AGAIN to onAgain"))
        assertTrue(screen.contains("StudyActionControl.REVIEW_HARD to onHard"))
        assertTrue(screen.contains("StudyActionControl.REVIEW_GOOD to onGood"))
        assertTrue(screen.contains("StudyActionControl.REVIEW_EASY to onEasy"))
        assertTrue(screen.contains("onClick = callbacks.getValue(control)"))
        assertTrue(screen.contains("enabled = !uiState.actionInProgress"))
        assertTrue(screen.contains("onClick = onPause"))
        assertTrue(screen.contains("onClick = onUndo"))
        assertTrue(screen.contains("performKeyboardAction"))
        assertTrue(screen.contains("LearningContentAudioController("))
        val presentation = studySource("StudyVisualThemePresentation.kt")
        assertFalse(presentation.contains("LearningContentAudioController"))
        assertFalse(presentation.contains("onKeyEvent"))
    }

    @Test
    fun `migrated presentation has no material color or raw color authority`() {
        listOf(
            "StudyScreen.kt",
            "FocusedAnswerSurface.kt",
            "CompactSchedulerFeedback.kt",
            "StudyVisualThemePresentation.kt"
        ).forEach { name ->
            val source = studySource(name)
            assertFalse(source.contains("MaterialTheme.colorScheme"), name)
            assertFalse(source.contains("isSystemInDarkTheme"), name)
            assertFalse(source.contains("Color(0x"), name)
            assertFalse(Regex("""\b(?:java\.io|repository|persistence)\b""").containsMatchIn(source), name)
        }
    }

    @Test
    fun `visual resolver owns neither responsive nor scheduler behavior`() {
        val source = studySource("StudyVisualThemePresentation.kt")
        listOf(
            "BoxWithConstraints",
            "Window",
            "viewportWidth",
            "ReviewScheduler",
            "FSRS",
            "repository",
            "persistence",
            "navigation"
        ).forEach { forbidden -> assertFalse(source.contains(forbidden), forbidden) }
        assertFalse(source.contains(".dp"))
        assertFalse(source.contains("RoundedCornerShape"))
    }

    @Test
    fun `responsive image and interval authorities remain unchanged`() {
        val answer = studySource("FocusedAnswerSurface.kt")
        val screen = studySource("StudyScreen.kt")
        assertTrue(answer.contains("ContentScale.Fit"))
        assertTrue(answer.contains("if (bitmap != null)"))
        assertTrue(answer.contains("imageMaxWidthDp"))
        assertTrue(answer.contains("imageMaxHeightDp"))
        assertTrue(screen.contains("StudyVisualLayoutResolver.resolve("))
        assertTrue(screen.contains("feedback.scheduledInterval"))
    }

    private fun buttonStyle(
        colors: vn.loi.learning.desktop.ui.theme.LEColors,
        variant: LEButtonVariant,
        enabled: Boolean,
        focused: Boolean = false
    ) = resolveButtonStyle(
        colors = colors,
        borders = createLEBorderTokens(colors),
        density = createLEDensityTokens(LEDensityMode.COMFORT),
        variant = variant,
        enabled = enabled,
        hovered = false,
        pressed = false,
        focused = focused
    )

    private fun studySource(name: String): String = studySourceDirectory().resolve(name).readText()

    private fun studySourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
    }
}
