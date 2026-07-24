package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class LearningWorkspaceProjectionPolicyTest {

    private fun createBrowserItem(
        id: String = "cnt-1",
        title: String = "Lesson 1",
        itemCount: Int = 10,
        started: Int = 0,
        mastered: Int = 0,
        due: Int = 0
    ): LessonBrowserItem {
        return LessonBrowserItem(
            id = id,
            title = title,
            type = "SENTENCE",
            group = "Group A",
            section = "Section 1",
            lesson = title,
            primaryText = title,
            translatedText = null,
            learningItemCount = itemCount,
            progress = LessonProgressUiModel(
                totalLearningItemCount = itemCount,
                unseenItemCount = itemCount - started,
                newStateItemCount = 0,
                startedItemCount = started,
                masteredItemCount = mastered,
                dueItemCount = due,
                suspendedItemCount = 0,
                completionPercent = if (itemCount > 0) (mastered * 100) / itemCount else 0,
                startedPercent = if (itemCount > 0) (started * 100) / itemCount else 0
            )
        )
    }

    private fun createBrowserState(
        pkgId: InstalledPackageId? = InstalledPackageId("pkg-1"),
        lessons: List<LessonBrowserItem> = emptyList(),
        selectedLessonId: String? = null
    ): LessonBrowserUiState {
        return LessonBrowserUiState(
            libraryId = "lib-1",
            libraryName = "Test Library",
            installedPackageId = pkgId,
            lessons = lessons,
            selectedLessonId = selectedLessonId
        )
    }

    @Test
    fun `1 valid START lesson projects Start action`() {
        val item = createBrowserItem(started = 0, mastered = 0)
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(LessonStudyActionType.START, workspace!!.action.type)
        assertEquals("Start Lesson", workspace.action.label)
        assertTrue(workspace.canStart)
        assertNull(workspace.unavailableReason)
    }

    @Test
    fun `2 valid CONTINUE lesson projects Continue action`() {
        val item = createBrowserItem(started = 3, mastered = 1)
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(LessonStudyActionType.CONTINUE, workspace!!.action.type)
        assertEquals("Continue Lesson", workspace.action.label)
        assertTrue(workspace.canStart)
    }

    @Test
    fun `3 valid REVIEW lesson projects Review action`() {
        val item = createBrowserItem(itemCount = 5, started = 5, mastered = 5)
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(LessonStudyActionType.REVIEW, workspace!!.action.type)
        assertEquals("Review Lesson", workspace.action.label)
        assertTrue(workspace.canStart)
    }

    @Test
    fun `4 zero-item lesson cannot create startable workspace`() {
        val item = createBrowserItem(itemCount = 0)
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(LessonStudyActionType.UNAVAILABLE, workspace!!.action.type)
        assertFalse(workspace.canStart)
        assertNotNull(workspace.unavailableReason)
    }

    @Test
    fun `5 missing package context returns null workspace`() {
        val item = createBrowserItem()
        val state = createBrowserState(pkgId = null, lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNull(workspace)
    }

    @Test
    fun `6 real InstalledPackageId preserved`() {
        val pkgId = InstalledPackageId("pkg-real-123")
        val item = createBrowserItem()
        val state = createBrowserState(pkgId = pkgId, lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(pkgId, workspace!!.installedPackageId)
    }

    @Test
    fun `7 real ContentId preserved`() {
        val item = createBrowserItem(id = "cnt-real-999")
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(ContentId("cnt-real-999"), workspace!!.contentId)
    }

    @Test
    fun `8 progress metrics preserved without recalculation`() {
        val item = createBrowserItem(itemCount = 20, started = 8, mastered = 5, due = 3)
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(20, workspace!!.totalItemCount)
        assertEquals(8, workspace.startedItemCount)
        assertEquals(5, workspace.masteredItemCount)
        assertEquals(3, workspace.dueItemCount)
        assertEquals(25, workspace.completionPercent)
    }

    @Test
    fun `9 selected recommended lesson includes recommendation reason`() {
        val item1 = createBrowserItem(id = "cnt-due", due = 2)
        val item2 = createBrowserItem(id = "cnt-other")
        val state = createBrowserState(lessons = listOf(item1, item2), selectedLessonId = item1.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item1)
        assertNotNull(workspace)
        assertTrue(workspace!!.isRecommended)
        assertNotNull(workspace.recommendationReason)
        assertTrue(workspace.recommendationReason!!.contains("due"))
    }

    @Test
    fun `10 selected non-recommended lesson does not claim recommendation`() {
        val item1 = createBrowserItem(id = "cnt-due", due = 2)
        val item2 = createBrowserItem(id = "cnt-other")
        val state = createBrowserState(lessons = listOf(item1, item2), selectedLessonId = item2.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item2)
        assertNotNull(workspace)
        assertFalse(workspace!!.isRecommended)
        assertNull(workspace.recommendationReason)
    }

    @Test
    fun `11 session preview ordering deterministic`() {
        val item = createBrowserItem()
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val workspace = LearningWorkspaceProjectionPolicy.create(state, item)
        assertNotNull(workspace)
        assertEquals(3, workspace!!.previewStages.size)
        assertEquals("primary", workspace.previewStages[0].key)
        assertEquals("answer-reveal", workspace.previewStages[1].key)
        assertEquals("rating-ready", workspace.previewStages[2].key)
    }

    @Test
    fun `12 repeated projection calls deterministic`() {
        val item = createBrowserItem()
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val w1 = LearningWorkspaceProjectionPolicy.create(state, item)
        val w2 = LearningWorkspaceProjectionPolicy.create(state, item)
        assertEquals(w1, w2)
    }

    @Test
    fun `13 input models are not mutated`() {
        val item = createBrowserItem()
        val state = createBrowserState(lessons = listOf(item), selectedLessonId = item.id)

        val initialLessonId = state.selectedLessonId
        val initialItemCount = item.learningItemCount

        LearningWorkspaceProjectionPolicy.create(state, item)

        assertEquals(initialLessonId, state.selectedLessonId)
        assertEquals(initialItemCount, item.learningItemCount)
    }
}
