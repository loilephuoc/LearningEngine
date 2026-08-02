package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus

class TypingActionIconPresentationTest {
    @Test
    fun `icon follows realtime typing evaluation and revealed answer state`() {
        assertEquals(
            TypingActionIconKind.NEUTRAL,
            TypingActionIconPresentationResolver.resolve(TypingAnswerEvaluationStatus.EMPTY, false).kind
        )
        assertEquals(
            TypingActionIconKind.NEUTRAL,
            TypingActionIconPresentationResolver.resolve(TypingAnswerEvaluationStatus.VALID_PREFIX, false).kind
        )
        assertEquals(
            "Câu trả lời đang đúng hướng",
            TypingActionIconPresentationResolver.resolve(TypingAnswerEvaluationStatus.VALID_PREFIX, false)
                .accessibilityDescription
        )
        assertEquals(
            TypingActionIconKind.INCORRECT,
            TypingActionIconPresentationResolver.resolve(TypingAnswerEvaluationStatus.INCORRECT, false).kind
        )
        assertEquals(
            TypingActionIconKind.CORRECT,
            TypingActionIconPresentationResolver.resolve(TypingAnswerEvaluationStatus.CORRECT, false).kind
        )
        assertEquals(
            "Có ký tự chưa đúng",
            TypingActionIconPresentationResolver.resolve(TypingAnswerEvaluationStatus.INCORRECT, false)
                .accessibilityDescription
        )
        val revealed = TypingActionIconPresentationResolver.resolve(null, true)
        assertEquals(TypingActionIconKind.NEUTRAL, revealed.kind)
        assertEquals("Đáp án đã được hiển thị", revealed.accessibilityDescription)
    }

    @Test
    fun `typing line box preserves descender space at every viewport`() {
        StudyViewportClass.entries.forEach { viewport ->
            val input = TypingPresentationResolver.input(viewport)
            assertTrue(input.lineBoxSafetyInsetDp > 0)
            assertTrue(input.resolvedLineBoxMinimumHeightDp >= input.typedTextLineHeightSp + 8)
            assertTrue(input.typedTextLineHeightSp > input.typedTextFontSizeSp)
            assertTrue(input.placeholderLineHeightSp > input.placeholderFontSizeSp)
        }
    }
}
