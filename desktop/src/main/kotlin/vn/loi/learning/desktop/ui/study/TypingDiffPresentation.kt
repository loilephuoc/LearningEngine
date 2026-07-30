package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.TypingAnswerDifference
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingDifferenceKind

data class TypingLiveDiffPresentation(
    val correctPrefix: String,
    val incorrectRemainder: String,
    val missingCharacterAtBoundary: Boolean,
    val firstMismatchIndex: Int
)

data class TypingRevealComparisonPresentation(
    val userAnswer: String,
    val correctAnswer: String,
    val differences: List<TypingAnswerDifference>,
    val userAnswerLabel: String,
    val correctAnswerLabel: String
)

fun resolveTypingLiveDiff(
    input: String,
    evaluation: TypingAnswerEvaluation
): TypingLiveDiffPresentation? {
    if (evaluation.status != TypingAnswerEvaluationStatus.INCORRECT || input.isEmpty()) return null
    val points = input.codePoints().toArray()
    val prefixLength = evaluation.correctPrefixLength.coerceAtMost(points.size)
    return TypingLiveDiffPresentation(
        correctPrefix = points.take(prefixLength).toCodePointText(),
        incorrectRemainder = points.drop(prefixLength).toCodePointText(),
        missingCharacterAtBoundary =
            evaluation.differences.getOrNull(evaluation.firstMismatchIndex ?: -1)?.kind ==
                TypingDifferenceKind.DELETION,
        firstMismatchIndex = evaluation.firstMismatchIndex ?: prefixLength
    )
}

fun resolveTypingRevealComparison(
    evaluation: TypingAnswerEvaluation?,
    userAnswerLabel: String,
    correctAnswerLabel: String
): TypingRevealComparisonPresentation? =
    evaluation
        ?.takeIf { it.status == TypingAnswerEvaluationStatus.INCORRECT }
        ?.let {
            TypingRevealComparisonPresentation(
                userAnswer = it.originalAnswer,
                correctAnswer = it.originalExpectedAnswer,
                differences = it.differences,
                userAnswerLabel = userAnswerLabel,
                correctAnswerLabel = correctAnswerLabel
            )
        }

private fun List<Int>.toCodePointText(): String =
    buildString {
        this@toCodePointText.forEach { appendCodePoint(it) }
    }
