package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight

class TypingFirstPresentationTest {
    @Test
    fun `typing input presentation stays large and responsive`() {
        val wide = TypingPresentationResolver.input(StudyViewportClass.WIDE)
        val standard = TypingPresentationResolver.input(StudyViewportClass.STANDARD)
        val compact = TypingPresentationResolver.input(StudyViewportClass.COMPACT)

        assertTrue(wide.minimumHeightDp >= 116)
        assertTrue(standard.minimumHeightDp >= 104)
        assertTrue(compact.minimumHeightDp >= 96)
        assertEquals(48, wide.typedTextFontSizeSp)
        assertEquals(43, standard.typedTextFontSizeSp)
        assertEquals(38, compact.typedTextFontSizeSp)
        assertEquals(56, wide.typedTextLineHeightSp)
        assertEquals(51, standard.typedTextLineHeightSp)
        assertEquals(46, compact.typedTextLineHeightSp)
        listOf(wide, standard, compact).forEach { presentation ->
            assertEquals(FontWeight.SemiBold, presentation.typedTextFontWeight)
            assertTrue(
                presentation.placeholderFontSizeSp <
                    presentation.typedTextFontSizeSp
            )
            assertTrue(
                presentation.typedTextLineHeightSp >
                    presentation.typedTextFontSizeSp
            )
            assertEquals(TextAlign.Center, presentation.horizontalAlignment)
            assertEquals(0, presentation.letterSpacingSp)
            assertTrue(presentation.placeholderAlpha in 0.65f..0.75f)
        }
        assertEquals(40, wide.placeholderFontSizeSp)
        assertEquals(36, standard.placeholderFontSizeSp)
        assertEquals(32, compact.placeholderFontSizeSp)
        assertEquals(48, wide.placeholderLineHeightSp)
        assertEquals(44, standard.placeholderLineHeightSp)
        assertEquals(40, compact.placeholderLineHeightSp)
        assertTrue(wide.labelFontSizeSp >= 14)
        assertEquals(1f, compact.revealWidthFraction)
        assertTrue(wide.revealWidthFraction < compact.revealWidthFraction)
    }

    @Test
    fun `short input centers vertically while long input retains multiline wrapping`() {
        val empty = TypingPresentationResolver.lineLayout("")
        val short = TypingPresentationResolver.lineLayout("take off")
        val long =
            TypingPresentationResolver.lineLayout("Where is the nearest hospital?")

        listOf(empty, short).forEach { presentation ->
            assertTrue(presentation.singleLine)
            assertEquals(1, presentation.minimumLines)
            assertEquals(1, presentation.maximumLines)
        }
        assertFalse(long.singleLine)
        assertEquals(2, long.minimumLines)
        assertEquals(5, long.maximumLines)
    }

    @Test
    fun `Forced Again owns one action and suppresses free rating shortcut presentation`() {
        val source = studySource("StudyScreen.kt")
        val dockStart = source.indexOf("private fun ActionDock(")
        val dockEnd = source.indexOf("private fun ReadOnlyRatingContextDock(", dockStart)
        val dock = source.substring(dockStart, dockEnd)
        val statusStart = source.indexOf("private fun StatusStrip(")
        val statusEnd = source.indexOf("private fun StudyQuickActionToolbar(", statusStart)
        val status = source.substring(statusStart, statusEnd)

        assertTrue(dock.contains("TypingRatingMode.FORCED_AGAIN"))
        assertTrue(dock.contains("typingContinueAgain"))
        assertTrue(dock.contains("[Enter / Space]"))
        assertTrue(
            dock.indexOf("TypingRatingMode.FORCED_AGAIN") <
                dock.indexOf("dockMode == StudyActionDockMode.ANSWER_ACTIONS")
        )
        assertTrue(status.contains("TypingRatingMode.STANDARD"))
    }

    @Test
    fun `success overlay scales long canonical English without changing its text`() {
        val short = "to sign"
        val long = "Where is the nearest hospital?"
        val shortPresentation =
            TypingPresentationResolver.successOverlay(StudyViewportClass.WIDE, short)
        val longPresentation =
            TypingPresentationResolver.successOverlay(StudyViewportClass.WIDE, long)
        val compact =
            TypingPresentationResolver.successOverlay(StudyViewportClass.COMPACT, long)

        assertTrue(longPresentation.answerFontSizeSp < shortPresentation.answerFontSizeSp)
        assertTrue(compact.answerFontSizeSp <= longPresentation.answerFontSizeSp)
        assertEquals("Where is the nearest hospital?", long)
        assertTrue(compact.horizontalMarginDp > 0)
    }

    @Test
    fun `focus is requested only while typing remains editable`() {
        assertTrue(shouldRequestTypingInputFocus(enabled = true, successInProgress = false))
        assertFalse(shouldRequestTypingInputFocus(enabled = false, successInProgress = false))
        assertFalse(shouldRequestTypingInputFocus(enabled = true, successInProgress = true))
    }

    @Test
    fun `typing front suppresses every manual scene audio role without removing paths`() {
        PresentedAudioRole.entries.forEach { role ->
            assertFalse(
                shouldRenderManualSceneAudio(ManualSceneAudioInteraction.SUPPRESS),
                "Typing must suppress $role"
            )
            assertTrue(
                shouldRenderManualSceneAudio(ManualSceneAudioInteraction.ALLOW),
                "Other modes must retain $role"
            )
        }
        val retainedMeaningPath = java.nio.file.Path.of("meaning.mp3")
        assertEquals(java.nio.file.Path.of("meaning.mp3"), retainedMeaningPath)

        val screen = studySource("StudyScreen.kt")
        val renderer = studySource("LearningSceneRenderer.kt")
        val answerSurface = studySource("FocusedAnswerSurface.kt")
        assertTrue(screen.contains("ManualSceneAudioInteraction.SUPPRESS"))
        assertTrue(renderer.contains("shouldRenderManualSceneAudio(manualSceneAudioInteraction)"))
        assertTrue(renderer.contains("primaryAudio?.path?.takeIf"))
        assertFalse(answerSurface.contains("ManualSceneAudioInteraction.SUPPRESS"))
    }

