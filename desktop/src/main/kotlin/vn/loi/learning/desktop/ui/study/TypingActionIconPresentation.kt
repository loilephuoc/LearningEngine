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
            "Câu trả lời chưa đúng"
        )
        else -> TypingActionIconPresentation(
            TypingActionIconKind.NEUTRAL,
            "Kiểm tra hoặc hiển thị đáp án"
        )
    }
}
