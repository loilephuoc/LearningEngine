package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.session.LearningSessionProgress
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession

class ExperienceRotationContextTest {
    @Test
    fun `queued presentation position produces stable zero-based ordinal`() {
        val first = item(position = 1, completed = 0)
        val third = item(position = 3, completed = 2)

        assertEquals(0L, ExperienceRotationContext.from(first).ordinal)
        assertEquals(2L, ExperienceRotationContext.from(third).ordinal)
        assertEquals(
            ExperienceRotationContext.from(third),
            ExperienceRotationContext.from(third)
        )
    }

    @Test
    fun `legacy presentation falls back to committed review count`() {
        val item = item(position = null, completed = 2)

        assertEquals(2L, ExperienceRotationContext.from(item).ordinal)
    }

    private fun item(
        position: Int?,
        completed: Int
    ): NextSessionItem {
        val contentId = ContentId("content")
        val learningItemId = LearningItemId("item")
        val session =
            StudySession.start(
                SessionId("session"),
                LearnerId("learner"),
                Moment(1),
                SessionPolicy(10, 10)
            ).copy(newItemsReviewed = completed)
        val content =
            Content(
                id = contentId,
                type = ContentType.SENTENCE,
                text = ContentText("question", "answer"),
                metadata = ContentMetadata()
            )
        val learningItem =
            LearningItem(
                id = learningItemId,
                contentId = contentId,
                mode = LearningMode.MEANING_RECOGNITION
            )
        val progress =
            position?.let {
                LearningSessionProgress(
                    completedItemCount = completed,
                    reviewedItemCount = completed,
                    skippedItemCount = 0,
                    remainingItemCount = 5 - completed,
                    totalItemCount = 5,
                    currentPosition = it,
                    totalIsKnown = true,
                    isEmpty = false,
                    isCompleted = false
                )
            }
        return NextSessionItem(
            session = session,
            item =
                NextLearningItem(
                    learningItem = learningItem,
                    content = content,
                    memoryState = null,
                    effectiveDueAt = Moment(1)
                ),
            progress = progress
        )
    }
}
