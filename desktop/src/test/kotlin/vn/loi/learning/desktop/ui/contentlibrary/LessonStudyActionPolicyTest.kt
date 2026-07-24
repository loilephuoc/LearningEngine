package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LessonStudyActionPolicyTest {

    // 1. zero items -> UNAVAILABLE
    @Test
    fun `zero items returns UNAVAILABLE with disabled CTA and No Learning Items label`() {
        val progress = LessonProgressUiModel(totalLearningItemCount = 0)
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.UNAVAILABLE, action.type)
        assertEquals("No Learning Items", action.label)
        assertFalse(action.isEnabled)
        assertFalse(action.isAvailable)
        assertNull(action.dueText)
    }

    // 2. unseen lesson -> START
    @Test
    fun `unseen lesson returns START with enabled CTA and Start Lesson label`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 5,
            unseenItemCount = 5,
            startedItemCount = 0,
            masteredItemCount = 0
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.START, action.type)
        assertEquals("Start Lesson", action.label)
        assertTrue(action.isEnabled)
        assertTrue(action.isAvailable)
    }

    // 3. NEW-state lesson -> START
    @Test
    fun `NEW state lesson with zero started and zero mastered returns START`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 3,
            newStateItemCount = 3,
            startedItemCount = 0,
            masteredItemCount = 0
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.START, action.type)
        assertEquals("Start Lesson", action.label)
        assertTrue(action.isEnabled)
    }

    // 4. partially started lesson -> CONTINUE
    @Test
    fun `partially started lesson returns CONTINUE with enabled CTA and Continue Lesson label`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 4,
            startedItemCount = 2,
            masteredItemCount = 0
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.CONTINUE, action.type)
        assertEquals("Continue Lesson", action.label)
        assertTrue(action.isEnabled)
    }

    // 5. partially mastered lesson -> CONTINUE
    @Test
    fun `partially mastered lesson returns CONTINUE when mastered is less than total`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 5,
            startedItemCount = 4,
            masteredItemCount = 2
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.CONTINUE, action.type)
        assertEquals("Continue Lesson", action.label)
        assertTrue(action.isEnabled)
    }

    // 6. fully mastered lesson -> REVIEW
    @Test
    fun `fully mastered lesson returns REVIEW with enabled CTA and Review Lesson label`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 4,
            startedItemCount = 4,
            masteredItemCount = 4
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.REVIEW, action.type)
        assertEquals("Review Lesson", action.label)
        assertTrue(action.isEnabled)
    }

    // 7. due count does not change START
    @Test
    fun `due count does not alter START action policy type`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 3,
            newStateItemCount = 3,
            startedItemCount = 0,
            masteredItemCount = 0,
            dueItemCount = 2
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.START, action.type)
        assertEquals("Start Lesson", action.label)
        assertTrue(action.isEnabled)
        assertEquals(2, action.dueCount)
        assertEquals("2 item(s) due now", action.dueText)
    }

    // 8. due count does not change CONTINUE
    @Test
    fun `due count does not alter CONTINUE action policy type`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 5,
            startedItemCount = 3,
            masteredItemCount = 1,
            dueItemCount = 3
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.CONTINUE, action.type)
        assertEquals("Continue Lesson", action.label)
        assertTrue(action.isEnabled)
        assertEquals(3, action.dueCount)
        assertEquals("3 item(s) due now", action.dueText)
    }

    // 9. due count does not change REVIEW
    @Test
    fun `due count does not alter REVIEW action policy type`() {
        val progress = LessonProgressUiModel(
            totalLearningItemCount = 2,
            startedItemCount = 2,
            masteredItemCount = 2,
            dueItemCount = 1
        )
        val action = LessonStudyActionPolicy.evaluate(progress)

        assertEquals(LessonStudyActionType.REVIEW, action.type)
        assertEquals("Review Lesson", action.label)
        assertTrue(action.isEnabled)
        assertEquals(1, action.dueCount)
        assertEquals("1 item(s) due now", action.dueText)
    }
}
