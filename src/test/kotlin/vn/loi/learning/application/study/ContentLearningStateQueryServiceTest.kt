package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.review.ReviewCommand
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
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ContentLearningStateQueryServiceTest {
    @Test
    fun `latest effective event across siblings follows authoritative order`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("learner")
        val content = ContentId("content")
        val itemA = LearningItemId("item-a")
        val itemB = LearningItemId("item-b")
        context.engine.registerContent(
            Content(content, ContentType.WORD, ContentText("word"))
        )
        context.engine.registerLearningItem(
            LearningItem(itemA, content, LearningMode.MEANING_RECOGNITION)
        )
        context.engine.registerLearningItem(
            LearningItem(itemB, content, LearningMode.LISTENING_RECOGNITION)
        )

        assertFalse(context.engine.getContentLearningState(learner, content).isLearned)
        context.engine.review(command("good", learner, itemA, ReviewRating.GOOD, 100))
        assertEquals(
            ReviewRating.GOOD,
            context.engine.getContentLearningState(learner, content).latestEffectiveRating
        )
        context.engine.review(command("hard", learner, itemB, ReviewRating.HARD, 200))
        context.engine.review(command("easy-tie", learner, itemA, ReviewRating.EASY, 200))

        val resolved = context.engine.getContentLearningState(learner, content)
        assertTrue(resolved.isLearned)
        assertEquals(setOf(itemA, itemB), resolved.siblingLearningItemIds)
        assertEquals(ReviewRating.EASY, resolved.latestEffectiveRating)
        assertEquals(ReviewEventId("easy-tie"), resolved.latestEffectiveReviewEvent?.id)
    }

    @Test
    fun `MemoryState without completed event is not learned Content history`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("learner")
        val content = ContentId("corrupt-content")
        val item = LearningItemId("corrupt-item")
        context.engine.registerContent(
            Content(content, ContentType.WORD, ContentText("word"))
        )
        context.engine.registerLearningItem(
            LearningItem(item, content, LearningMode.MEANING_RECOGNITION)
        )
        context.memoryStateRepository!!.save(
            MemoryState(
                learner,
                item,
                LearningStage.REVIEW,
                5.0,
                1.0,
                Moment(500),
                Moment(100),
                1,
                0
            )
        )

        val resolved = context.engine.getContentLearningState(learner, content)
        assertFalse(resolved.isLearned)
        assertNull(resolved.latestEffectiveRating)
    }

    private fun command(
        id: String,
        learner: LearnerId,
        item: LearningItemId,
        rating: ReviewRating,
        at: Long
    ) = ReviewCommand(
        ReviewEventId(id),
        learner,
        item,
        rating,
        Moment(at)
    )
}
