package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals

class LessonBrowserProjectionTest {
    private fun item(
        id: String,
        title: String,
        translation: String? = null,
        count: Int = 1
    ) = LessonBrowserItem(id, title, "vocab", null, null, null, title, translation, count)

    private val items =
        listOf(
            item("1", "Zulu", null, 2),
            item("2", "Alpha Lesson", "Dịch", 9)
        )

    @Test
    fun `searches by one term`() {
        assertEquals(
            "Alpha Lesson",
            projectLessons(items, "alpha", LessonBrowserFilter.ALL, LessonBrowserSort.PACKAGE_ORDER)
                .single()
                .title
        )
    }

    @Test
    fun `multi-term search requires all words in any order`() {
        assertEquals(
            "Alpha Lesson",
            projectLessons(items, "lesson alpha", LessonBrowserFilter.ALL, LessonBrowserSort.PACKAGE_ORDER)
                .single()
                .title
        )
        assertEquals(
            emptyList(),
            projectLessons(items, "alpha missing", LessonBrowserFilter.ALL, LessonBrowserSort.PACKAGE_ORDER)
        )
    }

    @Test
    fun `filters lessons with translations`() {
        assertEquals(
            "Alpha Lesson",
            projectLessons(items, "", LessonBrowserFilter.WITH_TRANSLATION, LessonBrowserSort.TITLE)
                .single()
                .title
        )
    }

    @Test
    fun `sorts by item count`() {
        assertEquals(
            "Alpha Lesson",
            projectLessons(items, "", LessonBrowserFilter.ALL, LessonBrowserSort.ITEM_COUNT)
                .first()
                .title
        )
    }
}