    @Test
    fun `typing meaning and POS typography stays centered responsive and wrap safe`() {
        val wide = TypingPresentationResolver.meaning(StudyViewportClass.WIDE)
        val compact = TypingPresentationResolver.meaning(StudyViewportClass.COMPACT)
        val source = studySource("LearningSceneRenderer.kt")

        assertTrue(wide.meaningFontSizeSp >= 28)
        assertTrue(compact.meaningFontSizeSp >= 21)
        assertTrue(wide.meaningFontSizeSp > compact.meaningFontSizeSp)
        assertTrue(wide.posFontSizeSp in 13..16)
        assertTrue(source.contains("centered = typingFront"))
        assertTrue(source.contains("TextAlign.Center.takeIf { typingFront }"))
        assertTrue(source.contains("softWrap = true"))
        assertFalse(
            shouldRenderSupportingSceneHeading(SceneType.TYPING, SceneType.MEANING)
        )
        assertTrue(
            shouldRenderSupportingSceneHeading(SceneType.TYPING, SceneType.EXAMPLE)
        )
        assertTrue(
            shouldRenderSupportingSceneHeading(SceneType.PROMPT, SceneType.MEANING)
        )
    }

    @Test
    fun `production input retains editable behavior and keyed autofocus`() {
        val source = studySource("StudyScreen.kt")
        val start = source.indexOf("private fun TypingRecallInput(")
        val end = source.indexOf("private fun TypingSuccessFocusOverlay(", start)
        val input = source.substring(start, end)

        assertTrue(input.contains("LaunchedEffect(focusIdentity, enabled)"))
        assertTrue(input.contains("typingLiveDiffVisualTransformation("))
        assertTrue(input.contains("singleLine = linePresentation.singleLine"))
        assertTrue(input.contains("minLines = linePresentation.minimumLines"))
        assertTrue(input.contains("maxLines = linePresentation.maximumLines"))
        assertTrue(input.contains(".heightIn("))
        assertTrue(input.contains("placeholder ="))
        assertTrue(input.contains("placeholderFontSizeSp"))
        assertTrue(input.contains("typedTextFontSizeSp"))
        assertTrue(input.contains("typedTextLineHeightSp"))
        assertTrue(input.contains("typedTextFontWeight"))
        assertTrue(input.contains("placeholderLineHeightSp"))
        assertTrue(input.contains("placeholderAlpha"))
        assertTrue(input.contains("textAlign = presentation.horizontalAlignment"))
        assertTrue(input.contains("letterSpacing = presentation.letterSpacingSp.sp"))
        assertTrue(input.contains("modifier = Modifier.fillMaxWidth()"))
        assertFalse(input.contains("split("))
        assertFalse(input.contains("forEachIndexed"))
        assertFalse(input.contains("AnimatedContent"))
        assertTrue(input.contains("RoundedCornerShape(16.dp)"))
        assertTrue(input.contains("Shortcut: Enter"))
        assertFalse(input.contains("TextButton("))
    }

    @Test
    fun `success overlay is root level canonical presentation before existing orchestration`() {
        val source = studySource("StudyScreen.kt")
        val effect = source.substring(
            source.indexOf("LaunchedEffect(\n        uiState.currentLearningItemId,\n        typingState.successInProgress"),
            source.indexOf("LaunchedEffect(\n        focusTransitionKey")
        )
        val overlay = source.substring(
            source.indexOf("private fun TypingSuccessFocusOverlay("),
            source.indexOf("private fun TypingEvaluationFeedback(")
        )

        assertTrue(source.contains("(learningScene as? TypingScene)?.prompt?.expectedAnswer"))
        assertTrue(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("1.04f at 280"))
        assertTrue(effect.indexOf("withFrameNanos") < effect.indexOf("awaitTypingAnswerAudio"))
        assertTrue(overlay.contains("fillMaxSize()"))
        assertTrue(overlay.contains("LiveRegionMode.Assertive"))
        assertTrue(overlay.contains("\"Correct. \$canonicalAnswer. \""))
        assertTrue(overlay.contains("typingRatingTransitionAccessibility"))
        assertTrue(overlay.contains("softWrap = true"))
        assertFalse(overlay.contains("vietnameseMeaning"))
        assertFalse(overlay.contains("onTypingCorrectCompleted"))
    }

    @Test
    fun `success blocks competing shortcuts while preserving cancel paths`() {
        val source = studySource("StudyScreen.kt")
        val start = source.indexOf("fun performKeyboardAction(")
        val end = source.indexOf("BoxWithConstraints(", start)
        val keyboard = source.substring(start, end)

        assertTrue(keyboard.contains("typingSuccessInProgress"))
        assertTrue(keyboard.contains("StudyKeyboardAction.UNDO_LATEST"))
        assertTrue(keyboard.contains("StudyKeyboardAction.PAUSE_WORKSPACE"))
        assertTrue(keyboard.contains("action !in"))
    }

    private fun studySource(name: String): String {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        return if (fromRoot.isFile) fromRoot.readText()
        else File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name").readText()
    }
}
