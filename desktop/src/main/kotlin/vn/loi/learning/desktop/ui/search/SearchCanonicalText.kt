package vn.loi.learning.desktop.ui.search

import java.text.Normalizer

internal data class SearchCanonicalText(
    val value: String,
    private val originalStarts: IntArray,
    private val originalEnds: IntArray
) {
    init {
        require(value.length == originalStarts.size)
        require(value.length == originalEnds.size)
    }

    fun findRanges(term: String): List<SearchMatchRange> {
        if (term.isEmpty() || value.isEmpty()) return emptyList()

        return buildList {
            var startIndex = 0
            while (startIndex <= value.length - term.length) {
                val matchIndex =
                    value.indexOf(
                        string = term,
                        startIndex = startIndex,
                        ignoreCase = true
                    )

                if (matchIndex < 0) break

                val lastIndex =
                    matchIndex + term.length - 1

                add(
                    SearchMatchRange(
                        start = originalStarts[matchIndex],
                        endExclusive = originalEnds[lastIndex]
                    )
                )

                startIndex =
                    matchIndex + term.length
            }
        }
    }
}

internal fun canonicalizeSearchText(
    text: String
): SearchCanonicalText {
    if (text.isEmpty()) {
        return SearchCanonicalText(
            value = "",
            originalStarts = IntArray(0),
            originalEnds = IntArray(0)
        )
    }

    val value = StringBuilder(text.length)
    val starts = mutableListOf<Int>()
    val ends = mutableListOf<Int>()

    var index = 0
    while (index < text.length) {
        val clusterStart = index

        index +=
            Character.charCount(
                text.codePointAt(index)
            )

        while (
            index < text.length &&
            text.codePointAt(index).isCombiningMark()
        ) {
            index +=
                Character.charCount(
                    text.codePointAt(index)
                )
        }

        val clusterEnd = index
        val normalized =
            Normalizer.normalize(
                text.substring(
                    startIndex = clusterStart,
                    endIndex = clusterEnd
                ),
                Normalizer.Form.NFKC
            )

        normalized.forEach { character ->
            value.append(character)
            starts += clusterStart
            ends += clusterEnd
        }
    }

    return SearchCanonicalText(
        value = value.toString(),
        originalStarts = starts.toIntArray(),
        originalEnds = ends.toIntArray()
    )
}

private fun Int.isCombiningMark(): Boolean =
    when (Character.getType(this)) {
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt() -> true
        else -> false
    }
