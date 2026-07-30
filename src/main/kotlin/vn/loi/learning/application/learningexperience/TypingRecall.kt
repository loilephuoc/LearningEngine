package vn.loi.learning.application.learningexperience

import java.util.Locale
import java.text.Normalizer
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
    val normalizedExpectedAnswer: String,
    val originalAnswer: String,
    val originalExpectedAnswer: String,
    val differences: List<TypingAnswerDifference>
) {
    val isCorrect: Boolean
        get() = status == TypingAnswerEvaluationStatus.CORRECT

    val isCompletedAttempt: Boolean
        get() = status != TypingAnswerEvaluationStatus.EMPTY

    val firstMismatchIndex: Int?
        get() = differences.indexOfFirst { it.kind != TypingDifferenceKind.MATCH }
            .takeIf { it >= 0 }

    val correctPrefixLength: Int
        get() = differences.takeWhile { it.kind == TypingDifferenceKind.MATCH }.size
}

enum class TypingDifferenceKind {
    MATCH,
    REPLACEMENT,
    INSERTION,
    DELETION
}

data class TypingAnswerDifference(
    val kind: TypingDifferenceKind,
    val typedText: String?,
    val expectedText: String?
)

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
            normalizedExpectedAnswer = normalizedExpectedAnswer,
            originalAnswer = answer,
            originalExpectedAnswer = prompt.expectedAnswer,
            differences = align(normalizedExpectedAnswer, normalizedAnswer)
        )
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
            .trim()
            .replace(WHITESPACE, " ")
            .lowercase(Locale.ROOT)

    private fun align(expected: String, typed: String): List<TypingAnswerDifference> {
        val expectedPoints = expected.codePoints().toArray()
        val typedPoints = typed.codePoints().toArray()
        if (expected.startsWith(typed)) {
            return typedPoints.map { point ->
                TypingAnswerDifference(
                    TypingDifferenceKind.MATCH,
                    point.asText(),
                    point.asText()
                )
            } + expectedPoints.drop(typedPoints.size).map { point ->
                TypingAnswerDifference(
                    TypingDifferenceKind.DELETION,
                    typedText = null,
                    expectedText = point.asText()
                )
            }
        }
        if (typed.startsWith(expected)) {
            return expectedPoints.map { point ->
                TypingAnswerDifference(
                    TypingDifferenceKind.MATCH,
                    point.asText(),
                    point.asText()
                )
            } + typedPoints.drop(expectedPoints.size).map { point ->
                TypingAnswerDifference(
                    TypingDifferenceKind.INSERTION,
                    typedText = point.asText(),
                    expectedText = null
                )
            }
        }
        val costs = Array(expectedPoints.size + 1) { IntArray(typedPoints.size + 1) }
        for (i in expectedPoints.indices) costs[i + 1][0] = i + 1
        for (j in typedPoints.indices) costs[0][j + 1] = j + 1
        for (i in expectedPoints.indices) {
            for (j in typedPoints.indices) {
                val substitution =
                    costs[i][j] + if (expectedPoints[i] == typedPoints[j]) 0 else 1
                costs[i + 1][j + 1] =
                    minOf(substitution, costs[i][j + 1] + 1, costs[i + 1][j] + 1)
            }
        }

        val reversed = mutableListOf<TypingAnswerDifference>()
        var i = expectedPoints.size
        var j = typedPoints.size
        while (i > 0 || j > 0) {
            val diagonalCost =
                if (i > 0 && j > 0) {
                    costs[i - 1][j - 1] +
                        if (expectedPoints[i - 1] == typedPoints[j - 1]) 0 else 1
                } else {
                    Int.MAX_VALUE
                }
            when {
                i > 0 && j > 0 && costs[i][j] == diagonalCost -> {
                    val matches = expectedPoints[i - 1] == typedPoints[j - 1]
                    reversed +=
                        TypingAnswerDifference(
                            kind =
                                if (matches) TypingDifferenceKind.MATCH
                                else TypingDifferenceKind.REPLACEMENT,
                            typedText = typedPoints[j - 1].asText(),
                            expectedText = expectedPoints[i - 1].asText()
                        )
                    i--
                    j--
                }
                i > 0 && costs[i][j] == costs[i - 1][j] + 1 -> {
                    reversed +=
                        TypingAnswerDifference(
                            TypingDifferenceKind.DELETION,
                            typedText = null,
                            expectedText = expectedPoints[i - 1].asText()
                        )
                    i--
                }
                else -> {
                    reversed +=
                        TypingAnswerDifference(
                            TypingDifferenceKind.INSERTION,
                            typedText = typedPoints[j - 1].asText(),
                            expectedText = null
                        )
                    j--
                }
            }
        }
        return reversed.asReversed()
    }

    private fun Int.asText(): String = String(Character.toChars(this))

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
