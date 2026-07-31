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
        val timerEnd = source.indexOf("private fun TypingRatingLegend(", timerStart)
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
                item.indexOf("TypingRecallInput(")
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
    fun `legend sits under timer and Again is localized as Reveal`() {
        val timerStart = source.indexOf("private fun TypingAutoRatingTimerPanel(")
        val timerEnd = source.indexOf("private data class TypingLegendEntry(", timerStart)
        val timer = source.substring(timerStart, timerEnd)

        assertTrue(timer.indexOf("formatTypingElapsed") < timer.indexOf("TypingRatingLegend("))
        assertTrue(timer.contains("workspaceStrings.typingLegendAgain"))
        assertFalse(StudyWorkspaceStrings.ENGLISH.typingLegendAgain.contains(">"))
        assertTrue(
            StudyWorkspaceStrings.ENGLISH.typingLegendAgain.contains(
                "Reveal",
                ignoreCase = true
            )
        )
    }

    @Test
    fun `timer semantics includes projection without live region announcements`() {
        val timerStart = source.indexOf("private fun TypingAutoRatingTimerPanel(")
        val timerEnd = source.indexOf("private fun TypingRatingLegend(", timerStart)
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
    fun `automatic rating card uses localized concise merged semantics`() {
        val cardStart = source.indexOf("private fun TypingAutomaticRatingInfoCard(")
        val cardEnd = source.indexOf("private fun LegacyReadOnlyRatingContextDock(", cardStart)
        val card = source.substring(cardStart, cardEnd)

        assertTrue(card.contains("LETheme.icons.Info"))
        assertTrue(card.contains("typingAutoRatingPrimary"))
        assertTrue(card.contains("typingAutoRatingSecondary"))
        assertTrue(card.contains("semantics(mergeDescendants = true)"))
        assertTrue(card.contains("textAlign"))
        assertFalse(card.contains("\"Your time will"))
        assertFalse(card.contains("\"No need to select"))
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
