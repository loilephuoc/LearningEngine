package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.resolveSurfaceStyle
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.LightLEColors

class StudyVisualFocusTest {
    @Test
    fun `question and answer content are primary anchors`() {
        listOf(StudyVisualFocusRole.QUESTION_CONTENT, StudyVisualFocusRole.ANSWER_CONTENT).forEach {
            assertEquals(StudyVisualEmphasis.PRIMARY, focus(it).emphasis)
            assertEquals(700, focus(it).hierarchyWeight)
        }
    }

    @Test
    fun `meaning is the strongest supporting focus`() {
        val meaning = focus(StudyVisualFocusRole.MEANING)
        assertEquals(StudyVisualEmphasis.SECONDARY_PRIMARY, meaning.emphasis)
        assertTrue(meaning.hierarchyWeight > focus(StudyVisualFocusRole.EXAMPLE).hierarchyWeight)
        assertTrue(meaning.hierarchyWeight > focus(StudyVisualFocusRole.SCHEDULER).hierarchyWeight)
    }

    @Test
    fun `example supports content above explanatory scheduler metadata`() {
        val example = focus(StudyVisualFocusRole.EXAMPLE)
        val scheduler = focus(StudyVisualFocusRole.SCHEDULER)
        assertEquals(StudyVisualEmphasis.SUPPORTING, example.emphasis)
        assertEquals(StudyVisualEmphasis.EXPLANATORY, scheduler.emphasis)
        assertTrue(example.hierarchyWeight > scheduler.hierarchyWeight)
    }

    @Test
    fun `header and metadata remain orientation and secondary information`() {
        assertEquals(StudyVisualEmphasis.ORIENTATION, focus(StudyVisualFocusRole.HEADER).emphasis)
        assertEquals(StudyVisualEmphasis.SECONDARY, focus(StudyVisualFocusRole.METADATA).emphasis)
        assertTrue(
            focus(StudyVisualFocusRole.HEADER).hierarchyWeight <
                focus(StudyVisualFocusRole.SCHEDULER).hierarchyWeight
        )
    }

    @Test
    fun `rating dock is an action destination below learning content`() {
        val action = focus(StudyVisualFocusRole.RATING_DOCK)
        assertEquals(StudyVisualEmphasis.ACTION, action.emphasis)
        assertTrue(action.hierarchyWeight < focus(StudyVisualFocusRole.MEANING).hierarchyWeight)
        assertTrue(action.hierarchyWeight > focus(StudyVisualFocusRole.EXAMPLE).hierarchyWeight)
    }

    @Test
    fun `rating actions retain all semantic color identities`() {
        assertTrue(focus(StudyVisualFocusRole.RATING_ACTION).preservesSemanticRatingIdentity)
        assertEquals(LEButtonVariant.RATING_AGAIN, resolveStudyRatingVariant(StudyActionControl.REVIEW_AGAIN))
        assertEquals(LEButtonVariant.RATING_HARD, resolveStudyRatingVariant(StudyActionControl.REVIEW_HARD))
        assertEquals(LEButtonVariant.RATING_GOOD, resolveStudyRatingVariant(StudyActionControl.REVIEW_GOOD))
        assertEquals(LEButtonVariant.RATING_EASY, resolveStudyRatingVariant(StudyActionControl.REVIEW_EASY))
    }

    @Test
    fun `scheduler is never a selected rating presentation`() {
        val scheduler = focus(StudyVisualFocusRole.SCHEDULER)
        assertFalse(scheduler.selectedRatingPresentation)
        assertFalse(scheduler.preservesSemanticRatingIdentity)
    }

    @Test
    fun `light and dark themes preserve semantic surface separation and readable content`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            val primary = focus(StudyVisualFocusRole.ANSWER_CONTENT)
            val meaning = focus(StudyVisualFocusRole.MEANING)
            val example = focus(StudyVisualFocusRole.EXAMPLE)
            assertNotEquals(
                resolveSurfaceStyle(colors, primary.surfaceVariant).containerColor,
                primary.resolveContentColor(colors)
            )
            assertNotEquals(
                resolveSurfaceStyle(colors, meaning.surfaceVariant).containerColor,
                meaning.resolveContentColor(colors)
            )
            assertNotEquals(
                resolveSurfaceStyle(colors, example.surfaceVariant).containerColor,
                example.resolveContentColor(colors)
            )
            assertNotEquals(
                resolveSurfaceStyle(colors, meaning.surfaceVariant).containerColor,
                resolveSurfaceStyle(colors, example.surfaceVariant).containerColor
            )
        }
    }

    @Test
    fun `responsive modes retain identical semantic hierarchy`() {
        StudyViewportClass.entries.forEach { viewport ->
            StudyVisualFocusRole.entries.forEach { role ->
                assertEquals(focus(role), StudyVisualFocusResolver.resolve(role, viewport))
            }
        }
    }

    @Test
    fun `same input resolves deterministically`() {
        StudyVisualFocusRole.entries.forEach { role ->
            assertEquals(focus(role), focus(role))
        }
    }

    @Test
    fun `study consumers use shared visual focus authority`() {
        val screen = source("StudyScreen.kt")
        val answer = source("FocusedAnswerSurface.kt")
        val scheduler = source("CompactSchedulerFeedback.kt")
        val statistics = source("StudyStatisticsDashboard.kt")
        assertTrue(screen.contains("StudyVisualFocusResolver.resolve("))
        assertTrue(answer.contains("StudyVisualFocusResolver.resolve("))
        assertTrue(scheduler.contains("StudyVisualFocusResolver.resolve("))
        assertTrue(statistics.contains("StudyVisualFocusResolver.resolve("))
    }

    @Test
    fun `visual focus boundary contains no policy persistence or raw visual values`() {
        val source = source("StudyVisualThemePresentation.kt")
        listOf("ReviewScheduler", "FSRS", "repository", "persistence", "StudyUiState", "Color(0x")
            .forEach { assertFalse(source.contains(it), it) }
        assertFalse(source.contains(".dp"))
        assertFalse(source.contains("alpha ="))
    }

    @Test
    fun `layout image typography spacing rating motion and accessibility contracts remain owned`() {
        val screen = source("StudyScreen.kt")
        val answer = source("FocusedAnswerSurface.kt")
        val layout = source("StudyVisualLayout.kt")
        assertTrue(screen.indexOf("LearningWorkspaceSurface(") < screen.indexOf("ActionDock("))
        assertTrue(answer.contains("ContentScale.Fit"))
        assertTrue(answer.contains("imageMaxWidthDp"))
        assertTrue(answer.contains("imageMaxHeightDp"))
        assertTrue(answer.contains("LETheme.typography.displayWord"))
        assertTrue(layout.contains("FullAnswerDensityClass.COMPACT -> 12"))
        assertTrue(layout.contains("FullAnswerDensityClass.MINIMUM -> 8"))
        assertTrue(screen.contains("LETheme.motion.ratingDuration"))
        assertTrue(screen.contains("ratingConfirmationAccessibility"))
        assertTrue(screen.contains("stateDescription"))
    }

    private fun focus(role: StudyVisualFocusRole) = StudyVisualFocusResolver.resolve(role)

    private fun source(name: String): String = sourceDirectory().resolve(name).readText()

    private fun sourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) fromRoot
        else File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
    }
}
