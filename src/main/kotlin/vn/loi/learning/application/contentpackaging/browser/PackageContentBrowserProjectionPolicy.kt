package vn.loi.learning.application.contentpackaging.browser

import java.text.Normalizer
import java.util.Locale

/**
 * Pure Domain/Application policy để lọc, tìm kiếm và sắp xếp danh sách [PackageContentBrowserItem].
 *
 * Đảm bảo:
 * - Case-insensitive, Unicode-safe, trimmed search.
 * - Sắp xếp đinh ninh (deterministic sort).
 * - Hoàn toàn độc lập với Compose UI.
 */
object PackageContentBrowserProjectionPolicy {

    fun filterAndSort(
        items: List<PackageContentBrowserItem>,
        query: String,
        lessonFilter: String?,
        mediaFilter: BrowserMediaFilter,
        sortOption: BrowserSortOption
    ): List<PackageContentBrowserItem> {
        val trimmedQuery = query.trim()
        val normalizedQuery = if (trimmedQuery.isNotEmpty()) normalizeSearchText(trimmedQuery) else ""

        val filteredSequence = items.asSequence().filter { item ->
            // 1. Search matching
            if (normalizedQuery.isNotEmpty()) {
                if (!item.searchableText.contains(normalizedQuery)) {
                    return@filter false
                }
            }

            // 2. Lesson filter
            if (!lessonFilter.isNullOrBlank() && lessonFilter != "ALL") {
                if (item.lesson != lessonFilter) {
                    return@filter false
                }
            }

            // 3. Media filter
            when (mediaFilter) {
                BrowserMediaFilter.ALL -> true
                BrowserMediaFilter.HAS_IMAGE -> item.hasImage
                BrowserMediaFilter.MISSING_IMAGE -> !item.hasImage
                BrowserMediaFilter.HAS_AUDIO -> item.hasAudio
                BrowserMediaFilter.MISSING_AUDIO -> !item.hasAudio
            }
        }

        val comparator = when (sortOption) {
            BrowserSortOption.ORIGINAL_ORDER -> compareBy<PackageContentBrowserItem> { it.index }
            BrowserSortOption.QUESTION_ASC -> compareBy<PackageContentBrowserItem>(
                { it.questionText.lowercase(Locale.ROOT) },
                { it.index }
            )
            BrowserSortOption.LESSON_ASC -> compareBy<PackageContentBrowserItem>(
                { it.lesson.lowercase(Locale.ROOT) },
                { it.questionText.lowercase(Locale.ROOT) },
                { it.index }
            )
            BrowserSortOption.MEDIA_COMPLETENESS -> compareBy<PackageContentBrowserItem>(
                { mediaCompletenessScore(it) },
                { it.index }
            )
        }

        return filteredSequence.sortedWith(comparator).toList()
    }

    private fun mediaCompletenessScore(item: PackageContentBrowserItem): Int {
        // High score = missing media (sorted to end or top depending on score; here 0 = both, 1 = 1 media, 2 = no media)
        var missing = 0
        if (!item.hasImage) missing += 1
        if (!item.hasAudio) missing += 1
        return missing
    }

    internal fun buildSearchableText(
        questionText: String,
        answerText: String,
        pronunciation: String,
        partOfSpeech: String,
        lesson: String,
        group: String?,
        section: String?,
        tags: Set<String>
    ): String {
        val raw = StringBuilder()
            .append(questionText).append(' ')
            .append(answerText).append(' ')
            .append(pronunciation).append(' ')
            .append(partOfSpeech).append(' ')
            .append(lesson).append(' ')
            .append(group.orEmpty()).append(' ')
            .append(section.orEmpty()).append(' ')
            .append(tags.joinToString(" "))
            .toString()

        return normalizeSearchText(raw)
    }

    fun normalizeSearchText(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
        return normalized.lowercase(Locale.ROOT)
    }
}

object PackageContentBrowserSearchEnterPolicy {
    fun resolveTarget(
        filteredItems: List<PackageContentBrowserItem>,
        query: String
    ): PackageContentBrowserItem? {
        val normalizedQuery = PackageContentBrowserProjectionPolicy.normalizeSearchText(query.trim())
        if (normalizedQuery.isEmpty()) return null
        val exactQuestions = filteredItems.filter { item ->
            PackageContentBrowserProjectionPolicy.normalizeSearchText(item.questionText.trim()) == normalizedQuery
        }
        return when {
            exactQuestions.size == 1 -> exactQuestions.single()
            exactQuestions.size > 1 -> null
            filteredItems.size == 1 -> filteredItems.single()
            else -> null
        }
    }
}
