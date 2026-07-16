package vn.loi.learning.domain.study.learning.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId

class LearningItemTest {

    @Test
    fun `learning item references content and learning mode`() {
        val item = LearningItem(
            id = LearningItemId("sentence-001-listening"),
            contentId = ContentId("sentence-001"),
            mode = LearningMode.LISTENING_RECOGNITION
        )

        assertEquals(
            LearningItemId("sentence-001-listening"),
            item.id
        )

        assertEquals(
            ContentId("sentence-001"),
            item.contentId
        )

        assertEquals(
            LearningMode.LISTENING_RECOGNITION,
            item.mode
        )

        assertTrue(item.isEnabled)
    }

    @Test
    fun `learning item can be disabled without deleting it`() {
        val item = LearningItem(
            id = LearningItemId("sentence-001-speaking"),
            contentId = ContentId("sentence-001"),
            mode = LearningMode.SPEAKING_RECALL,
            isEnabled = false
        )

        assertFalse(item.isEnabled)
    }
}