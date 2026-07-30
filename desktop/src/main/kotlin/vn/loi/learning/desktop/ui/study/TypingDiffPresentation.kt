package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
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
    val accessibilityDescription: String
)

internal fun typingComparisonForCanonicalWord(
    comparison: TypingRevealComparisonPresentation?,
    canonicalWord: String
): TypingRevealComparisonPresentation? =
    comparison?.takeIf { it.correctAnswer == canonicalWord }

fun resolvePositionalTypingLiveDiff(
    input: String,
    evaluation: TypingAnswerEvaluation
): TypingLiveDiffPresentation? {
    if (evaluation.status != TypingAnswerEvaluationStatus.INCORRECT || input.isEmpty()) return null
    val rawPointCount = input.codePointCount(0, input.length)
    val normalizedPointCount =
        evaluation.normalizedAnswer.codePointCount(0, evaluation.normalizedAnswer.length)
    if (rawPointCount != normalizedPointCount) return null

    val typedPoints = evaluation.normalizedAnswer.codePoints().toArray()
    val expectedPoints = evaluation.normalizedExpectedAnswer.codePoints().toArray()
    val spans =
        buildList {
            typedPoints.forEachIndexed { index, typedPoint ->
                val kind =
                    when {
                        index >= expectedPoints.size -> TypingDifferenceKind.INSERTION
                        typedPoint != expectedPoints[index] -> TypingDifferenceKind.REPLACEMENT
                        else -> null
                    }
                if (kind != null) {
                    add(TypingLiveMismatchSpan(index, index + 1, kind))
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
        resolvePositionalTypingLiveDiff(source.text, currentEvaluation)
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
    correctAnswerLabel: String
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
                accessibilityDescription =
                    "$userAnswerLabel: ${evaluation.originalAnswer}. " +
                        "$correctAnswerLabel: ${evaluation.originalExpectedAnswer}. " +
                        typingDifferenceAccessibilityText(evaluation.differences)
            )
        }

internal fun typingComparisonAnnotatedText(
    text: String,
    spans: List<TypingLiveMismatchSpan>,
    color: Color
) =
    buildAnnotatedString {
        append(text)
        spans.forEach { span ->
            val start = text.offsetByCodePoints(0, span.startCodePoint)
            val end = text.offsetByCodePoints(0, span.endCodePoint)
            addStyle(
                SpanStyle(
                    color = color,
                    textDecoration =
                        if (span.kind == TypingDifferenceKind.INSERTION) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.Underline
                        }
                ),
                start,
                end
            )
        }
    }

internal fun typingDifferenceAccessibilityText(
    differences: List<TypingAnswerDifference>
): String =
    differences.fold(mutableListOf<TypingAnswerDifference>()) { groups, difference ->
        if (difference.kind == TypingDifferenceKind.MATCH) {
            groups
        } else {
            val previous = groups.lastOrNull()
            if (previous?.kind == difference.kind) {
                groups[groups.lastIndex] =
                    previous.copy(
                        typedText =
                            previous.typedText.orEmpty() + difference.typedText.orEmpty(),
                        expectedText =
                            previous.expectedText.orEmpty() + difference.expectedText.orEmpty()
                    )
            } else {
                groups += difference
            }
            groups
        }
    }
        .joinToString(" ") { difference ->
            when (difference.kind) {
                TypingDifferenceKind.MATCH -> error("Matches were filtered")
                TypingDifferenceKind.REPLACEMENT ->
                    "Replace ${difference.typedText.orEmpty().trim()} with " +
                        "${difference.expectedText.orEmpty().trim()}."
                TypingDifferenceKind.INSERTION ->
                    "Remove inserted ${difference.typedText.orEmpty().trim()}."
                TypingDifferenceKind.DELETION ->
                    "Missing ${difference.expectedText.orEmpty().trim()}."
            }
        }
