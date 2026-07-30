package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.LearningApplicationFactory

class LearnEntryReviewUseCasesTest {
    private val learner = LearnerId("learner")
    private val packageId = InstalledPackageId("package")
    private val topicId = TopicId("topic")
    private val contentIds = (1..6).map { ContentId("content-$it") }
    private val scope = LearnEntryScope(learner, packageId, topicId, contentIds.toSet())

    @Test
    fun `fifty two learned Content create an uncapped fifty two target`() {
        val fixture = fixture()
        val allContentIds = (1..52).map { ContentId("all-content-$it") }
        allContentIds.forEachIndexed { index, contentId ->
            val item =
                LearningItem(
                    LearningItemId("all-item-${index + 1}"),
                    contentId,
                    LearningMode.MEANING_RECOGNITION
                ).also(fixture.items::save)
            fixture.memories.save(reviewedState(item.id, index.toLong() + 1))
        }
        val allScope = LearnEntryScope(learner, packageId, topicId, allContentIds.toSet())

        val availability = fixture.availability.execute(allScope, Moment(100))
        val learned =
            assertIs<LearnedItemsReviewAvailability.Available>(availability.learnedItems)
        val accepted =
            assertIs<StartLearnedItemsReviewResult.Accepted>(
                fixture.start.execute(StartLearnedItemsReviewRequest(allScope, Moment(100)))
            )

        assertEquals(52, learned.totalLearnedCount)
        assertEquals(52, learned.sessionItemCount)
        assertEquals(52, accepted.session.policy.reviewItemLimit)
        assertEquals(52, accepted.queue.effectiveReviewWorkload)
    }

    @Test
    fun `review all uses every unique learned Content regardless of ordinary review limit`() {
        val fixture = fixture()
        val items = contentIds.mapIndexed { index, contentId ->
            LearningItem(
                LearningItemId("item-${index + 1}"),
                contentId,
                LearningMode.MEANING_RECOGNITION,
                isEnabled = index != 4
            ).also(fixture.items::save)
        }
        listOf(0, 1, 2, 4).forEach { index ->
            fixture.memories.save(reviewedState(items[index].id, lastReviewedAt = 10L + index))
        }

        val result = assertIs<StartLearnedItemsReviewResult.Accepted>(
            fixture.start.execute(
                StartLearnedItemsReviewRequest(scope, Moment(100))
            )
        )

        assertEquals(listOf(items[0].id, items[1].id, items[2].id), result.queue.learningItemIds)
        assertEquals(setOf(SessionItemOrigin.REVIEW), result.queue.itemOrigins.values.toSet())
        assertEquals(0, result.session.policy.newItemLimit)
        assertEquals(3, result.session.policy.reviewItemLimit)
        assertEquals(packageId, result.session.installedPackageId)
        assertEquals(topicId, result.session.topicId)
        assertEquals(3, result.queue.configuredReviewTarget)
        assertEquals(3, result.queue.effectiveReviewWorkload)
    }

    @Test
    fun `never reviewed skipped-only disabled and empty scope produce no review work`() {
        val fixture = fixture()
        fixture.items.save(
            LearningItem(
                LearningItemId("never-reviewed"),
                contentIds.first(),
                LearningMode.MEANING_RECOGNITION
            )
        )
        assertEquals(
            StartLearnedItemsReviewResult.NoItems,
            fixture.start.execute(
                StartLearnedItemsReviewRequest(scope, Moment(100))
            )
        )
    }

    @Test
    fun `latest completed availability stays inside learner package topic and committed membership`() {
        val fixture = fixture()
        val item = LearningItem(
            LearningItemId("learned"),
            contentIds.first(),
            LearningMode.MEANING_RECOGNITION
        ).also(fixture.items::save)
        fixture.memories.save(reviewedState(item.id, 20))
        saveFinished(fixture, "valid-old", learner, packageId, topicId, item, 200)
        saveFinished(fixture, "valid-latest", learner, packageId, topicId, item, 400)
        saveFinished(fixture, "other-learner", LearnerId("other"), packageId, topicId, item, 900)
        saveFinished(
            fixture, "other-package", learner, InstalledPackageId("other"), topicId, item, 800
        )
        saveFinished(
            fixture, "other-topic", learner, packageId, TopicId("other"), item, 700
        )
        fixture.sessions.save(
            StudySession.start(
                SessionId("active"),
                learner,
                Moment(950),
                SessionPolicy(0, 5),
                contentIds.toSet(),
                topicId,
                packageId
            )
        )

        val result = fixture.availability.execute(scope, now = Moment(1_000))
        val latest = assertIs<LatestCompletedSessionAvailability.Available>(
            result.latestCompletedSession
        )
        assertEquals(SessionId("valid-latest"), latest.sessionId)
        assertEquals(1, latest.itemCount)
        val learned = assertIs<LearnedItemsReviewAvailability.Available>(result.learnedItems)
        assertEquals(1, learned.totalLearnedCount)
        assertEquals(1, learned.sessionItemCount)
    }

