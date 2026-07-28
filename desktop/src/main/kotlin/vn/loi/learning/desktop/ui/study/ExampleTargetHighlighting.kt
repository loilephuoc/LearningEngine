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
    if (text.isEmpty() || target.isBlank()) return emptyList()

    val rawTarget = target.trim()
    val cleanedTarget = rawTarget.trimPunctuation()
    val candidates = listOf(rawTarget, cleanedTarget)
        .filter { it.isNotBlank() }
        .distinct()

    val matches = mutableListOf<ExampleTargetMatch>()
    for (candidate in candidates) {
        val found = findMatchesForCandidate(text, candidate)
        matches.addAll(found)
    }

    return mergeAndDeduplicateMatches(matches)
}

private fun findMatchesForCandidate(text: String, candidate: String): List<ExampleTargetMatch> {
    val options = setOf(RegexOption.IGNORE_CASE)
    val expression = Regex(Regex.escape(candidate), options)
    val directMatches = expression.findAll(text).mapNotNull { match ->
        val start = match.range.first
        val endExclusive = match.range.last + 1
        if (text.hasSemanticBoundaryAt(start, endExclusive)) {
            ExampleTargetMatch(start, endExclusive)
        } else {
            null
        }
    }.toList()

    if (directMatches.isNotEmpty()) {
        return directMatches
    }

    // Try common inflections ('s, s, ed, ing, es) at semantic boundary
    val inflections = listOf("'s", "s", "ed", "ing", "es")
    for (suffix in inflections) {
        val inflectedRegex = Regex(Regex.escape(candidate) + Regex.escape(suffix), options)
        val inflectedMatches = inflectedRegex.findAll(text).mapNotNull { match ->
            val start = match.range.first
            val endExclusive = match.range.last + 1
            if (text.hasSemanticBoundaryAt(start, endExclusive)) {
                ExampleTargetMatch(start, endExclusive)
            } else {
                null
            }
        }.toList()
        if (inflectedMatches.isNotEmpty()) {
            return inflectedMatches
        }
    }

    return emptyList()
}

private fun mergeAndDeduplicateMatches(matches: List<ExampleTargetMatch>): List<ExampleTargetMatch> {
    if (matches.isEmpty()) return emptyList()
    val sorted = matches.sortedWith(compareBy({ it.start }, { -it.endExclusive }))
    val result = mutableListOf<ExampleTargetMatch>()
    var current = sorted.first()
    for (i in 1 until sorted.size) {
        val next = sorted[i]
        if (next.start < current.endExclusive) {
            if (next.endExclusive > current.endExclusive) {
                current = ExampleTargetMatch(current.start, next.endExclusive)
            }
        } else {
            result.add(current)
            current = next
        }
    }
    result.add(current)
    return result
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

private fun String.trimPunctuation(): String {
    var start = 0
    var end = length - 1
    while (start <= end && !this[start].isSemanticWordCharacter()) {
        start++
    }
    while (end >= start && !this[end].isSemanticWordCharacter()) {
        end--
    }
    return if (start <= end) substring(start, end + 1) else ""
}

private fun String.hasSemanticBoundaryAt(start: Int, endExclusive: Int): Boolean {
    val startsAtBoundary = start == 0 || !this[start - 1].isSemanticWordCharacter()
    val endsAtBoundary = endExclusive == length || !this[endExclusive].isSemanticWordCharacter()
    return startsAtBoundary && endsAtBoundary
}

private fun Char.isSemanticWordCharacter(): Boolean = isLetterOrDigit() || this == '_'
