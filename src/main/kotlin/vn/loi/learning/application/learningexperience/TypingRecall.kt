package vn.loi.learning.application.learningexperience

import java.util.Locale
import vn.loi.learning.application.learningcontent.LearningContent

data class TypingRecallPrompt(
    val expectedAnswer: String
) {
    init {
        require(expectedAnswer.isNotBlank()) {
            "A typing recall prompt requires a non-blank expected answer."
        }
    }
}

object TypingRecallPromptExtractor {
    fun extract(content: LearningContent?): TypingRecallPrompt? {
        val expectedAnswer = content
            ?.answer
            ?.textBlocks
            ?.map { block -> block.value.trim() }
            ?.filter(String::isNotBlank)
            ?.joinToString("\n")
            .orEmpty()

        return expectedAnswer
            .takeIf(String::isNotBlank)
            ?.let(::TypingRecallPrompt)
    }
}

enum class TypingAnswerEvaluationStatus {
    CORRECT,
    INCORRECT,
    EMPTY
}

data class TypingAnswerEvaluation(
    val status: TypingAnswerEvaluationStatus,
    val normalizedAnswer: String,
    val normalizedExpectedAnswer: String
) {
    val isCorrect: Boolean
        get() = status == TypingAnswerEvaluationStatus.CORRECT

    val isCompletedAttempt: Boolean
        get() = status != TypingAnswerEvaluationStatus.EMPTY
}

class TypingAnswerEvaluator {
    fun evaluate(
        prompt: TypingRecallPrompt,
        answer: String
    ): TypingAnswerEvaluation {
        val normalizedAnswer = normalize(answer)
        val normalizedExpectedAnswer = normalize(prompt.expectedAnswer)
        val status = when {
            normalizedAnswer.isEmpty() -> TypingAnswerEvaluationStatus.EMPTY
            normalizedAnswer == normalizedExpectedAnswer -> TypingAnswerEvaluationStatus.CORRECT
            else -> TypingAnswerEvaluationStatus.INCORRECT
        }
        return TypingAnswerEvaluation(
            status = status,
            normalizedAnswer = normalizedAnswer,
            normalizedExpectedAnswer = normalizedExpectedAnswer
        )
    }

    private fun normalize(value: String): String =
        value
            .trim()
            .replace(WHITESPACE, " ")
            .lowercase(Locale.ROOT)

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