    @Test
    fun `duplicate immediate review-all invocation cannot create a second active session`() {
        val fixture = fixture()
        val item = LearningItem(
            LearningItemId("learned"),
            contentIds.first(),
            LearningMode.MEANING_RECOGNITION
        ).also(fixture.items::save)
        fixture.memories.save(reviewedState(item.id, 20))
        val request = StartLearnedItemsReviewRequest(scope, Moment(100))

        assertIs<StartLearnedItemsReviewResult.Accepted>(fixture.start.execute(request))
        assertEquals(
            StartLearnedItemsReviewRejection.OTHER_ACTIVE_SESSION_EXISTS,
            assertIs<StartLearnedItemsReviewResult.Rejected>(
                fixture.start.execute(request.copy(requestedAt = Moment(101)))
            ).reason
        )
        assertEquals(1, fixture.sessions.findAll().size)
    }

    @Test
    fun `accepted review-all session uses ordinary rating statistics and Undo transaction`() {
        val context = LearningApplicationFactory.createInMemory()
        val contentId = ContentId("ordinary-content")
        val itemId = LearningItemId("ordinary-item")
        context.engine.registerContent(
            Content(contentId, ContentType.WORD, ContentText("word", "meaning"))
        )
        context.engine.registerLearningItem(
            LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
        )
        context.engine.review(
            vn.loi.learning.application.review.ReviewCommand(
                ReviewEventId("seed"),
                learner,
                itemId,
                ReviewRating.GOOD,
                Moment(1)
            )
        )
        val accepted = assertIs<StartLearnedItemsReviewResult.Accepted>(
            context.engine.startLearnedItemsReview(
                StartLearnedItemsReviewRequest(
                    LearnEntryScope(
                        learner,
                        packageId,
                        topicId,
                        includedContentIds = setOf(contentId)
                    ),
                    Moment(100)
                )
            )
        )
        context.engine.getNextSessionItem(accepted.session.id, Moment(100))!!
        context.engine.revealSessionItem(accepted.session.id, itemId)
        context.engine.reviewSessionItem(
            ReviewSessionItemCommand(
                accepted.session.id,
                ReviewEventId("review-all"),
                itemId,
                ReviewRating.HARD,
                Moment(101)
            )
        )

        assertEquals(2, context.reviewEventRepository!!.findAll(learner).size)
        assertEquals(1, context.engine.getSession(accepted.session.id)!!.reviewItemsReviewed)
        assertIs<UndoLatestSessionReviewResult.Undone>(
            context.engine.undoLatestSessionReview(accepted.session.id)
        )
        assertEquals(1, context.reviewEventRepository!!.findAll(learner).size)
        assertEquals(0, context.engine.getSession(accepted.session.id)!!.reviewItemsReviewed)
    }

