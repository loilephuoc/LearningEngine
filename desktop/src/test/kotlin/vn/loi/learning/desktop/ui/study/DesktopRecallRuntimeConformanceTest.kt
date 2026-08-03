package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopRecallRuntimeConformanceTest {
    @Test
    fun `all five production runtime routes remain explicit`() {
        val router = source("DesktopMultipleChoiceRuntime.kt")

        listOf(
            "RecallMode.TYPING -> DesktopRecallRenderer.TYPING",
            "RecallMode.MULTIPLE_CHOICE -> DesktopRecallRenderer.MULTIPLE_CHOICE",
            "RecallMode.LISTENING -> DesktopRecallRenderer.LISTENING",
            "RecallMode.IMAGE_RECALL -> DesktopRecallRenderer.IMAGE_RECALL",
            "RecallMode.EXAMPLE_COMPLETION -> DesktopRecallRenderer.EXAMPLE_COMPLETION"
        ).forEach { assertTrue(router.contains(it), it) }
    }

    @Test
    fun `every non Typing runtime initial focus is keyed by attempt identity`() {
        val screen = source("StudyScreen.kt")

        listOf(
            "MultipleChoicePanel(",
            "ListeningRecallPanel(",
            "ImageRecallInputPanel(",
            "ExampleCompletionRecallPanel("
        ).forEach { call ->
            val arguments = screen.substringAfter(call).substringBefore(")\n")
            assertTrue(arguments.contains("focusIdentity = uiState.recallPlan?.planId"), call)
        }
        assertTrue(screen.contains("LaunchedEffect(focusIdentity) { focusRequester.requestFocus() }"))
        assertTrue(screen.contains("LaunchedEffect(focusIdentity, mediaReady)"))
        assertTrue(screen.contains("LaunchedEffect(focusIdentity, ready != null)"))
        assertFalse(screen.contains("LaunchedEffect(Unit) { focusRequester.requestFocus() }"))
    }

    @Test
    fun `Tab Shift Tab and Escape remain untrapped by recall panels`() {
        val panels = source("StudyScreen.kt")
            .substringAfter("private fun MultipleChoicePanel(")
            .substringBefore("internal fun resolveLearningStageLabel")

        assertFalse(panels.contains("Key.Tab"))
        assertFalse(panels.contains("Key.Escape"))
        assertTrue(panels.contains("Key.One, Key.NumPad1"))
        assertTrue(panels.contains("event.isCtrlPressed && event.key == Key.R"))
    }

    @Test
    fun `Listening replay cannot submit and unavailable audio disables every submit path`() {
        val panel = source("StudyScreen.kt")
            .substringAfter("private fun ListeningRecallPanel(")
            .substringBefore("internal fun resolveLearningStageLabel")

        assertTrue(panel.contains("onReplay()"))
        assertTrue(panel.contains("enabled = enabled && audioAvailable"))
        assertTrue(panel.contains("rawInput.isNotBlank() && audioAvailable"))
        assertTrue(panel.contains("enabled = enabled && audioAvailable && rawInput.isNotBlank()"))
        assertFalse(panel.substringAfter("onReplay()").substringBefore("true").contains("onSubmit()"))
    }

    @Test
    fun `all non Typing gates are plan keyed and guard one attempt`() {
        val screen = source("StudyScreen.kt")
        listOf(
            "MultipleChoiceSubmissionGate()",
            "ListeningSubmissionGate()",
            "ImageRecallSubmissionGate()",
            "ExampleCompletionSubmissionGate()"
        ).forEach { gate ->
            assertTrue(
                screen.contains("remember(uiState.recallPlan?.planId) { $gate }"),
                gate
            )
        }
        listOf(
            "DesktopMultipleChoiceRuntime.kt",
            "DesktopListeningRecallRuntime.kt",
            "DesktopImageRecallRuntime.kt",
            "DesktopExampleCompletionRuntime.kt"
        ).forEach { runtime ->
            assertTrue(source(runtime).contains("submitted"), runtime)
        }
    }

    @Test
    fun `all shared pipeline submissions use duplicate safe continuation authority`() {
        val facade = source("StudyFacade.kt")
        listOf("submitMultipleChoice", "submitListening", "submitImageRecall", "submitExampleCompletion")
            .forEachIndexed { index, function ->
                val next = listOf(
                    "submitListening", "submitImageRecall", "submitExampleCompletion", "recallStrategyContext"
                ).get(index)
                val body = facade.substringAfter("fun $function(").substringBefore("fun $next(")
                assertTrue(body.contains("is RecallExecutionResult.DuplicateAttempt -> return load()"), function)
                assertTrue(body.contains("engine.executeRecallLearning("), function)
                assertTrue(body.contains("return loadNextItem("), function)
            }
    }

    @Test
    fun `responsive and accessibility contract stays token based across recall panels`() {
        val panels = source("StudyScreen.kt")
            .substringAfter("private fun MultipleChoicePanel(")
            .substringBefore("internal fun resolveLearningStageLabel")

        assertTrue(panels.contains(".fillMaxWidth()"))
        assertTrue(panels.contains("LETheme.spacing.space3"))
        assertTrue(panels.contains("LETheme.spacing.space4"))
        assertTrue(panels.contains("contentDescription"))
        assertTrue(panels.contains("liveRegion = LiveRegionMode.Polite"))
        assertTrue(panels.contains("softWrap = true"))
        assertFalse(panels.contains("Color(0x"))
    }

    private fun source(name: String): String {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        return (if (fromRoot.isFile) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
            .readText()
    }
}
