package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.ContentId

class PackageLearningRecommendationPolicyTest {

    private fun createItem(
        id: String,
        title: String,
        learningItemCount: Int,
        progress: LessonProgressUiModel = LessonProgressUiModel.empty()
    ): LessonBrowserItem = LessonBrowserItem(
        id = id,
        title = title,
        type = "SENTENCE",
        group = "Group",
        section = "Section",
        lesson = title,
        primaryText = title,
        translatedText = null,
        learningItemCount = learningItemCount,
        progress = progress
    )

    // 1. empty lessons -> NONE
    @Test
    fun `empty lessons list returns null recommendation`() {
        val result = PackageLearningRecommendationPolicy.evaluate(emptyList())
        assertNull(result)
    }

    // 2. only zero-item lessons -> NONE
    @Test
    fun `only zero-item lessons returns null recommendation`() {
        val item1 = createItem("c1", "Zero 1", learningItemCount = 0)
        val item2 = createItem("c2", "Zero 2", learningItemCount = 0)
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item1, item2))
        assertNull(result)
    }

    // 3. one due lesson -> DUE_NOW
    @Test
    fun `one due lesson returns DUE_NOW recommendation`() {
        val item = createItem(
            "c1", "Due Lesson", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, dueItemCount = 2)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
        assertEquals("Due Lesson", result.lessonTitle)
        assertEquals(RecommendationReasonType.DUE_NOW, result.reasonType)
        assertEquals("Review due items", result.actionLabel)
        assertEquals("2 item(s) are due now", result.reasonText)
    }

    // 4. due beats in-progress
    @Test
    fun `due lesson beats in-progress lesson`() {
        val itemInProgress = createItem(
            "c1", "In Progress", learningItemCount = 4,
            progress = LessonProgressUiModel(totalLearningItemCount = 4, startedItemCount = 2)
        )
        val itemDue = createItem(
            "c2", "Due Lesson", learningItemCount = 4,
            progress = LessonProgressUiModel(totalLearningItemCount = 4, dueItemCount = 1)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(itemInProgress, itemDue))
        assertNotNull(result)
        assertEquals(ContentId("c2"), result.contentId)
        assertEquals(RecommendationReasonType.DUE_NOW, result.reasonType)
    }

    // 5. due beats unstarted
    @Test
    fun `due lesson beats unstarted lesson`() {
        val itemUnstarted = createItem("c1", "Unstarted", learningItemCount = 3)
        val itemDue = createItem(
            "c2", "Due Lesson", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, dueItemCount = 1)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(itemUnstarted, itemDue))
        assertNotNull(result)
        assertEquals(ContentId("c2"), result.contentId)
        assertEquals(RecommendationReasonType.DUE_NOW, result.reasonType)
    }

    // 6. first due lesson wins by canonical order
    @Test
    fun `first due lesson in canonical order wins`() {
        val itemDue1 = createItem(
            "c1", "Due First", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, dueItemCount = 1)
        )
        val itemDue2 = createItem(
            "c2", "Due Second", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, dueItemCount = 5)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(itemDue1, itemDue2))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
        assertEquals("Due First", result.lessonTitle)
    }

    // 7. in-progress beats unstarted
    @Test
    fun `in-progress lesson beats unstarted lesson`() {
        val itemUnstarted = createItem("c1", "Unstarted", learningItemCount = 5)
        val itemInProgress = createItem(
            "c2", "In Progress", learningItemCount = 5,
            progress = LessonProgressUiModel(totalLearningItemCount = 5, startedItemCount = 2, masteredItemCount = 1)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(itemUnstarted, itemInProgress))
        assertNotNull(result)
        assertEquals(ContentId("c2"), result.contentId)
        assertEquals(RecommendationReasonType.CONTINUE_IN_PROGRESS, result.reasonType)
        assertEquals("Continue this lesson", result.actionLabel)
        assertEquals("1 of 5 items mastered", result.reasonText)
    }

    // 8. first in-progress lesson wins by canonical order
    @Test
    fun `first in-progress lesson in canonical order wins`() {
        val item1 = createItem(
            "c1", "Progress 1", learningItemCount = 4,
            progress = LessonProgressUiModel(totalLearningItemCount = 4, startedItemCount = 1)
        )
        val item2 = createItem(
            "c2", "Progress 2", learningItemCount = 4,
            progress = LessonProgressUiModel(totalLearningItemCount = 4, startedItemCount = 3)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item1, item2))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
    }

    // 9. unstarted lesson -> START_NEW
    @Test
    fun `unstarted lesson returns START_NEW recommendation`() {
        val item = createItem("c1", "New Lesson", learningItemCount = 4)
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
        assertEquals(RecommendationReasonType.START_NEW, result.reasonType)
        assertEquals("Start this lesson", result.actionLabel)
        assertEquals("This is the next unstarted lesson", result.reasonText)
    }

    // 10. first unstarted lesson wins by canonical order
    @Test
    fun `first unstarted lesson in canonical order wins`() {
        val item1 = createItem("c1", "New 1", learningItemCount = 2)
        val item2 = createItem("c2", "New 2", learningItemCount = 2)
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item1, item2))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
    }

    // 11. only mastered lessons -> REVIEW_COMPLETED
    @Test
    fun `only mastered lessons returns REVIEW_COMPLETED`() {
        val item1 = createItem(
            "c1", "Mastered 1", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, startedItemCount = 3, masteredItemCount = 3)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item1))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
        assertEquals(RecommendationReasonType.REVIEW_COMPLETED, result.reasonType)
        assertEquals("Review this lesson", result.actionLabel)
        assertEquals("All items are mastered", result.reasonText)
    }

    // 12. first mastered lesson wins by canonical order
    @Test
    fun `first mastered lesson in canonical order wins`() {
        val item1 = createItem(
            "c1", "Mastered 1", learningItemCount = 2,
            progress = LessonProgressUiModel(totalLearningItemCount = 2, startedItemCount = 2, masteredItemCount = 2)
        )
        val item2 = createItem(
            "c2", "Mastered 2", learningItemCount = 2,
            progress = LessonProgressUiModel(totalLearningItemCount = 2, startedItemCount = 2, masteredItemCount = 2)
        )
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item1, item2))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
    }

    // 13. zero-item lesson is skipped
    @Test
    fun `zero-item lesson is skipped in favor of valid lesson`() {
        val zeroItem = createItem("c1", "Zero Item", learningItemCount = 0)
        val validItem = createItem("c2", "Valid Lesson", learningItemCount = 3)
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(zeroItem, validItem))
        assertNotNull(result)
        assertEquals(ContentId("c2"), result.contentId)
    }

    // 14. recommendation preserves real ContentId
    @Test
    fun `recommendation preserves exact real ContentId`() {
        val item = createItem("content-unique-id-999", "Unique Content", learningItemCount = 1)
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item))
        assertNotNull(result)
        assertEquals(ContentId("content-unique-id-999"), result.contentId)
    }

    // 15. input ordering remains unchanged
    @Test
    fun `input list ordering is preserved as canonical tie breaker`() {
        val item1 = createItem("c1", "Z Title", learningItemCount = 2)
        val item2 = createItem("c2", "A Title", learningItemCount = 2)
        val result = PackageLearningRecommendationPolicy.evaluate(listOf(item1, item2))
        assertNotNull(result)
        assertEquals(ContentId("c1"), result.contentId)
    }

    // 16. policy is deterministic across repeated calls
    @Test
    fun `policy is completely deterministic across repeated evaluations`() {
        val item1 = createItem("c1", "L1", learningItemCount = 2)
        val item2 = createItem(
            "c2", "L2", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, dueItemCount = 1)
        )
        val list = listOf(item1, item2)

        val res1 = PackageLearningRecommendationPolicy.evaluate(list)
        val res2 = PackageLearningRecommendationPolicy.evaluate(list)

        assertEquals(res1, res2)
    }
}
