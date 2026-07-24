package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.library.model.InstalledPackageId

class LessonBrowserProductCompletionTest {

    private fun createSampleItem(
        id: String,
        title: String,
        group: String? = "English",
        section: String? = "Unit 1",
        lesson: String? = "Lesson 1",
        primaryText: String = title,
        translatedText: String? = "Translation",
        learningItemCount: Int = 5
    ) = LessonBrowserItem(
        id = id,
        title = title,
        type = "SENTENCE",
        group = group,
        section = section,
        lesson = lesson,
        primaryText = primaryText,
        translatedText = translatedText,
        learningItemCount = learningItemCount
    )

    // T1 — Package Context
    @Test
    fun `T1 package context is correctly preserved in LessonBrowserUiState`() {
        val pkgId = InstalledPackageId("pkg-topic-a")
        val item1 = createSampleItem("c1", "Greeting", learningItemCount = 5)
        val item2 = createSampleItem("c2", "Farewell", learningItemCount = 3)

        val state = LessonBrowserUiState(
            libraryId = pkgId.value,
            libraryName = "Topic Alpha",
            installedPackageId = pkgId,
            lessons = listOf(item1, item2)
        )

        assertEquals(pkgId, state.installedPackageId)
        assertEquals("Topic Alpha", state.libraryName)
        assertEquals(2, state.lessonCount)
        assertEquals(8, state.totalLearningItemCount)
        assertFalse(state.isEmpty)
    }

    // T2 — Hierarchy
    @Test
    fun `T2 hierarchical grouping handles blank and null metadata with fallback labels General and Other Lessons`() {
        val item1 = createSampleItem("c1", "Normal Item", group = "English", section = "Unit 1")
        val item2 = createSampleItem("c2", "Blank Group Item", group = "   ", section = "")
        val item3 = createSampleItem("c3", "Null Metadata Item", group = null, section = null)

        val groups = groupLessonsHierarchically(listOf(item1, item2, item3))

        assertEquals(2, groups.size)
        // Group 1: English
        assertEquals("English", groups[0].name)
        assertEquals(1, groups[0].sections.size)
        assertEquals("Unit 1", groups[0].sections[0].name)

        // Group 2: General (fallback)
        assertEquals("General", groups[1].name)
        assertEquals(1, groups[1].sections.size)
        assertEquals("Other Lessons", groups[1].sections[0].name)
        assertEquals(2, groups[1].sections[0].lessons.size)
    }

    // T3 — Search title
    @Test
    fun `T3 search matches lesson title case-insensitively`() {
        val item1 = createSampleItem("c1", "Basic Greetings")
        val item2 = createSampleItem("c2", "Advanced Grammar")
        val items = listOf(item1, item2)

        val filtered = projectLessons(items, query = "gReEt", filter = LessonBrowserFilter.ALL, sort = LessonBrowserSort.PACKAGE_ORDER)

        assertEquals(1, filtered.size)
        assertEquals("c1", filtered.first().id)
    }

    // T4 — Search translated text
    @Test
    fun `T4 search matches translated text`() {
        val item1 = createSampleItem("c1", "Hello", translatedText = "Xin chao tat ca")
        val item2 = createSampleItem("c2", "Goodbye", translatedText = "Tam biet")
        val items = listOf(item1, item2)

        val filtered = projectLessons(items, query = "tat ca", filter = LessonBrowserFilter.ALL, sort = LessonBrowserSort.PACKAGE_ORDER)

        assertEquals(1, filtered.size)
        assertEquals("c1", filtered.first().id)
    }

    // T5 — Search isolation
    @Test
    fun `T5 search in Topic A does not match or return Topic B content`() {
        val itemA = createSampleItem("cnt-a-1", "Topic A Lesson", primaryText = "Unique A Phrase")
        val itemsA = listOf(itemA)

        // Searching for Topic B content in Topic A items returns empty
        val filtered = projectLessons(itemsA, query = "Topic B Unique Text", filter = LessonBrowserFilter.ALL, sort = LessonBrowserSort.PACKAGE_ORDER)

        assertTrue(filtered.isEmpty())
    }

    // T6 — Search empty
    @Test
    fun `T6 non-matching search results in empty visibleLessons without setting package empty or error`() {
        val item1 = createSampleItem("c1", "Lesson One")
        val state = LessonBrowserUiState(
            libraryName = "Topic A",
            lessons = listOf(item1),
            query = "non-matching-xyz",
            appliedQuery = "non-matching-xyz"
        )

        assertFalse(state.isEmpty, "Package itself must not be marked empty")
        assertTrue(state.visibleLessons.isEmpty(), "visibleLessons must be empty for non-matching query")
        assertNull(state.selectedLessonInView, "selectedLessonInView must be null when query excludes selected lesson")
    }

