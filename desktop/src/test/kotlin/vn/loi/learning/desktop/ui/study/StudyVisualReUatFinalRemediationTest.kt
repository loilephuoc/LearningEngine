package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.usesRatingActionTypography
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.LightLEColors
import vn.loi.learning.desktop.ui.theme.createLEBorderTokens
import vn.loi.learning.desktop.ui.theme.createLETypography

class StudyVisualReUatFinalRemediationTest {
    @Test
    fun `meaning POS and rating actions use dedicated readable typography roles`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            val typography = createLETypography(colors)
            assertTrue(typography.meaningPos.fontSize > typography.metadataPos.fontSize)
            assertTrue(typography.ratingAction.fontSize > typography.statusText.fontSize)
            assertTrue(typography.ratingAction.lineHeight > typography.statusText.lineHeight)
            assertEquals(colors.accentPrimary, typography.meaningPos.color)
        }
    }

    @Test
    fun `only rating variants select rating action typography`() {
        listOf(
            LEButtonVariant.RATING_AGAIN,
            LEButtonVariant.RATING_HARD,
            LEButtonVariant.RATING_GOOD,
            LEButtonVariant.RATING_EASY
        ).forEach { assertTrue(it.usesRatingActionTypography()) }
        assertFalse(LEButtonVariant.PRIMARY.usesRatingActionTypography())
        assertFalse(LEButtonVariant.SECONDARY.usesRatingActionTypography())
        assertFalse(LEButtonVariant.QUIET.usesRatingActionTypography())
    }

    @Test
    fun `meaning POS group uses token spacing and centered wrapping children`() {
        val source = studySource("FocusedAnswerSurface.kt")
        assertTrue(source.contains("StudyMeaningPosGroup("))
        assertTrue(source.contains("horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space2)"))
        assertTrue(source.contains("Modifier.align(Alignment.CenterVertically)"))
        assertTrue(source.contains("resolveStudyMeaningPos(partOfSpeech)"))
    }

    @Test
    fun `question meaning receives the same normalized POS as full answer`() {
        val screen = studySource("StudyScreen.kt")
        val renderer = studySource("LearningSceneRenderer.kt")
        val answer = studySource("FocusedAnswerSurface.kt")
        assertTrue(screen.contains("partOfSpeech = answerModel.partOfSpeech"))
        assertTrue(renderer.contains("StudyMeaningPosGroup(partOfSpeech = partOfSpeech)"))
        assertTrue(renderer.contains("block.role == PresentedTextRole.VIETNAMESE_MEANING"))
        assertTrue(answer.contains("partOfSpeech = disclosure.partOfSpeech"))
        assertFalse(renderer.contains("normalizePartOfSpeech"))
        assertFalse(answer.contains("normalizePartOfSpeech"))
    }

    @Test
    fun `English example colors remain readable at rest and hover in both themes`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            val resting = exampleStyle(colors, StudyExampleRowKind.ENGLISH, hovered = false)
            val hovered = exampleStyle(colors, StudyExampleRowKind.ENGLISH, hovered = true)
            assertEquals(colors.surfacePrimary, resting.containerColor)
            assertEquals(resting.containerColor, hovered.containerColor)
            assertEquals(colors.textPrimary, hovered.contentColor)
            assertEquals(colors.danger, hovered.highlightColor)
            assertEquals(colors.textSecondary, hovered.iconColor)
            assertNotEquals(hovered.containerColor, hovered.contentColor)
            assertNotEquals(hovered.containerColor, hovered.highlightColor)
            assertNotEquals(hovered.containerColor, hovered.iconColor)
            assertEquals(colors.borderMedium, hovered.borderColor)
        }
    }

    @Test
    fun `Vietnamese example hover preserves its semantic hierarchy`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            val resting = exampleStyle(colors, StudyExampleRowKind.VIETNAMESE, hovered = false)
            val hovered = exampleStyle(colors, StudyExampleRowKind.VIETNAMESE, hovered = true)
            assertEquals(colors.surfaceSecondary, resting.containerColor)
            assertEquals(resting.containerColor, hovered.containerColor)
            assertEquals(colors.textSecondary, hovered.contentColor)
            assertEquals(colors.danger, hovered.highlightColor)
            assertNotEquals(hovered.containerColor, hovered.contentColor)
        }
    }

    @Test
    fun `example focus and active loop use token border without layout padding`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            val borders = createLEBorderTokens(colors)
            val focused = resolveStudyExampleInteractionStyle(
                colors, borders, StudyExampleRowKind.ENGLISH, enabled = true,
                hovered = false, pressed = false, focused = true, activeLoop = false
            )
            val active = resolveStudyExampleInteractionStyle(
                colors, borders, StudyExampleRowKind.ENGLISH, enabled = true,
                hovered = true, pressed = false, focused = false, activeLoop = true
            )
            assertEquals(colors.borderFocus, focused.borderColor)
            assertEquals(borders.thick, focused.borderWidth)
            assertEquals(colors.accentPrimary, active.iconColor)
            assertEquals(colors.surfacePrimary, active.containerColor)
        }
        assertFalse(studySource("StudyVisualThemePresentation.kt").contains(".padding("))
    }

    @Test
    fun `rating order callbacks shortcuts dimensions and enabled authority remain unchanged`() {
        val screen = studySource("StudyScreen.kt")
        assertEquals(
            listOf(
                StudyActionControl.REVIEW_AGAIN,
                StudyActionControl.REVIEW_HARD,
                StudyActionControl.REVIEW_GOOD,
                StudyActionControl.REVIEW_EASY
            ),
            studyRatingOrder
        )
        assertTrue(screen.contains("onClick = callbacks.getValue(control)"))
        assertTrue(screen.contains("enabled = !uiState.actionInProgress"))
        assertTrue(screen.contains("""label = "[${'$'}{action.shortcutHint}]  ${'$'}{action.visibleLabel}""""))
        assertTrue(screen.contains("modifier.height(64.dp)"))
    }

    @Test
    fun `final remediation presentation dependencies remain bounded`() {
        listOf(
            "FocusedAnswerSurface.kt",
            "LearningSceneRenderer.kt",
            "StudyVisualThemePresentation.kt"
        ).forEach { name ->
            val source = studySource(name)
            assertFalse(source.contains("Color(0x"), name)
            assertFalse(source.contains("isSystemInDarkTheme"), name)
        }
        val resolver = studySource("StudyVisualThemePresentation.kt")
        assertFalse(resolver.contains("LearningContentAudioController"))
        assertFalse(resolver.contains("vn.loi.learning.infrastructure"))
        assertFalse(resolver.contains("repository"))
        assertFalse(resolver.contains("persistence"))
    }

    private fun exampleStyle(
        colors: vn.loi.learning.desktop.ui.theme.LEColors,
        kind: StudyExampleRowKind,
        hovered: Boolean
    ) = resolveStudyExampleInteractionStyle(
        colors = colors,
        borders = createLEBorderTokens(colors),
        kind = kind,
        enabled = true,
        hovered = hovered,
        pressed = false,
        focused = false,
        activeLoop = false
    )

    private fun studySource(name: String): String = studySourceDirectory().resolve(name).readText()

    private fun studySourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
    }
}
