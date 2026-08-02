package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus

enum class TypingActionIconKind { NEUTRAL, INCORRECT, CORRECT }

data class TypingActionIconPresentation(
    val kind: TypingActionIconKind,
    val accessibilityDescription: String
)

object TypingActionIconPresentationResolver {
    fun resolve(
        status: TypingAnswerEvaluationStatus?,
        answerRevealed: Boolean
    ): TypingActionIconPresentation = when {
        answerRevealed -> TypingActionIconPresentation(
            TypingActionIconKind.NEUTRAL,
            "Đáp án đã được hiển thị"
        )
        status == TypingAnswerEvaluationStatus.CORRECT -> TypingActionIconPresentation(
            TypingActionIconKind.CORRECT,
            "Câu trả lời đúng"
        )
        status == TypingAnswerEvaluationStatus.INCORRECT -> TypingActionIconPresentation(
            TypingActionIconKind.INCORRECT,
            "Có ký tự chưa đúng"
        )
        status == TypingAnswerEvaluationStatus.VALID_PREFIX -> TypingActionIconPresentation(
            TypingActionIconKind.NEUTRAL,
            "Câu trả lời đang đúng hướng"
        )
        else -> TypingActionIconPresentation(
            TypingActionIconKind.NEUTRAL,
            "Nhập câu trả lời"
        )
    }
}
