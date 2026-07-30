package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import vn.loi.learning.application.learningexperience.TypingAnswerDifference
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingDifferenceKind

data class TypingLiveMismatchSpan(
    val startCodePoint: Int,
    val endCodePoint: Int,
    val kind: TypingDifferenceKind
)

data class TypingLiveDiffPresentation(
    val input: String,
    val mismatchSpans: List<TypingLiveMismatchSpan>
) {
    val firstMismatchIndex: Int
        get() = mismatchSpans.minOf { it.startCodePoint }
}

data class TypingRevealComparisonPresentation(
    val userAnswer: String,
    val correctAnswer: String,
    val differences: List<TypingAnswerDifference>,
    val userMismatchSpans: List<TypingLiveMismatchSpan>,
    val expectedMismatchSpans: List<TypingLiveMismatchSpan>,
    val userAnswerLabel: String,
    val correctAnswerLabel: String,
    val differencesLabel: String
)

fun resolveTypingLiveDiff(
    input: String,
    evaluation: TypingAnswerEvaluation
): TypingLiveDiffPresentation? {
    if (evaluation.status != TypingAnswerEvaluationStatus.INCORRECT || input.isEmpty()) return null
    val rawPointCount = input.codePointCount(0, input.length)
    val normalizedPointCount =
        evaluation.normalizedAnswer.codePointCount(0, evaluation.normalizedAnswer.length)
    if (rawPointCount != normalizedPointCount) return null

    var typedIndex = 0
    val spans =
        buildList {
            evaluation.differences.forEach { difference ->
                when (difference.kind) {
                    TypingDifferenceKind.MATCH -> typedIndex++
                    TypingDifferenceKind.REPLACEMENT,
                    TypingDifferenceKind.INSERTION -> {
                        add(TypingLiveMismatchSpan(typedIndex, typedIndex + 1, difference.kind))
                        typedIndex++
                    }
                    TypingDifferenceKind.DELETION -> Unit
                }
            }
        }
    return spans.takeIf(List<*>::isNotEmpty)?.let {
        TypingLiveDiffPresentation(input = input, mismatchSpans = spans)
    }
}

fun typingLiveDiffVisualTransformation(
    evaluation: TypingAnswerEvaluation?,
    dangerColor: Color
): VisualTransformation =
    VisualTransformation { source ->
        val transformed = androidx.compose.ui.text.AnnotatedString.Builder(source)
        val currentEvaluation = evaluation
            ?: return@VisualTransformation TransformedText(source, OffsetMapping.Identity)
        resolveTypingLiveDiff(source.text, currentEvaluation)
            ?.mismatchSpans
            ?.forEach { span ->
                val start = source.text.offsetByCodePoints(0, span.startCodePoint)
                val end = source.text.offsetByCodePoints(0, span.endCodePoint)
                transformed.addStyle(SpanStyle(color = dangerColor), start, end)
            }
        TransformedText(transformed.toAnnotatedString(), OffsetMapping.Identity)
    }

fun resolveTypingRevealComparison(
    evaluation: TypingAnswerEvaluation?,
    userAnswerLabel: String,
    correctAnswerLabel: String,
    differencesLabel: String = "Differences"
): TypingRevealComparisonPresentation? =
    evaluation
        ?.takeIf {
            it.status == TypingAnswerEvaluationStatus.INCORRECT &&
                it.originalAnswer.isNotBlank()
        }
        ?.let { evaluation ->
            val userSpans = mutableListOf<TypingLiveMismatchSpan>()
            val expectedSpans = mutableListOf<TypingLiveMismatchSpan>()
            var userIndex = 0
            var expectedIndex = 0
            evaluation.differences.forEach { difference ->
                when (difference.kind) {
                    TypingDifferenceKind.MATCH -> {
                        userIndex++
                        expectedIndex++
                    }
                    TypingDifferenceKind.REPLACEMENT -> {
                        userSpans +=
                            TypingLiveMismatchSpan(userIndex, userIndex + 1, difference.kind)
                        expectedSpans +=
                            TypingLiveMismatchSpan(expectedIndex, expectedIndex + 1, difference.kind)
                        userIndex++
                        expectedIndex++
                    }
                    TypingDifferenceKind.INSERTION -> {
                        userSpans +=
                            TypingLiveMismatchSpan(userIndex, userIndex + 1, difference.kind)
                        userIndex++
                    }
                    TypingDifferenceKind.DELETION -> {
                        expectedSpans +=
                            TypingLiveMismatchSpan(expectedIndex, expectedIndex + 1, difference.kind)
                        expectedIndex++
                    }
                }
            }
            TypingRevealComparisonPresentation(
                userAnswer = evaluation.originalAnswer,
                correctAnswer = evaluation.originalExpectedAnswer,
                differences = evaluation.differences,
                userMismatchSpans =
                    userSpans.takeIf {
                        evaluation.originalAnswer.codePointCount(
                            0,
                            evaluation.originalAnswer.length
                        ) ==
                            evaluation.normalizedAnswer.codePointCount(
                                0,
                                evaluation.normalizedAnswer.length
                            )
                    }.orEmpty(),
                expectedMismatchSpans =
                    expectedSpans.takeIf {
                        evaluation.originalExpectedAnswer.codePointCount(
                            0,
                            evaluation.originalExpectedAnswer.length
                        ) ==
                            evaluation.normalizedExpectedAnswer.codePointCount(
                                    0,
                                    evaluation.normalizedExpectedAnswer.length
                                )
                    }.orEmpty(),
                userAnswerLabel = userAnswerLabel,
                correctAnswerLabel = correctAnswerLabel,
                differencesLabel = differencesLabel
            )
        }
