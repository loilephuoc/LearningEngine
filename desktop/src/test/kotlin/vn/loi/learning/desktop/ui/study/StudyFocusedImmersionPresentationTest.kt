package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.LightLEColors

class StudyFocusedImmersionPresentationTest {
    @Test
    fun `canvas owns the complete focused immersion layer order`() {
        val canvas = StudyCanvasPresentationResolver.resolve(StudyViewportClass.WIDE)

        assertEquals(
            listOf(
                FocusedImmersionLayer.WORKSPACE,
                FocusedImmersionLayer.LEARNING_STAGE,
                FocusedImmersionLayer.HERO,
                FocusedImmersionLayer.UNDERSTANDING,
                FocusedImmersionLayer.DECISION
            ),
            canvas.orderedLayers
        )
        assertTrue(canvas.stageUsesExpansiveShape)
        assertTrue(canvas.separatesWorkspaceAndStageTone)
    }

    @Test
    fun `discovery and understanding each resolve one focal hero`() {
        val discovery = StudyHeroPresentationResolver.resolve(StudySurfaceStage.DISCOVERY)
        val understanding = StudyHeroPresentationResolver.resolve(StudySurfaceStage.UNDERSTANDING)

        assertEquals(FocusedImmersionContentRole.DISCOVERY_PROMPT, discovery.role)
        assertEquals(FocusedImmersionContentRole.HERO_IDENTITY, understanding.role)
        assertTrue(discovery.focal)
        assertTrue(understanding.focal)
        assertTrue(discovery.usesAccentTone)
        assertTrue(understanding.usesExpansiveShape)
    }

    @Test
    fun `meaning examples scheduler and decision retain semantic product roles`() {
        val content = StudyContentRhythmPresentationResolver.resolve()
        val decision = StudyDecisionAreaPresentationResolver.resolve()

        assertEquals(FocusedImmersionContentRole.KNOWLEDGE, content.meaningRole)
        assertEquals(FocusedImmersionContentRole.READING, content.examplesRole)
        assertEquals(FocusedImmersionContentRole.EXPLANATION, content.schedulerRole)
        assertTrue(content.examplesReadAsContinuousContent)
        assertTrue(content.schedulerRemainsExplanatory)
        assertEquals(FocusedImmersionContentRole.DECISION, decision.role)
        assertTrue(decision.groupedChoices)
        assertTrue(decision.preservesSemanticRatings)
    }

    @Test
    fun `minimum compact preserve stage while standard wide remain centered`() {
        val compact = StudyCanvasPresentationResolver.resolve(StudyViewportClass.COMPACT)
        val standard = StudyCanvasPresentationResolver.resolve(StudyViewportClass.STANDARD)
        val wide = StudyCanvasPresentationResolver.resolve(StudyViewportClass.WIDE)

        assertEquals(StudyVisualLayoutResolver.COMPACT_MAX_WIDTH_DP, compact.centralStageMaxWidthDp)
        assertTrue(standard.centralStageMaxWidthDp < StudyVisualLayoutResolver.STANDARD_MAX_WIDTH_DP)
        assertEquals(1040, wide.centralStageMaxWidthDp)
        assertTrue(compact.orderedLayers.contains(FocusedImmersionLayer.HERO))
        assertTrue(compact.orderedLayers.contains(FocusedImmersionLayer.DECISION))
    }

    @Test
    fun `motion is bounded deterministic and does not identify reveal as item arrival`() {
        assertEquals(
            StudyImmersionMotionResolver.itemArrival(0.4f),
            StudyImmersionMotionResolver.itemArrival(0.4f)
        )
        assertEquals(0f, StudyImmersionMotionResolver.itemArrival(-1f).alpha)
        assertEquals(1f, StudyImmersionMotionResolver.itemArrival(2f).alpha)
        val source = source("StudyFocusedImmersionPresentation.kt")
        assertFalse(source.contains("canReview"))
        assertFalse(source.contains("durationMillis"))
        assertFalse(source.contains("delay("))
    }

    @Test
    fun `light and dark preserve stage and hero tone separation`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            assertFalse(colors.windowBackground == colors.surfacePrimary)
            assertFalse(colors.accentSoft == colors.textPrimary)
        }
    }

    @Test
    fun `Compose consumers render canvas hero knowledge reading and decision contracts`() {
        val screen = source("StudyScreen.kt")
        val answer = source("FocusedAnswerSurface.kt")
        val front = source("LearningSceneRenderer.kt")

        assertTrue(screen.contains("StudyCanvasPresentationResolver.resolve"))
        assertTrue(screen.contains("StudyDecisionAreaPresentationResolver.resolve"))
        assertTrue(screen.contains("LETheme.shapes.radius2XL"))
        assertTrue(answer.contains("StudyHeroPresentationResolver.resolve"))
        assertTrue(answer.contains("StudyContentRhythmPresentationResolver.resolve"))
        assertTrue(front.contains("StudyHeroPresentationResolver.resolve"))
        assertTrue(front.contains("LETheme.colors.accentSoft"))
    }

    @Test
    fun `item arrival keys only item identity and preserves reveal continuity contracts`() {
        val screen = source("StudyScreen.kt")
        val continuity = source("StudySessionContinuityPresentation.kt")

        assertTrue(screen.contains("remember(uiState.currentLearningItemId) { Animatable(0f) }"))
        assertTrue(screen.contains("LETheme.motion.durationNormal"))
        assertTrue(screen.contains("StudyMicroInteractionResolver.reveal"))
        assertTrue(continuity.contains("retainOverlayDuringExit"))
        assertTrue(continuity.contains("StudySessionTransitionDestination.NEXT_ITEM"))
    }

    @Test
    fun `interaction image and accessibility authorities remain in their established owners`() {
        val screen = source("StudyScreen.kt")
        val answer = source("FocusedAnswerSurface.kt")
        val feedback = source("CompactSchedulerFeedback.kt")

        assertTrue(screen.contains("StudyActionControl.REVIEW_AGAIN to onAgain"))
        assertTrue(screen.contains("performKeyboardAction"))
        assertTrue(answer.contains("ContentScale.Fit"))
        assertTrue(answer.contains("imageMaxWidthDp"))
        assertTrue(answer.contains("imageMaxHeightDp"))
        assertTrue(feedback.contains("SchedulerFeedbackContext.ACTIVE_ANSWER"))
        assertTrue(feedback.contains("accessibility.conciseSummary"))
    }

    private fun source(name: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
}
