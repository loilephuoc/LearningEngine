package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.LightLEColors
import vn.loi.learning.domain.study.memory.model.ReviewRating

class SchedulerFeedbackPresentationTest {
    @Test
    fun `active answer is a bounded quiet explanatory presentation`() {
        val presentation =
            SchedulerFeedbackPresentationResolver.resolve(SchedulerFeedbackContext.ACTIVE_ANSWER)

        assertEquals(SchedulerFeedbackEmphasis.EXPLANATORY, presentation.emphasis)
        assertEquals(SchedulerFeedbackPlacement.INLINE_DECISION, presentation.placement)
        assertEquals(StudyContentTone.SECONDARY, presentation.contentTone)
        assertTrue(presentation.preservesSemanticRatingIdentity)
        assertTrue(presentation.detailsAvailable)
        assertTrue(presentation.allowsCompactWrap)
        assertEquals(640, presentation.maximumContentWidthDp)
        assertFalse(presentation.emphasis == SchedulerFeedbackEmphasis.CONSEQUENCE)
    }

    @Test
    fun `completion and continuity retain consequence contexts`() {
        val completion =
            SchedulerFeedbackPresentationResolver.resolve(SchedulerFeedbackContext.COMPLETION)
        val continuity =
            SchedulerFeedbackPresentationResolver.resolve(SchedulerFeedbackContext.CONTINUITY)

        assertEquals(SchedulerFeedbackEmphasis.CONSEQUENCE, completion.emphasis)
        assertEquals(SchedulerFeedbackPlacement.CONTAINED, completion.placement)
        assertEquals(SchedulerFeedbackEmphasis.CONSEQUENCE, continuity.emphasis)
        assertEquals(SchedulerFeedbackPlacement.OVERLAY, continuity.placement)
        assertNull(completion.maximumContentWidthDp)
        assertNull(continuity.maximumContentWidthDp)
    }

    @Test
    fun `resolver is deterministic and independent of display text`() {
        SchedulerFeedbackContext.values().forEach { context ->
            assertEquals(
                SchedulerFeedbackPresentationResolver.resolve(context),
                SchedulerFeedbackPresentationResolver.resolve(context)
            )
        }
        val resolver =
            source("CompactSchedulerFeedback.kt")
                .substringBefore("@Composable\nfun CompactSchedulerFeedback")
        assertFalse(resolver.contains("feedback.rating"))
        assertFalse(resolver.contains("scheduledInterval"))
    }

    @Test
    fun `consumers select explicit contexts and details remain discoverable`() {
        val answer = source("FocusedAnswerSurface.kt")
        val completion = source("SessionCompletionCard.kt")
        val screen = source("StudyScreen.kt")
        val component = source("CompactSchedulerFeedback.kt")

        assertTrue(answer.containsCodeIgnoringWhitespace("context = SchedulerFeedbackContext.ACTIVE_ANSWER"))
        assertTrue(completion.contains("context = SchedulerFeedbackContext.COMPLETION"))
        assertFalse(screen.contains("context = SchedulerFeedbackContext.CONTINUITY"))
        assertTrue(screen.contains("StudySessionTransitionPhase.RESULT_SHOWN"))
        assertTrue(component.contains("onClick = { isExpanded = !isExpanded }"))
        assertTrue(component.contains("Xem chi tiết"))
        assertTrue(component.contains("accessibility.conciseSummary"))
    }

    @Test
    fun `active answer stays bounded and compact text can wrap without placeholder data`() {
        val component = source("CompactSchedulerFeedback.kt")

        assertTrue(component.contains("Modifier.widthIn(max = presentation.maximumContentWidthDp.dp)"))
        assertTrue(component.contains("if (presentation.allowsCompactWrap) Modifier.weight(1f)"))
        assertFalse(component.contains("StudySchedulerFeedback("))
    }

    @Test
    fun `theme hierarchy preserves token typography and avoids raw color`() {
        val component = source("CompactSchedulerFeedback.kt")

        assertTrue(component.contains("LETheme.colors"))
        assertTrue(component.contains("LETheme.typography.statusText"))
        assertTrue(component.contains("MaterialTheme.typography.titleMedium"))
        assertFalse(component.contains("Color(0x"))
        assertFalse(component.contains("fontSize ="))
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            assertEquals(colors.dangerText, resolveSchedulerRatingIdentityColor(ReviewRating.AGAIN, colors))
            assertEquals(colors.warningText, resolveSchedulerRatingIdentityColor(ReviewRating.HARD, colors))
            assertEquals(colors.successText, resolveSchedulerRatingIdentityColor(ReviewRating.GOOD, colors))
            assertEquals(colors.info, resolveSchedulerRatingIdentityColor(ReviewRating.EASY, colors))
        }
    }

    private fun source(name: String): String =
        Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        )
}
