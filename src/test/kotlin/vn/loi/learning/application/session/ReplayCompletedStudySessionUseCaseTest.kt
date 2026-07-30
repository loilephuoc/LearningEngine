package vn.loi.learning.application.session

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class ReplayCompletedStudySessionUseCaseTest {
    private val learner = LearnerId("learner")
    private val predecessorId = SessionId("completed")
    private val itemIds = listOf(LearningItemId("item-2"), LearningItemId("item-1"))

    @Test
    fun `finished predecessor accepts exact ordered membership as an ordinary review session`() {
        val fixture = fixture(itemIds)
        val predecessor = fixture.sessions.findById(predecessorId)!!

        val result =
            assertIs<CompletedStudySessionReplayResult.Accepted>(
                fixture.useCase.execute(request())
            )

        assertEquals(itemIds, result.queue.learningItemIds)
        assertEquals(itemIds.toSet(), result.queue.itemOrigins.keys)
        assertEquals(
            setOf(SessionItemOrigin.REVIEW),
            result.queue.itemOrigins.values.toSet()
        )
        assertEquals(0, result.session.policy.newItemLimit)
        assertEquals(2, result.session.policy.reviewItemLimit)
        assertEquals(SessionStatus.ACTIVE, result.session.status)
        assertEquals(predecessor, fixture.sessions.findById(predecessorId))
        assertEquals(itemIds, fixture.queues.findBySessionId(predecessorId)!!.learningItemIds)
    }

    @Test
    fun `sequential duplicate invocation reuses the deterministic replay session`() {
        val fixture = fixture(itemIds)
        val first =
            assertIs<CompletedStudySessionReplayResult.Accepted>(
                fixture.useCase.execute(request())
            )
        val repeated =
            assertIs<CompletedStudySessionReplayResult.Accepted>(
                fixture.useCase.execute(request().copy(requestedAt = Moment(4_000)))
            )

        assertEquals(first.session.id, repeated.session.id)
        assertEquals(false, first.alreadyAccepted)
        assertEquals(true, repeated.alreadyAccepted)
        assertEquals(2, fixture.sessions.findAll().size)
        assertEquals(2, fixture.queues.count())
    }

    @Test
    fun `queue items skipped without a committed review are not replayed`() {
        val fixture = fixture(itemIds, reviewedItems = listOf(itemIds.first()))
        val result =
            assertIs<CompletedStudySessionReplayResult.Accepted>(
                fixture.useCase.execute(request())
            )

        assertEquals(listOf(itemIds.first()), result.queue.learningItemIds)
    }

    @Test
    fun `persisted recreation reuses the same accepted replay session`() {
        val directory = Files.createTempDirectory("completed-session-replay")
        try {
            val firstContext = LearningApplicationFactory.createPersisted(directory)
            persistPredecessor(
                firstContext.studySessionRepository!!,
                firstContext.studyQueue,
                itemIds
            )
            val first =
                assertIs<CompletedStudySessionReplayResult.Accepted>(
                    firstContext.engine.replayCompletedStudySession(request())
                )

            val restarted = LearningApplicationFactory.createPersisted(directory)
            val repeated =
                assertIs<CompletedStudySessionReplayResult.Accepted>(
                    restarted.engine.replayCompletedStudySession(
                        request().copy(requestedAt = Moment(9_000))
                    )
                )
            assertEquals(first.session.id, repeated.session.id)
            assertEquals(true, repeated.alreadyAccepted)
            assertEquals(2, restarted.studySessionRepository!!.findAll().size)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `missing active mismatched learner and missing queue produce explicit outcomes`() {
        val missing = fixtureWithoutPredecessor()
        assertEquals(
            CompletedStudySessionReplayRejection.PRECEDING_SESSION_NOT_FOUND,
            assertIs<CompletedStudySessionReplayResult.Rejected>(
                missing.useCase.execute(request())
            ).reason
        )

        val active = fixture(itemIds, finished = false)
        assertEquals(
            CompletedStudySessionReplayRejection.PRECEDING_SESSION_NOT_COMPLETED,
            assertIs<CompletedStudySessionReplayResult.Rejected>(
                active.useCase.execute(request())
            ).reason
        )

        val mismatch = fixture(itemIds)
        assertEquals(
            CompletedStudySessionReplayRejection.LEARNER_MISMATCH,
            assertIs<CompletedStudySessionReplayResult.Rejected>(
                mismatch.useCase.execute(request().copy(learnerId = LearnerId("other")))
            ).reason
        )

        val noQueue = fixtureWithoutPredecessor()
        noQueue.sessions.save(finishedSession(itemIds))
        assertEquals(
            CompletedStudySessionReplayRejection.PRECEDING_QUEUE_NOT_FOUND,
            assertIs<CompletedStudySessionReplayResult.Rejected>(
                noQueue.useCase.execute(request())
            ).reason
        )
    }

    @Test
    fun `empty completed membership returns no items without creating a session`() {
        val fixture = fixture(emptyList())
        assertEquals(
            CompletedStudySessionReplayResult.NoItems,
            fixture.useCase.execute(request())
        )
        assertNull(fixture.sessions.findActiveByLearner(learner))
        assertEquals(1, fixture.sessions.findAll().size)
    }

    @Test
    fun `persisted queue rejects duplicate membership before replay orchestration`() {
        assertFailsWith<IllegalArgumentException> {
            StudyQueueSnapshot.create(
                sessionId = predecessorId,
                createdAt = Moment(1_000),
                learningItemIds = listOf(itemIds.first(), itemIds.first())
            )
        }
    }

    @Test
    fun `replay item uses the ordinary review transaction and retained Undo boundary`() {
        val context = LearningApplicationFactory.createInMemory()
        val contentId = ContentId("content")
        val itemId = LearningItemId("item")
        context.engine.registerContent(
            Content(contentId, ContentType.WORD, ContentText("word", "meaning"))
        )
        context.engine.registerLearningItem(
            LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
        )
        context.engine.review(
            vn.loi.learning.application.review.ReviewCommand(
                ReviewEventId("seed-review"),
                learner,
                itemId,
                ReviewRating.GOOD,
                Moment(500)
            )
        )
        persistPredecessor(
            context.studySessionRepository!!,
            context.studyQueue,
            listOf(itemId)
        )
        val replay =
            assertIs<CompletedStudySessionReplayResult.Accepted>(
                context.engine.replayCompletedStudySession(request())
            )
        val presented =
            context.engine.getNextSessionItem(
                replay.session.id,
                Moment(Long.MAX_VALUE / 2)
            )!!
        context.engine.revealSessionItem(replay.session.id, itemId)
        context.engine.reviewSessionItem(
            ReviewSessionItemCommand(
                replay.session.id,
                ReviewEventId("replay-review"),
                itemId,
                ReviewRating.GOOD,
                Moment(Long.MAX_VALUE / 2 + 1)
            )
        )

        assertEquals(itemId, presented.item.learningItem.id)
        assertEquals(2, context.reviewEventRepository!!.findAll(learner).size)
        assertIs<UndoLatestSessionReviewResult.Undone>(
            context.engine.undoLatestSessionReview(replay.session.id)
        )
        assertEquals(1, context.reviewEventRepository!!.findAll(learner).size)
    }

    private fun fixture(
        items: List<LearningItemId>,
        finished: Boolean = true,
        reviewedItems: List<LearningItemId> = items
    ): Fixture =
        fixtureWithoutPredecessor().also { fixture ->
            val session = if (finished) finishedSession(reviewedItems) else activeSession()
            fixture.sessions.save(session)
            var queue =
                fixture.queueService.create(
                    sessionId = predecessorId,
                    createdAt = Moment(1_000),
                    learningItemIds = items,
                    itemOrigins = items.associateWith { SessionItemOrigin.REVIEW },
                    itemContentIds = items.associateWith { ContentId("content-${it.value}") },
                    configuredReviewTarget = items.size,
                    effectiveReviewWorkload = items.size
                )
            repeat(items.size) { queue = fixture.queueService.advance(predecessorId) }
        }

    private fun fixtureWithoutPredecessor(): Fixture {
        val sessions = InMemoryStudySessionRepository()
        val queues = InMemoryStudyQueueRepository()
        val queueService = StudyQueueService(queues)
        return Fixture(
            sessions,
            queues,
            queueService,
            ReplayCompletedStudySessionUseCase(sessions, queueService)
        )
    }

    private fun persistPredecessor(
        sessions: vn.loi.learning.application.port.StudySessionRepository,
        queues: StudyQueueService,
        items: List<LearningItemId>
    ) {
        sessions.save(finishedSession(items))
        queues.create(
            sessionId = predecessorId,
            createdAt = Moment(1_000),
            learningItemIds = items,
            itemOrigins = items.associateWith { SessionItemOrigin.REVIEW },
            configuredReviewTarget = items.size,
            effectiveReviewWorkload = items.size
        )
        repeat(items.size) { queues.advance(predecessorId) }
    }

    private fun activeSession() =
        StudySession.start(
            id = predecessorId,
            learnerId = learner,
            startedAt = Moment(1_000),
            policy = SessionPolicy(newItemLimit = 0, reviewItemLimit = 10)
        )

    private fun finishedSession(items: List<LearningItemId>): StudySession {
        val contentIds = items.mapTo(linkedSetOf()) { ContentId("content-${it.value}") }
        return activeSession()
            .copy(
                reviewedItemIds = items.toSet(),
                reviewedContentIds = contentIds,
                reviewItemsReviewed = contentIds.size
            )
            .finish(Moment(2_000))
    }

    private fun request() =
        ReplayCompletedStudySessionRequest(
            precedingSessionId = predecessorId,
            learnerId = learner,
            requestedAt = Moment(3_000)
        )

    private data class Fixture(
        val sessions: InMemoryStudySessionRepository,
        val queues: InMemoryStudyQueueRepository,
        val queueService: StudyQueueService,
        val useCase: ReplayCompletedStudySessionUseCase
    )
}
