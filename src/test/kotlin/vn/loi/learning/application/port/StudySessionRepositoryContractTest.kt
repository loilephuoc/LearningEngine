package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.session.model.UndoableSessionReview
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedStudySessionRepository
import vn.loi.learning.infrastructure.persistence.store.InMemoryStudySessionStore

class StudySessionRepositoryContractTest {

    private val repository: StudySessionRepository =
        StoreBackedStudySessionRepository(
            store = InMemoryStudySessionStore()
        )

    @Test
    fun `findById returns null when session does not exist`() {
        val result = repository.findById(
            SessionId("missing-session")
        )

        assertNull(result)
    }

    @Test
    fun `save stores study session`() {
        val session = createActiveSession(
            sessionId = SessionId("session-1")
        )

        repository.save(session)

        val result = repository.findById(session.id)

        assertEquals(
            expected = session,
            actual = result
        )
    }

    @Test
    fun `save replaces existing session with same id`() {
        val sessionId = SessionId("session-1")

        val initialSession = createActiveSession(
            sessionId = sessionId
        )

        val reviewedSession = initialSession.recordReview(
            learningItemId = LearningItemId("item-1"),
            contentId = ContentId("content-1"),
            wasNewItem = true
        )

        repository.save(initialSession)
        repository.save(reviewedSession)

        val result = repository.findById(sessionId)

        assertEquals(
            expected = reviewedSession,
            actual = result
        )
    }

    @Test
    fun `sessions with different ids do not overwrite each other`() {
        val firstSession = createActiveSession(
            sessionId = SessionId("session-1"),
            learnerId = LearnerId("learner-1")
        )

        val secondSession = createActiveSession(
            sessionId = SessionId("session-2"),
            learnerId = LearnerId("learner-2")
        )

        repository.save(firstSession)
        repository.save(secondSession)

        val firstResult =
            repository.findById(firstSession.id)

        val secondResult =
            repository.findById(secondSession.id)

        assertEquals(
            expected = firstSession,
            actual = firstResult
        )

        assertEquals(
            expected = secondSession,
            actual = secondResult
        )
    }

    @Test
    fun `save preserves reviewed item and content ids`() {
        val initialSession = createActiveSession(
            sessionId = SessionId("session-1")
        )

        val firstReview = initialSession.recordReview(
            learningItemId = LearningItemId("item-1"),
            contentId = ContentId("content-1"),
            wasNewItem = true
        )

        val secondReview = firstReview.recordReview(
            learningItemId = LearningItemId("item-2"),
            contentId = ContentId("content-2"),
            wasNewItem = false
        )

        repository.save(secondReview)

        val result = requireNotNull(
            repository.findById(secondReview.id)
        )

        assertEquals(
            expected = setOf(
                LearningItemId("item-1"),
                LearningItemId("item-2")
            ),
            actual = result.reviewedItemIds
        )

        assertEquals(
            expected = setOf(
                ContentId("content-1"),
                ContentId("content-2")
            ),
            actual = result.reviewedContentIds
        )

        assertEquals(
            expected = 1,
            actual = result.newItemsReviewed
        )

        assertEquals(
            expected = 1,
            actual = result.reviewItemsReviewed
        )
    }

    @Test
    fun `save preserves finished session snapshot`() {
        val activeSession = createActiveSession(
            sessionId = SessionId("session-1"),
            startedAt = Moment(1_000L)
        )

        val finishedSession = activeSession.finish(
            at = Moment(2_000L)
        )

        repository.save(finishedSession)

        val result = requireNotNull(
            repository.findById(finishedSession.id)
        )

        assertEquals(
            expected = SessionStatus.FINISHED,
            actual = result.status
        )

        assertEquals(
            expected = Moment(2_000L),
            actual = result.finishedAt
        )
    }


