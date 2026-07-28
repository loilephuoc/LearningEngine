package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class LearningStageDiagnosticsTest {
    @Test
    fun `diagnostic carries stable identities and authoritative memory evidence without content text`() {
        val content = Content(
            ContentId("content-1"),
            ContentType.WORD,
            ContentText("sensitive word", "sensitive meaning")
        )
        val item = LearningItem(
            LearningItemId("item-1"),
            content.id,
            LearningMode.MEANING_RECOGNITION
        )
        val state = MemoryState(
            LearnerId("learner"),
            item.id,
            LearningStage.REVIEW,
            5.0,
            2.0,
            Moment(2_000),
            Moment(1_000),
            3,
            0
        )

        val diagnostic = LearningStageDiagnosticsResolver.resolve(
            NextLearningItem(
                content,
                item,
                state,
                state.dueAt,
                hasPersistedMemoryState = true
            )
        )

        assertEquals("item-1", diagnostic.learningItemId)
        assertEquals("content-1", diagnostic.contentId)
        assertEquals(LearningStage.REVIEW, diagnostic.stage)
        assertEquals(3, diagnostic.reviewCount)
        assertEquals(Moment(1_000), diagnostic.lastReviewedAt)
        assertEquals(
            LearningStageSelectionReason.CURRENT_SESSION_QUEUE_ITEM,
            diagnostic.selectionReason
        )
        assertFalse(diagnostic.toString().contains("sensitive"))
    }
}
