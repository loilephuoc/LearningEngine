package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignatureStudyRuntimeCompositionTest {
    @Test
    fun `runtime orders compact header stage and fixed decision dock`() {
        val screen = source("StudyScreen.kt")
        val header = screen.indexOf("SessionHeader(")
        val stage = screen.indexOf("LearningWorkspaceSurface(", header)
        val dock = screen.indexOf("ActionDock(", stage)

        assertTrue(header >= 0)
        assertTrue(header < stage)
        assertTrue(stage < dock)
        assertTrue(screen.contains("signaturePresentation.headerHeightDp.dp"))
        assertFalse(screen.substringAfter("private fun ActiveSessionChrome(")
            .substringBefore("private fun StudyChromeIconAction(")
            .contains("StudyHeaderStatisticsRow("))
    }

    @Test
    fun `recall is a filled control with one integrated circular action`() {
        val input = source("StudyScreen.kt")
            .substringAfter("private fun CenteredTypingField(")
            .substringBefore("private fun TypingEvaluationFeedback(")

        assertTrue(input.contains("BasicTextField("))
        assertFalse(input.contains("OutlinedTextField("))
        assertTrue(input.contains("verticalAlignment = Alignment.CenterVertically"))
        assertTrue(input.contains("contentAlignment = Alignment.Center"))
        assertTrue(input.contains("shape = LETheme.shapes.radiusPill"))
        assertTrue(input.contains("onClick = onReveal"))
    }

    @Test
    fun `answer uses signature flow and reading passages instead of fit blocks and field rows`() {
        val answer = source("FocusedAnswerSurface.kt")
        val english = answer.substringAfter("fun EnglishExampleAudioRow(")
            .substringBefore("fun VietnameseExampleAudioRow(")
        val vietnamese = answer.substringAfter("fun VietnameseExampleAudioRow(")
            .substringBefore("private fun")

        assertFalse(answer.contains("FullAnswerFitLayout("))
        assertTrue(answer.contains("spacePresentation.imageMaximumHeightDp"))
        assertFalse(english.contains("Surface("))
        assertFalse(vietnamese.contains("Surface("))
        assertTrue(answer.contains("contentScale = ContentScale.Fit"))
    }

    @Test
    fun `decision segments share one group geometry in row and grid modes`() {
        val dock = source("StudyScreen.kt")
            .substringAfter("private fun ActionDock(")
            .substringBefore("private fun ReadOnlyRatingContextDock(")

        assertTrue(dock.contains("contentDescription = \"Rating decision\""))
        assertTrue(dock.contains("studyRatingOrder.chunked(2)"))
        assertTrue(dock.contains("horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)"))
        assertTrue(source("StudyScreen.kt").contains("shape = LETheme.shapes.radiusS"))
    }

    @Test
    fun `resize policy does not key arrival focus or example disclosure state`() {
        val screen = source("StudyScreen.kt")
        val answer = source("FocusedAnswerSurface.kt")

        assertFalse(screen.contains("remember(uiState.currentLearningItemId) { Animatable(0f) }"))
        assertTrue(screen.contains("resolveStudySessionContinuityPresentation(continuityTransition)"))
        assertTrue(answer.contains("remember(currentLearningItemId)"))
        assertFalse(answer.contains("remember(currentLearningItemId, policy.layout)"))
        assertTrue(screen.contains("signaturePresentation.allowAnswerContentScroll"))
    }

    private fun source(name: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
}
