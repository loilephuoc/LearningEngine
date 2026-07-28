package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import java.text.Normalizer
import java.util.Locale

internal enum class ExampleTargetLanguage {
    ENGLISH,
    VIETNAMESE
}

internal data class ExampleTargetMatch(
    val start: Int,
    val endExclusive: Int
)

internal data class LearningKeyMatchingPolicy(
    val language: ExampleTargetLanguage,
    val allowEnglishInfinitiveSurface: Boolean = language == ExampleTargetLanguage.ENGLISH
)

/**
 * Pure matching authority. Normalized offsets are never exposed: every match is mapped back to
 * the exact UTF-16 range in the original display text.
 */
internal object LearningKeyMatcher {
    fun resolve(
        displayText: String,
        candidates: List<String>,
        policy: LearningKeyMatchingPolicy
    ): List<ExampleTargetMatch> {
        if (displayText.isEmpty()) return emptyList()
        val source = NormalizedTextIndex.create(displayText)
        val candidateIndexes = candidates
            .flatMap { candidate ->
                buildList {
                    add(candidate)
                    if (policy.allowEnglishInfinitiveSurface) {
                        val trimmed = candidate.trim()
                        if (trimmed.length > 3 && trimmed.startsWith("to ", ignoreCase = true)) {
                            add(trimmed.substring(3))
                        }
                    }
                }
            }
            .filter(String::isNotBlank)
            .map(NormalizedTextIndex::create)
            .filter { it.normalized.isNotBlank() }
            .distinctBy { it.normalized }
            .sortedByDescending { it.normalized.length }

        val possible = buildList {
            candidateIndexes.forEachIndexed { priority, candidate ->
                var from = 0
                while (from <= source.normalized.length - candidate.normalized.length) {
                    val index = source.normalized.indexOf(candidate.normalized, from)
                    if (index < 0) break
                    val end = index + candidate.normalized.length
                    if (source.hasLexicalBoundaries(index, end)) {
                        add(
                            PrioritizedMatch(
                                match = source.originalRange(index, end),
                                normalizedLength = candidate.normalized.length,
                                priority = priority
                            )
                        )
                    }
                    from = index + 1
                }
            }
        }

        return possible
            .sortedWith(
                compareByDescending<PrioritizedMatch> { it.normalizedLength }
                    .thenBy { it.priority }
                    .thenBy { it.match.start }
            )
            .fold(mutableListOf<ExampleTargetMatch>()) { accepted, candidate ->
                if (accepted.none { it.overlaps(candidate.match) }) accepted.add(candidate.match)
                accepted
            }
            .sortedBy { it.start }
    }
}

private data class PrioritizedMatch(
    val match: ExampleTargetMatch,
    val normalizedLength: Int,
    val priority: Int
)

private fun ExampleTargetMatch.overlaps(other: ExampleTargetMatch): Boolean =
    start < other.endExclusive && other.start < endExclusive

private data class NormalizedUnit(
    val value: String,
    val originalStart: Int,
    val originalEndExclusive: Int
)

internal class NormalizedTextIndex private constructor(
    val original: String,
    val normalized: String,
    private val originalStarts: IntArray,
    private val originalEnds: IntArray
) {
    fun originalRange(normalizedStart: Int, normalizedEndExclusive: Int): ExampleTargetMatch =
        ExampleTargetMatch(
            start = originalStarts[normalizedStart],
            endExclusive = originalEnds[normalizedEndExclusive - 1]
        )

    fun hasLexicalBoundaries(start: Int, endExclusive: Int): Boolean {
        val first = normalized[start]
        val last = normalized[endExclusive - 1]
        val before = normalized.getOrNull(start - 1)
        val after = normalized.getOrNull(endExclusive)
        return (!first.isLexical() || before == null || !before.isLexical()) &&
            (!last.isLexical() || after == null || !after.isLexical())
    }

    companion object {
        fun create(value: String): NormalizedTextIndex {
            val units = mutableListOf<NormalizedUnit>()
            var offset = 0
            while (offset < value.length) {
                val start = offset
                var codePoint = value.codePointAt(offset)
                offset += Character.charCount(codePoint)
                while (offset < value.length) {
                    codePoint = value.codePointAt(offset)
                    val type = Character.getType(codePoint)
                    if (
                        type != Character.NON_SPACING_MARK.toInt() &&
                        type != Character.COMBINING_SPACING_MARK.toInt() &&
                        type != Character.ENCLOSING_MARK.toInt()
                    ) break
                    offset += Character.charCount(codePoint)
                }
                val raw = value.substring(start, offset)
                val canonical = canonicalize(raw)
                canonical.forEach { character ->
                    units += NormalizedUnit(character.toString(), start, offset)
                }
            }

            val compact = mutableListOf<NormalizedUnit>()
            units.forEach { unit ->
                if (unit.value == " " && compact.lastOrNull()?.value == " ") {
                    val previous = compact.removeAt(compact.lastIndex)
                    compact += previous.copy(originalEndExclusive = unit.originalEndExclusive)
                } else {
                    compact += unit
                }
            }
            return NormalizedTextIndex(
                original = value,
                normalized = compact.joinToString("") { it.value },
                originalStarts = compact.map { it.originalStart }.toIntArray(),
                originalEnds = compact.map { it.originalEndExclusive }.toIntArray()
            )
        }

        private fun canonicalize(raw: String): String {
            val normalized = Normalizer.normalize(raw, Normalizer.Form.NFC)
                .lowercase(Locale.ROOT)
            return when {
                normalized.all(Char::isWhitespace) -> " "
                normalized in APOSTROPHES -> "'"
                normalized in HYPHENS -> "-"
                else -> normalized
            }
        }

        private val APOSTROPHES = setOf("'", "’", "‘", "`", "´", "ʼ", "＇")
        private val HYPHENS = setOf("-", "‐", "‑", "‒", "–", "—", "−")
    }
}

private fun Char.isLexical(): Boolean = isLetterOrDigit() || this == '_' || this == '\''

internal fun resolveExampleTargetMatches(
    text: String,
    target: String,
    language: ExampleTargetLanguage
): List<ExampleTargetMatch> =
    LearningKeyMatcher.resolve(text, listOf(target), LearningKeyMatchingPolicy(language))

internal fun highlightedExampleText(
    text: String,
    target: String,
    language: ExampleTargetLanguage,
    highlightStyle: SpanStyle
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        resolveExampleTargetMatches(text, target, language).forEach {
            addStyle(highlightStyle, it.start, it.endExclusive)
        }
    }