    // T7 — Selection
    @Test
    fun `T7 clicking lesson selects single item and updates selectedLessonId`() {
        val item1 = createSampleItem("c1", "Lesson 1")
        val item2 = createSampleItem("c2", "Lesson 2")
        var state = LessonBrowserUiState(lessons = listOf(item1, item2))

        assertNull(state.selectedLessonId)

        state = state.select("c1")
        assertEquals("c1", state.selectedLessonId)
        assertEquals("c1", state.selectedLesson?.id)

        state = state.select("c2")
        assertEquals("c2", state.selectedLessonId)
        assertEquals("c2", state.selectedLesson?.id)
    }

    // T8 — Start disabled when no lesson selected
    @Test
    fun `T8 isStartEnabled is false when no lesson is selected`() {
        val item1 = createSampleItem("c1", "Lesson 1", learningItemCount = 5)
        val state = LessonBrowserUiState(lessons = listOf(item1), selectedLessonId = null)

        assertFalse(state.isStartEnabled)
        assertNull(state.selectedLessonInView)
    }

    // T9 — Start selected lesson
    @Test
    fun `T9 isStartEnabled is true when valid lesson with learning items is selected`() {
        val pkgId = InstalledPackageId("pkg-1")
        val item1 = createSampleItem("c1", "Lesson 1", learningItemCount = 5)
        val state = LessonBrowserUiState(
            installedPackageId = pkgId,
            libraryName = "Topic Alpha",
            lessons = listOf(item1),
            selectedLessonId = "c1"
        )

        assertTrue(state.isStartEnabled)
        val selectedInView = state.selectedLessonInView
        assertNotNull(selectedInView)

        val selection = PackageLessonSelection(
            installedPackageId = pkgId,
            lessonId = selectedInView.id,
            packageName = state.libraryName,
            lessonTitle = selectedInView.title
        )

        assertEquals(pkgId, selection.installedPackageId)
        assertEquals("c1", selection.lessonId)
        assertEquals("Topic Alpha", selection.packageName)
        assertEquals("Lesson 1", selection.lessonTitle)
    }

    // T10 — Zero learning items
    @Test
    fun `T10 isStartEnabled is false when selected lesson has zero learning items`() {
        val itemZero = createSampleItem("c-zero", "Empty Lesson", learningItemCount = 0)
        val state = LessonBrowserUiState(lessons = listOf(itemZero), selectedLessonId = "c-zero")

        assertNotNull(state.selectedLessonInView)
        assertEquals(0, state.selectedLessonInView?.learningItemCount)
        assertFalse(state.isStartEnabled, "Start Lesson must be disabled for lesson with 0 learning items")
    }

    // T11 — Back navigation
    @Test
    fun `T11 clearSelection resets selectedLessonId without mutating package state`() {
        val item1 = createSampleItem("c1", "Lesson 1")
        val state = LessonBrowserUiState(lessons = listOf(item1), selectedLessonId = "c1")

        val cleared = state.clearSelection()
        assertNull(cleared.selectedLessonId)
        assertEquals(1, cleared.lessonCount)
    }

    // T12 — Stale state isolation
    @Test
    fun `T12 new package load produces fresh state with empty query and null selection`() {
        val pkgIdA = InstalledPackageId("pkg-a")
        val stateA = LessonBrowserUiState(
            installedPackageId = pkgIdA,
            libraryName = "Topic A",
            lessons = listOf(createSampleItem("c-a", "Lesson A")),
            selectedLessonId = "c-a",
            query = "search-a",
            appliedQuery = "search-a"
        )

        // Fresh state for Topic B
        val pkgIdB = InstalledPackageId("pkg-b")
        val stateB = LessonBrowserUiState(
            installedPackageId = pkgIdB,
            libraryName = "Topic B",
            lessons = listOf(createSampleItem("c-b", "Lesson B"))
        )

        assertEquals("", stateB.query)
        assertEquals("", stateB.appliedQuery)
        assertNull(stateB.selectedLessonId)
        assertEquals(1, stateB.lessonCount)
        assertEquals("c-b", stateB.lessons.first().id)
    }
}
