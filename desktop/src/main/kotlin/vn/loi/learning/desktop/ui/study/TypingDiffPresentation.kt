package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
    val correctAnswerLabel: String,
    val differencesLabel: String
)

fun resolveTypingLiveDiff(
    input: String,
    evaluation: TypingAnswerEvaluation
): TypingLiveDiffPresentation? {
    if (evaluation.status != TypingAnswerEvaluationStatus.INCORRECT || input.isEmpty()) return null
    val points = input.codePoints().toArray()
    val prefixLength = evaluation.correctPrefixLength.coerceAtMost(points.size)
    val incorrectRemainder = points.drop(prefixLength).toCodePointText()
    if (incorrectRemainder.isEmpty()) return null
    return TypingLiveDiffPresentation(
        correctPrefix = points.take(prefixLength).toCodePointText(),
        incorrectRemainder = incorrectRemainder,
        missingCharacterAtBoundary =
            evaluation.differences.getOrNull(evaluation.firstMismatchIndex ?: -1)?.kind ==
                TypingDifferenceKind.DELETION,
        firstMismatchIndex = evaluation.firstMismatchIndex ?: prefixLength
    )
}

fun typingLiveDiffVisualTransformation(
    evaluation: TypingAnswerEvaluation,
    normalColor: Color,
    dangerColor: Color
): VisualTransformation =
    VisualTransformation { source ->
        val live = resolveTypingLiveDiff(source.text, evaluation)
        val transformed =
            if (live == null) {
                source
            } else {
                buildAnnotatedString {
                    withStyle(SpanStyle(color = normalColor)) {
                        append(live.correctPrefix)
                    }
                    withStyle(SpanStyle(color = dangerColor)) {
                        append(live.incorrectRemainder)
                    }
                }
            }
        TransformedText(transformed, OffsetMapping.Identity)
    }

fun resolveTypingRevealComparison(
    evaluation: TypingAnswerEvaluation?,
    userAnswerLabel: String,
    correctAnswerLabel: String,
    differencesLabel: String = "Differences"
): TypingRevealComparisonPresentation? =
    evaluation
        ?.takeIf { it.status != TypingAnswerEvaluationStatus.EMPTY && it.originalAnswer.isNotBlank() }
        ?.let {
            TypingRevealComparisonPresentation(
                userAnswer = it.originalAnswer,
                correctAnswer = it.originalExpectedAnswer,
                differences = it.differences,
                userAnswerLabel = userAnswerLabel,
                correctAnswerLabel = correctAnswerLabel,
                differencesLabel = differencesLabel
            )
        }

private fun List<Int>.toCodePointText(): String =
    buildString {
        this@toCodePointText.forEach { appendCodePoint(it) }
    }
