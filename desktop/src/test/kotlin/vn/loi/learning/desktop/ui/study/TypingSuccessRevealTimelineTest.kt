package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class TypingSuccessRevealTimelineTest {
    @Test
    fun `complete timeline follows icon answer translation lexical result order`() {
        val timeline = TypingSuccessRevealTimelineResolver.resolve(true, true)
        assertEquals(
            listOf(
                TypingSuccessRevealStage.ICON,
                TypingSuccessRevealStage.ANSWER,
                TypingSuccessRevealStage.TRANSLATION,
                TypingSuccessRevealStage.LEXICAL_METADATA,
                TypingSuccessRevealStage.RESULT
            ),
            timeline.segments.map(TypingSuccessRevealSegment::stage)
        )
    }

    @Test
    fun `all visual content completes within 280 milliseconds`() {
        assertEquals(265, TypingSuccessRevealTimelineResolver.resolve(true, true).completesAtMillis)
        assertTrue(TypingSuccessRevealTimelineResolver.resolve(true, true).completesAtMillis <= 280)
    }

    @Test
    fun `missing translation removes its stage and compresses subsequent stages`() {
        val full = TypingSuccessRevealTimelineResolver.resolve(true, true)
        val collapsed = TypingSuccessRevealTimelineResolver.resolve(false, true)
        assertNull(collapsed.segments.singleOrNull { it.stage == TypingSuccessRevealStage.TRANSLATION })
        assertTrue(collapsed.segment(TypingSuccessRevealStage.LEXICAL_METADATA).startsAtMillis <
            full.segment(TypingSuccessRevealStage.LEXICAL_METADATA).startsAtMillis)
    }

    @Test
    fun `missing lexical metadata removes its stage and compresses result`() {
        val full = TypingSuccessRevealTimelineResolver.resolve(true, true)
        val collapsed = TypingSuccessRevealTimelineResolver.resolve(true, false)
        assertNull(collapsed.segments.singleOrNull { it.stage == TypingSuccessRevealStage.LEXICAL_METADATA })
        assertTrue(collapsed.segment(TypingSuccessRevealStage.RESULT).startsAtMillis <
            full.segment(TypingSuccessRevealStage.RESULT).startsAtMillis)
    }

    @Test
    fun `hold and no-audio lifecycle remain within agreed bounds`() {
        assertTrue(TypingSuccessLifecyclePolicy.VISUAL_HOLD_MILLIS in 550L..700L)
        assertEquals(
            TypingSuccessLifecyclePolicy.TARGET_TOTAL_MILLIS,
            TypingSuccessRevealTimelineResolver.resolve(true, true).completesAtMillis +
                TypingSuccessLifecyclePolicy.VISUAL_HOLD_MILLIS
        )
        assertTrue(TypingSuccessLifecyclePolicy.TARGET_TOTAL_MILLIS in 750L..980L)
    }

    @Test
    fun `audio completion consumes elapsed time without creating a second full dwell`() {
        assertEquals(865L, TypingSuccessLifecyclePolicy.remainingDwellMillis(0L))
        assertEquals(265L, TypingSuccessLifecyclePolicy.remainingDwellMillis(600L))
        assertEquals(0L, TypingSuccessLifecyclePolicy.remainingDwellMillis(900L))
    }

    @Test
    fun `reveal visual uses bounded fade movement and icon scale`() {
        val segment = TypingSuccessRevealTimelineResolver.resolve(true, true)
            .segment(TypingSuccessRevealStage.ICON)
        val initial = resolveTypingSuccessRevealVisual(segment, 0f)
        val final = resolveTypingSuccessRevealVisual(segment, 100f)
        assertEquals(0f, initial.alpha)
        assertEquals(0.88f, initial.scale)
        assertTrue(initial.upwardOffsetDp <= 3f)
        assertEquals(1f, final.alpha)
        assertEquals(1f, final.scale)
        assertEquals(0f, final.upwardOffsetDp)
    }

    @Test
    fun `overlay reserves final layout and uses one cancellable animation authority`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
        ).substringAfter("private fun TypingSuccessFocusOverlay(")
            .substringBefore("private fun PopupLexicalMetadataRow(")
        assertEquals(1, Regex("LaunchedEffect\\(revealTimeline\\)").findAll(source).count())
        assertEquals(1, Regex("Animatable\\(0f\\)").findAll(source).count())
        assertFalse(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("graphicsLayer"))
        assertTrue(source.contains("easing = LinearEasing"))
        assertTrue(source.contains("contentDescription ="))
    }

    @Test
    fun `translation is prominent while IPA and POS remain one row`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
        ).substringAfter("private fun TypingSuccessFocusOverlay(")
            .substringBefore("private fun ReviewRating.toColorRole")
        assertTrue(source.contains("MaterialTheme.typography.titleLarge"))
        assertTrue(source.contains("FontWeight.SemiBold"))
        assertTrue(source.contains("color = LETheme.colors.accentPrimary"))
        assertTrue(source.contains("private fun PopupLexicalMetadataRow("))
        assertTrue(source.contains("Row("))
        assertTrue(source.contains("heightMode == StudyHeightMode.COMFORTABLE"))
    }
}
