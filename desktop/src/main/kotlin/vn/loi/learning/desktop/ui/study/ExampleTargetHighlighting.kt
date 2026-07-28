package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

internal enum class ExampleTargetLanguage {
    ENGLISH,
    VIETNAMESE
}

internal data class ExampleTargetMatch(
    val start: Int,
    val endExclusive: Int
)

internal fun resolveExampleTargetMatches(
    text: String,
    target: String,
    language: ExampleTargetLanguage
): List<ExampleTargetMatch> {
    val normalizedTarget = target.trim()
    if (text.isEmpty() || normalizedTarget.isEmpty()) return emptyList()

    val options =
        if (language == ExampleTargetLanguage.ENGLISH) setOf(RegexOption.IGNORE_CASE)
        else emptySet()
    val expression = Regex(Regex.escape(normalizedTarget), options)
    return expression.findAll(text)
        .mapNotNull { match ->
            val start = match.range.first
            val endExclusive = match.range.last + 1
            if (text.hasSemanticBoundaryAt(start, endExclusive)) {
                ExampleTargetMatch(start, endExclusive)
            } else {
                null
            }
        }
        .toList()
}

internal fun highlightedExampleText(
    text: String,
    target: String,
    language: ExampleTargetLanguage,
    highlightStyle: SpanStyle
): AnnotatedString {
    val matches = resolveExampleTargetMatches(text, target, language)
    return buildAnnotatedString {
        append(text)
        matches.forEach { match ->
            addStyle(highlightStyle, match.start, match.endExclusive)
        }
    }
}

private fun String.hasSemanticBoundaryAt(start: Int, endExclusive: Int): Boolean {
    val startsAtBoundary = start == 0 || !this[start - 1].isSemanticWordCharacter()
    val endsAtBoundary = endExclusive == length || !this[endExclusive].isSemanticWordCharacter()
    return startsAtBoundary && endsAtBoundary
}

private fun Char.isSemanticWordCharacter(): Boolean = isLetterOrDigit() || this == '_'
