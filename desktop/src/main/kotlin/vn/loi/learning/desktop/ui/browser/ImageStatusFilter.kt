package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentmedia.MediaReferencePolicy
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem

/**
 * Filter for identifying Content items based on their current image reference state or question duplicate state.
 * Strictly read-only derived presentation state.
 */
enum class ContentItemFilter(val label: String) {
    ALL("All"),
    MISSING_IMAGE("Missing Image"),
    DUPLICATE_IMAGE("Duplicate Image Filename"),
    DUPLICATE_QUESTION("Duplicate Question"),
    HAS_IMAGE("Has Image")
}

typealias ImageStatusFilter = ContentItemFilter

object ImageStatusProjectionPolicy {

    /**
     * Determines whether an image reference is considered missing according to canonical policy.
     * Includes null, blank, and canonical sentinel filenames (e.g. no_image.jpg, no_image.png).
     */
    fun isMissingImage(imageRef: String?): Boolean {
        if (imageRef.isNullOrBlank()) return true
        return MediaReferencePolicy.isNoImageSentinel(imageRef)
    }

    /**
     * Extracts the canonical filename key for duplicate comparison.
     * Whitespace and path separators are normalized, but extensions remain strictly distinct.
     */
    fun imageRefKey(imageRef: String): String =
        imageRef.trim().replace('\\', '/').substringAfterLast('/').lowercase()

    /**
     * Normalizes a question string for duplicate comparison: trimmed and lowercased.
     * Blank questions are not normalized into a valid duplicate key.
     */
    fun questionKey(questionText: String): String =
        questionText.trim().lowercase()

    /**
     * Computes the map of canonical image keys to their occurrence counts for keys appearing >= 2 times.
     * Missing/placeholder image references are strictly excluded before counting duplicates.
     */
    fun computeDuplicateImageCounts(items: List<PackageContentBrowserItem>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (item in items) {
            val ref = item.imageRef
            if (!isMissingImage(ref)) {
                val key = imageRefKey(ref!!)
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts.filterValues { it >= 2 }
    }

    /**
     * Computes the map of normalized question keys to their occurrence counts for keys appearing >= 2 times.
     * Blank questions are strictly excluded before counting duplicates.
     */
    fun computeDuplicateQuestionCounts(items: List<PackageContentBrowserItem>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (item in items) {
            val q = item.questionText
            if (q.isNotBlank()) {
                val key = questionKey(q)
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts.filterValues { it >= 2 }
    }

    /**
     * Groups duplicate items by canonical image key into [DuplicateImageGroup]s.
     * Invariants:
     * - Only groups with >= 2 items are produced.
     * - Ordering of groups is deterministic and follows the first occurrence in the original list.
     * - Ordering of items within each group strictly preserves original list/package index order.
     */
    fun computeDuplicateGroups(items: List<PackageContentBrowserItem>): List<DuplicateImageGroup> {
        val counts = computeDuplicateImageCounts(items)
        if (counts.isEmpty()) return emptyList()

        val groups = LinkedHashMap<String, MutableList<PackageContentBrowserItem>>()
        val firstRefMap = mutableMapOf<String, String>()

        for (item in items) {
            val ref = item.imageRef
            if (!isMissingImage(ref)) {
                val key = imageRefKey(ref!!)
                if (key in counts) {
                    groups.getOrPut(key) { mutableListOf() }.add(item)
                    if (key !in firstRefMap) {
                        firstRefMap[key] = ref
                    }
                }
            }
        }

        return groups.mapNotNull { (key, groupItems) ->
            if (groupItems.size >= 2) {
                DuplicateImageGroup(
                    imageKey = key,
                    imageRef = firstRefMap[key] ?: key,
                    items = groupItems
                )
            } else null
        }
    }

    /**
     * Computes the set of canonical image keys that appear in 2 or more items within the given package items.
     * Missing/placeholder image references are strictly excluded before counting duplicates.
     */
    fun computeDuplicateImageKeys(items: List<PackageContentBrowserItem>): Set<String> =
        computeDuplicateImageCounts(items).keys

    /**
     * Computes the set of normalized question keys that appear in 2 or more items within the given package items.
     * Blank questions are strictly excluded.
     */
    fun computeDuplicateQuestionKeys(items: List<PackageContentBrowserItem>): Set<String> =
        computeDuplicateQuestionCounts(items).keys

    /**
     * Filters a list of items by content item status.
     * Preserves original relative ordering and performs zero mutations.
     */
    fun filter(
        items: List<PackageContentBrowserItem>,
        filter: ContentItemFilter,
        duplicateImageKeys: Set<String>,
        duplicateQuestionKeys: Set<String> = emptySet()
    ): List<PackageContentBrowserItem> = when (filter) {
        ContentItemFilter.ALL -> items
        ContentItemFilter.MISSING_IMAGE -> items.filter { isMissingImage(it.imageRef) }
        ContentItemFilter.HAS_IMAGE -> items.filter { !isMissingImage(it.imageRef) }
        ContentItemFilter.DUPLICATE_IMAGE -> items.filter { item ->
            val ref = item.imageRef
            !isMissingImage(ref) && imageRefKey(ref!!) in duplicateImageKeys
        }
        ContentItemFilter.DUPLICATE_QUESTION -> items.filter { item ->
            item.questionText.isNotBlank() && questionKey(item.questionText) in duplicateQuestionKeys
        }
    }
}

/**
 * Presentation projection representing a group of content items sharing the same canonical image reference.
 */
data class DuplicateImageGroup(
    val imageKey: String,
    val imageRef: String,
    val items: List<PackageContentBrowserItem>
)
