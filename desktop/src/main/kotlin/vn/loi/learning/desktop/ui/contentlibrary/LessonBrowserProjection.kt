package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.containsSearchQuery

fun projectLessons(
    items: List<LessonBrowserItem>,
    query: String,
    filter: LessonBrowserFilter,
    sort: LessonBrowserSort
): List<LessonBrowserItem> {
    val filtered =
        items.filter { item ->
            filter.matches(item) &&
                listOfNotNull(
                    item.title,
                    item.type,
                    item.group,
                    item.section,
                    item.lesson,
                    item.primaryText,
                    item.translatedText
                ).any { searchableText ->
                    searchableText.containsSearchQuery(query)
                }
        }

    return when (sort) {
        LessonBrowserSort.PACKAGE_ORDER ->
            filtered

        LessonBrowserSort.TITLE ->
            filtered.sortedBy {
                it.title.lowercase()
            }

        LessonBrowserSort.TYPE ->
            filtered.sortedWith(
                compareBy(
                    { it.type.lowercase() },
                    { it.title.lowercase() }
                )
            )

        LessonBrowserSort.ITEM_COUNT ->
            filtered.sortedByDescending {
                it.learningItemCount
            }
    }
}