    @Test
    fun `Review All tracks unique Content while Again retries remain fair and completion bounded`() {
        val context = LearningApplicationFactory.createInMemory()
        val reviewContentIds = (1..3).map { ContentId("coverage-content-$it") }
        val itemIds = reviewContentIds.mapIndexed { index, contentId ->
            val itemId = LearningItemId("coverage-item-${index + 1}")
            context.engine.registerContent(
                Content(contentId, ContentType.WORD, ContentText("word-$index", "meaning-$index"))
            )
            context.engine.registerLearningItem(
                LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
            )
            context.engine.review(
                vn.loi.learning.application.review.ReviewCommand(
                    ReviewEventId("coverage-seed-$index"),
                    learner,
                    itemId,
                    ReviewRating.GOOD,
                    Moment(index.toLong() + 1)
                )
            )
            itemId
        }
        val accepted =
            assertIs<StartLearnedItemsReviewResult.Accepted>(
                context.engine.startLearnedItemsReview(
                    StartLearnedItemsReviewRequest(
                        LearnEntryScope(
                            learner,
                            packageId,
                            topicId,
                            includedContentIds = reviewContentIds.toSet()
                        ),
                        Moment(100)
                    )
                )
            )

        fun reviewCurrent(event: String, rating: ReviewRating, at: Long) {
            val current = context.engine.getNextSessionItem(accepted.session.id, Moment(at))!!
            context.engine.revealSessionItem(accepted.session.id, current.item.learningItem.id)
            context.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    accepted.session.id,
                    ReviewEventId(event),
                    current.item.learningItem.id,
                    rating,
                    Moment(at)
                )
            )
        }

        reviewCurrent("coverage-first", ReviewRating.AGAIN, 101)
        assertEquals(1, context.engine.getSession(accepted.session.id)!!.reviewItemsReviewed)
        reviewCurrent("coverage-second", ReviewRating.GOOD, 102)
        assertEquals(2, context.engine.getSession(accepted.session.id)!!.reviewItemsReviewed)
        assertEquals(itemIds.first(), context.engine.requireStudyQueueProgress(accepted.session.id).currentLearningItemId)
        reviewCurrent("coverage-first-retry", ReviewRating.HARD, 103)
        assertEquals(2, context.engine.getSession(accepted.session.id)!!.reviewItemsReviewed)
        reviewCurrent("coverage-third", ReviewRating.EASY, 104)

        val completedSession = context.engine.getSession(accepted.session.id)!!
        val completedQueue = context.engine.requireStudyQueueProgress(accepted.session.id)
        assertEquals(3, completedSession.reviewItemsReviewed)
        assertEquals(3, completedSession.reviewedContentIds.size)
        assertTrue(completedQueue.isCompleted)
        assertEquals(4, completedQueue.completedItemCount)
    }

    private fun fixture(): Fixture {
        val sessions = InMemoryStudySessionRepository()
        val queueRepository = InMemoryStudyQueueRepository()
        val queues = StudyQueueService(queueRepository)
        val items = InMemoryLearningItemRepository()
        val memories = InMemoryMemoryStateRepository()
        val availability = LearnEntryReviewAvailabilityQuery(
            sessions,
            queues,
            items,
            memories,
            InMemoryReviewEventRepository(),
            null
        )
        return Fixture(
            sessions,
            queues,
            items,
            memories,
            availability,
            StartLearnedItemsReviewUseCase(sessions, queues, availability)
        )
    }

    private fun reviewedState(itemId: LearningItemId, lastReviewedAt: Long): MemoryState =
        MemoryState.new(learner, itemId, Moment(0)).copy(
            stage = LearningStage.REVIEW,
            dueAt = Moment(50),
            lastReviewedAt = Moment(lastReviewedAt),
            reviewCount = 1
        )

    private fun saveFinished(
        fixture: Fixture,
        id: String,
        owner: LearnerId,
        installedPackageId: InstalledPackageId,
        topic: TopicId,
        item: LearningItem,
        finishedAt: Long
    ) {
        val sessionId = SessionId(id)
        val session = StudySession.start(
            sessionId,
            owner,
            Moment(1),
            SessionPolicy(0, 5),
            contentIds.toSet(),
            topic,
            installedPackageId
        ).copy(
            reviewedItemIds = setOf(item.id),
            reviewedContentIds = setOf(item.contentId),
            reviewItemsReviewed = 1
        ).finish(Moment(finishedAt))
        fixture.sessions.save(session)
        fixture.queues.create(
            sessionId,
            Moment(1),
            listOf(item.id),
            mapOf(item.id to SessionItemOrigin.REVIEW),
            mapOf(item.id to item.contentId),
            configuredReviewTarget = 1,
            effectiveReviewWorkload = 1
        )
        fixture.queues.advance(sessionId)
    }

    private data class Fixture(
        val sessions: InMemoryStudySessionRepository,
        val queues: StudyQueueService,
        val items: InMemoryLearningItemRepository,
        val memories: InMemoryMemoryStateRepository,
        val availability: LearnEntryReviewAvailabilityQuery,
        val start: StartLearnedItemsReviewUseCase
    )
}
