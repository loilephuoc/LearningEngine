package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating

class LeaveActiveStudySessionUseCaseTest {
    @Test
    fun `leave closes one active session and releases only its navigation queue`() {
        val learner = LearnerId("learner")
        val sessionId = SessionId("active")
        val sessions = InMemoryStudySessionRepository()
        val queues = StudyQueueService(InMemoryStudyQueueRepository())
        sessions.save(
            StudySession.start(
                sessionId,
                learner,
                Moment(10),
                SessionPolicy(10, 20)
            )
        )
        queues.create(sessionId, Moment(10), listOf(LearningItemId("item")))

        val left =
            LeaveActiveStudySessionUseCase(
                sessions,
                queues,
                InMemoryTransactionRunner()
            ).execute(learner, Moment(20))

        assertEquals(SessionStatus.FINISHED, left?.status)
        assertNull(sessions.findActiveByLearner(learner))
        assertNull(queues.get(sessionId))
        assertTrue(sessions.findById(sessionId) != null)
    }

    @Test
    fun `leave then Review All preserves committed history and accepts only one replacement`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("learner")
        val contentId = ContentId("content")
        val itemId = LearningItemId("item")
        val activeId = SessionId("active")
        context.engine.registerContent(
            Content(contentId, ContentType.WORD, ContentText("word", "meaning"))
        )
        context.engine.registerLearningItem(
            LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
        )
        context.engine.review(
            vn.loi.learning.application.review.ReviewCommand(
                ReviewEventId("committed"),
                learner,
                itemId,
                ReviewRating.GOOD,
                Moment(1)
            )
        )
        context.engine.startSession(
            StartStudySessionCommand(
                activeId,
                learner,
                Moment(10),
                SessionPolicy(0, 1),
                includedContentIds = setOf(contentId),
                topicId = TopicId("topic"),
                installedPackageId = InstalledPackageId("package")
            )
        )

        context.engine.leaveActiveStudySession(learner, Moment(20))
        val replacement =
            context.engine.startLearnedItemsReview(
                StartLearnedItemsReviewRequest(
                    LearnEntryScope(
                        learner,
                        InstalledPackageId("package"),
                        TopicId("topic"),
                        setOf(contentId)
                    ),
                    Moment(21),
                    10
                )
            )

        val accepted = replacement as StartLearnedItemsReviewResult.Accepted
        assertEquals(1, context.reviewEventRepository!!.findAll(learner).size)
        assertEquals(
            accepted.session.id,
            context.engine.getActiveSession(learner)?.id
        )
        assertEquals(SessionStatus.FINISHED, context.engine.getSession(activeId)?.status)
        assertNull(context.engine.getStudyQueueProgress(activeId))
    }
}