    @Test
    fun `findActiveByLearner returns latest active session`() {
        val learnerId = LearnerId("learner-1")

        val olderActiveSession = createActiveSession(
            sessionId = SessionId("session-older"),
            learnerId = learnerId,
            startedAt = Moment(1_000L)
        )

        val latestActiveSession = createActiveSession(
            sessionId = SessionId("session-latest"),
            learnerId = learnerId,
            startedAt = Moment(2_000L)
        )

        val finishedSession = createActiveSession(
            sessionId = SessionId("session-finished"),
            learnerId = learnerId,
            startedAt = Moment(3_000L)
        ).finish(
            at = Moment(4_000L)
        )

        repository.save(olderActiveSession)
        repository.save(latestActiveSession)
        repository.save(finishedSession)

        assertEquals(
            expected = latestActiveSession,
            actual = repository.findActiveByLearner(
                learnerId
            )
        )
    }

    @Test
    fun `findActiveByLearner ignores sessions for other learners`() {
        repository.save(
            createActiveSession(
                sessionId = SessionId("session-other"),
                learnerId = LearnerId("other-learner")
            )
        )

        assertNull(
            repository.findActiveByLearner(
                LearnerId("learner-1")
            )
        )
    }

    @Test
    fun `latest undoable query excludes legacy sessions without package or topic ownership`() {
        listOf<() -> StudySessionRepository>(
            { InMemoryStudySessionRepository() },
            { StoreBackedStudySessionRepository(InMemoryStudySessionStore()) }
        ).forEach { repositoryFactory ->
            val learnerId = LearnerId("learner-legacy")
            val missingPackageRepository = repositoryFactory()
            missingPackageRepository.save(
                createUndoableFinishedSession(
                    sessionId = SessionId("missing-package"),
                    learnerId = learnerId
                ).copy(installedPackageId = null)
            )
            assertNull(missingPackageRepository.findLatestUndoableByLearner(learnerId))

            val missingTopicRepository = repositoryFactory()
            missingTopicRepository.save(
                createUndoableFinishedSession(
                    sessionId = SessionId("missing-topic"),
                    learnerId = learnerId
                ).copy(topicId = null)
            )
            assertNull(missingTopicRepository.findLatestUndoableByLearner(learnerId))

            val validRepository = repositoryFactory()
            val valid = createUndoableFinishedSession(
                sessionId = SessionId("valid"),
                learnerId = learnerId
            )
            validRepository.save(valid)
            assertEquals(valid, validRepository.findLatestUndoableByLearner(learnerId))
        }
    }

    private fun createActiveSession(
        sessionId: SessionId,
        learnerId: LearnerId = LearnerId("learner-1"),
        startedAt: Moment = Moment(1_000L)
    ): StudySession =
        StudySession.start(
            id = sessionId,
            learnerId = learnerId,
            startedAt = startedAt,
            policy = SessionPolicy(
                newItemLimit = 10,
                reviewItemLimit = 10
            )
        )

    private fun createUndoableFinishedSession(
        sessionId: SessionId,
        learnerId: LearnerId
    ): StudySession {
        val itemId = LearningItemId("item-${sessionId.value}")
        val contentId = ContentId("content-${sessionId.value}")
        val startedAt = Moment(1_000L)
        val undo = UndoableSessionReview(
            reviewEventId = ReviewEventId("review-${sessionId.value}"),
            learningItemId = itemId,
            contentId = contentId,
            memoryStateBefore = MemoryState.new(learnerId, itemId, startedAt),
            memoryStateExistedBefore = false,
            reviewedItemIdsBefore = emptySet(),
            reviewedContentIdsBefore = emptySet(),
            newItemsReviewedBefore = 0,
            reviewItemsReviewedBefore = 0,
            currentItemPresentedAtBefore = startedAt,
            answerRevealedBefore = true
        )
        return StudySession.start(
            id = sessionId,
            learnerId = learnerId,
            startedAt = startedAt,
            policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 1),
            topicId = TopicId("topic"),
            installedPackageId = InstalledPackageId("package")
        ).recordReview(
            learningItemId = itemId,
            contentId = contentId,
            wasNewItem = true,
            undoableReview = undo
        ).finish(Moment(2_000L))
    }
}
