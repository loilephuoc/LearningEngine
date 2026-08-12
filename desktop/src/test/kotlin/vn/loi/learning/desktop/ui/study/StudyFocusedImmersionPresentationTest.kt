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
    fun `approved workspace fixes discovery and answer reading order`() {
        val workspace = ApprovedStudyWorkspacePresentationResolver.resolve()

        assertEquals(
            listOf(
                ApprovedStudyWorkspaceRegion.HEADER,
                ApprovedStudyWorkspaceRegion.LEXICAL_HERO,
                ApprovedStudyWorkspaceRegion.CONTEXTUAL_IMAGE,
                ApprovedStudyWorkspaceRegion.MEANING,
                ApprovedStudyWorkspaceRegion.TYPING,
                ApprovedStudyWorkspaceRegion.PRIMARY_ACTION,
                ApprovedStudyWorkspaceRegion.DECISION
            ),
            workspace.discoveryOrder
        )
        assertEquals(
            listOf(
                ApprovedStudyWorkspaceRegion.HEADER,
                ApprovedStudyWorkspaceRegion.CONFIRMATION,
                ApprovedStudyWorkspaceRegion.LEXICAL_HERO,
                ApprovedStudyWorkspaceRegion.CONTEXTUAL_IMAGE,
                ApprovedStudyWorkspaceRegion.MEANING,
                ApprovedStudyWorkspaceRegion.EXAMPLES,
                ApprovedStudyWorkspaceRegion.SCHEDULER,
                ApprovedStudyWorkspaceRegion.DECISION
            ),
            workspace.answerOrder
        )
        assertTrue(workspace.lexicalHeroIsPrimary)
        assertTrue(workspace.imageIsSecondary)
        assertTrue(workspace.typingActionIsIntegrated)
        assertTrue(workspace.decisionIsOneSemanticGroup)
    }

    @Test
    fun `discovery keeps the target concealed while presenting image before meaning`() {
        val discovery = source("DiscoveryFrontSurface.kt")
        val image = discovery.indexOf("StudyVocabularyImageBlock(")
        val meaning = discovery.indexOf("text = meaning")

        assertFalse(discovery.contains("VocabularyIdentitySurface("))
        assertTrue(image >= 0)
        assertTrue(image < meaning)
        assertTrue(discovery.containsCodeIgnoringWhitespace("StudySurfaceStage.DISCOVERY, StudySurfaceRole.HERO"))
    }

    @Test
    fun `typing action is integrated in the input without a second button`() {
        val screen = source("StudyScreen.kt")
        val input = screen.substringAfter("private fun CenteredTypingField(")

        assertTrue(input.contains("trailingIcon ="))
        assertTrue(input.contains("onClick = onReveal"))
        assertTrue(input.contains("LEIcons.Success"))
        assertFalse(input.contains("Button(\n            onClick = onReveal"))
    }

    @Test
    fun `answer confirms before hero and reads meaning before examples`() {
        val answer = source("FocusedAnswerSurface.kt")
        val hero = answer.indexOf("VocabularyIdentitySurface(")
        val supporting = answer.substringAfter("private fun ResponsiveAnswerSupportingRegion(")
        val meaning = supporting.indexOf("meaningContent()")
        val examples = supporting.indexOf("examplesContent()")

        assertTrue(hero >= 0)
        assertTrue(meaning >= 0)
        assertTrue(meaning < examples)
        assertTrue(answer.contains("responsivePolicy.layout == AnswerSurfaceLayout.WIDE"))
    }

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
        assertFalse(discovery.usesAccentTone)
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
        assertEquals(760, wide.centralStageMaxWidthDp)
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
        assertTrue(answer.contains("VocabularyIdentitySurface"))
        assertTrue(answer.containsCodeIgnoringWhitespace("HorizontalDivider(color = LETheme.colors.borderSubtle)"))
        assertTrue(front.contains("StudyHeroPresentationResolver.resolve"))
        assertTrue(front.contains("LETheme.colors.accentSoft"))
    }

    @Test
    fun `item arrival keys only item identity and preserves reveal continuity contracts`() {
        val screen = source("StudyScreen.kt")
        val continuity = source("StudySessionContinuityPresentation.kt")

        assertFalse(screen.contains("remember(uiState.currentLearningItemId) { Animatable(0f) }"))
        assertTrue(screen.contains("resolveStudySessionContinuityPresentation(continuityTransition)"))
        assertTrue(screen.contains("continuityPhaseDuration.coerceAtLeast(1)"))
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
