package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypingAutoRatingTimerCompositionTest {
    private val source = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
    )

    @Test
    fun `timer is a dedicated prominent vector presentation after scene content`() {
        val timerStart = source.indexOf("private fun TypingAutoRatingTimerPanel(")
        val timerEnd = source.indexOf("private fun typingDecisionExplanation(", timerStart)
        val timer = source.substring(timerStart, timerEnd)
        val item = source.substring(
            source.indexOf("private fun StudyItemCard("),
            source.indexOf("internal fun resolveLearningStageLabel(")
        )

        assertTrue(timer.contains("LETheme.icons.Timer"))
        assertTrue(timer.contains("visual.valueFontSizeSp.sp"))
        assertTrue(timer.contains("FontFamily.Monospace"))
        assertTrue(timer.contains("FontWeight.Bold"))
        assertFalse(timer.contains("labelLarge"))
        assertFalse(timer.contains("⏱"))
        assertTrue(
            item.indexOf("LearningSceneRenderer(") <
                item.indexOf("TypingAutoRatingTimerPanel(")
        )
        assertTrue(
            item.indexOf("TypingAutoRatingTimerPanel(") <
                item.indexOf("CenteredTypingField(")
        )
    }

    @Test
    fun `semantic rating colors preserve Good green and Easy blue`() {
        val resolverStart = source.indexOf("private fun resolveTypingRatingPreviewColor(")
        val resolverEnd = source.indexOf("private fun FlowProgressIndicator(", resolverStart)
        val resolver = source.substring(resolverStart, resolverEnd)

        assertTrue(resolver.contains("TypingRatingColorRole.AGAIN -> colors.danger"))
        assertTrue(resolver.contains("TypingRatingColorRole.HARD -> colors.warning"))
        assertTrue(resolver.contains("TypingRatingColorRole.GOOD -> colors.success"))
        assertTrue(resolver.contains("TypingRatingColorRole.EASY -> colors.info"))
    }

    @Test
    fun `persistent explanation and automatic rating legend are absent from Study surface`() {
        val timerStart = source.indexOf("private fun TypingAutoRatingTimerPanel(")
        val timerEnd = source.indexOf("private fun typingDecisionExplanation(", timerStart)
        val timer = source.substring(timerStart, timerEnd)

        assertFalse(timer.contains("TypingRatingLegend("))
        assertFalse(source.contains("private fun TypingRatingLegend("))
        assertFalse(timer.contains("speed ·"))
        assertFalse(timer.contains("workspaceStrings.typingLegend"))
        assertTrue(timer.contains("typingProjectedRating(ratingLabel)"))
    }

    @Test
    fun `timer semantics includes projection without live region announcements`() {
        val timerStart = source.indexOf("private fun TypingAutoRatingTimerPanel(")
        val timerEnd = source.indexOf("private fun typingDecisionExplanation(", timerStart)
        val timer = source.substring(timerStart, timerEnd)

        assertTrue(timer.contains("typingTimerAccessibility("))
        assertTrue(timer.contains("typingProjectedRating(ratingLabel)"))
        assertTrue(timer.contains("val timerColor"))
        assertTrue(timer.contains("val ratingColor"))
        assertTrue(timer.contains("color = timerColor"))
        assertTrue(timer.contains("color = ratingColor"))
        assertTrue(timer.contains("speedLabel"))
        assertTrue(timer.contains("explanation"))
        assertFalse(timer.contains("liveRegion"))
    }

    @Test
    fun `typing rating dock contains only the four compact status segments`() {
        val dockStart = source.indexOf("private fun ReadOnlyRatingContextDock(")
        val dockEnd = source.indexOf("private fun LegacyReadOnlyRatingContextDock(", dockStart)
        val dock = source.substring(dockStart, dockEnd)

        assertTrue(dock.contains("resolveTypingRatingStatusPresentation"))
        assertTrue(dock.contains("segments.forEach"))
        assertTrue(dock.contains("statusLabel"))
        assertFalse(dock.contains("TypingAutomaticRatingInfoCard"))
        assertFalse(dock.contains("typingAutoRatingPrimary"))
        assertFalse(dock.contains("typingAutoRatingSecondary"))
        assertFalse(dock.contains("typingRatingStatusNote"))
    }

    @Test
    fun `success overlay shows a textual semantic previous to final rating transition`() {
        val start = source.indexOf("private fun TypingSuccessFocusOverlay(")
        val end = source.indexOf("private fun TypingEvaluationFeedback(", start)
        val overlay = source.substring(start, end)

        assertTrue(overlay.contains("previousLabel"))
        assertTrue(overlay.contains("finalLabel"))
        assertTrue(overlay.contains("text = \"→\""))
        assertTrue(overlay.contains("typingRatingTransitionAccessibility"))
        assertTrue(overlay.contains("typingDecisionExplanation"))
        assertTrue(overlay.contains("resolveTypingRatingPreviewColor"))
        assertFalse(overlay.contains("flow state", ignoreCase = true))
    }
}
