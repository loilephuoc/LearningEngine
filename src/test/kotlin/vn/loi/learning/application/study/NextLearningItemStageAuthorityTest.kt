package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

class NextLearningItemStageAuthorityTest {
    private val content = Content(
        ContentId("content-1"),
        ContentType.WORD,
        ContentText("word", "meaning")
    )
    private val learningItem = LearningItem(
        LearningItemId("item-1"),
        content.id,
        LearningMode.MEANING_RECOGNITION
    )

    @Test
    fun `effective NEW state is new regardless of persistence existence`() {
        val effectiveNew = MemoryState.new(
            LearnerId("learner"),
            learningItem.id,
            Moment(100)
        )

        val item = next(effectiveNew, hasPersistedMemoryState = false)

        assertEquals(LearningStage.NEW, item.learningStage)
        assertTrue(item.isNew)
        assertFalse(item.hasPersistedMemoryState)
    }

    @Test
    fun `persisted NEW remains authoritative NEW without nullability heuristic`() {
        val persistedNew = MemoryState.new(
            LearnerId("learner"),
            learningItem.id,
            Moment(100)
        )

        val item = next(persistedNew, hasPersistedMemoryState = true)

        assertEquals(LearningStage.NEW, item.learningStage)
        assertTrue(item.isNew)
        assertTrue(item.hasPersistedMemoryState)
    }

    @Test
    fun `reviewed effective state is never classified NEW`() {
        val reviewed = MemoryState(
            learnerId = LearnerId("learner"),
            learningItemId = learningItem.id,
            stage = LearningStage.REVIEW,
            difficulty = 5.0,
            stabilityDays = 2.0,
            dueAt = Moment(2_000),
            lastReviewedAt = Moment(1_000),
            reviewCount = 1,
            lapseCount = 0
        )

        val item = next(reviewed, hasPersistedMemoryState = true)

        assertEquals(LearningStage.REVIEW, item.learningStage)
        assertFalse(item.isNew)
    }

    private fun next(
        state: MemoryState,
        hasPersistedMemoryState: Boolean
    ) = NextLearningItem(
        content = content,
        learningItem = learningItem,
        memoryState = state,
        effectiveDueAt = state.dueAt,
        hasPersistedMemoryState = hasPersistedMemoryState
    )
}
