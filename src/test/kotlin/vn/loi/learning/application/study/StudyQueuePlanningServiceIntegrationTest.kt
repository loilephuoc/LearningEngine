package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
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
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository

class StudyQueuePlanningServiceIntegrationTest {

    @Test
    fun `session seed randomizes the new subset before policy limiting`() {
        val contentRepository = InMemoryContentRepository()
        val itemRepository = InMemoryLearningItemRepository()
        val memoryRepository = InMemoryMemoryStateRepository()
        (1..20).forEach { registerItem(contentRepository, itemRepository, it) }
        val planningService = StudyQueuePlanningService(
            StudyQueuePlanner(contentRepository, itemRepository, memoryRepository)
        )
        fun plan(sessionId: String) = planningService.plan(
            StudySession.start(
                id = SessionId(sessionId),
                learnerId = LearnerId("learner-1"),
                startedAt = Moment(1_000L),
                policy = SessionPolicy(newItemLimit = 4, reviewItemLimit = 0)
            )
        ).learningItemIds

        val first = plan("seeded-session-a")

        assertEquals(first, plan("seeded-session-a"))
        assertEquals(4, first.size)
        assertTrue(first.toSet() != plan("seeded-session-b").toSet())
    }

    @Test
    fun `service creates plan from active session scope`() {
        val contentRepository =
            InMemoryContentRepository()

        val itemRepository =
            InMemoryLearningItemRepository()

        val memoryRepository =
            InMemoryMemoryStateRepository()

        registerItem(
            contentRepository =
                contentRepository,
            itemRepository =
                itemRepository,
            number = 1
        )

        registerItem(
            contentRepository =
                contentRepository,
            itemRepository =
                itemRepository,
            number = 2
        )

        registerItem(
            contentRepository =
                contentRepository,
            itemRepository =
                itemRepository,
            number = 3
        )

        val planningService =
            StudyQueuePlanningService(
                StudyQueuePlanner(
                    contentRepository =
                        contentRepository,
                    learningItemRepository =
                        itemRepository,
                    memoryStateRepository =
                        memoryRepository
                )
            )

        val session =
            StudySession.start(
                id =
                    SessionId("session-1"),
                learnerId =
                    LearnerId("learner-1"),
                startedAt =
                    Moment(1_000L),
                policy =
                    SessionPolicy(
                        newItemLimit = 3,
                        reviewItemLimit = 3
                    ),
                includedContentIds =
                    setOf(
                        ContentId("content-1"),
                        ContentId("content-3")
                    )
            )

        val plan =
            planningService.plan(
                session
            )

        assertEquals(
            expected =
                session.id,
            actual =
                plan.sessionId
        )

        assertEquals(
            expected =
                session.startedAt,
            actual =
                plan.plannedAt
        )

        assertEquals(
            expected = 2,
            actual =
                plan.totalItemCount
        )

        assertTrue(
            LearningItemId("item-1") in
                    plan
        )

        assertTrue(
            LearningItemId("item-3") in
                    plan
        )

        assertTrue(
            LearningItemId("item-2") !in
                    plan
        )
    }

    @Test
    fun `service rejects finished session`() {
        val planningService =
            StudyQueuePlanningService(
                StudyQueuePlanner(
                    contentRepository =
                        InMemoryContentRepository(),
                    learningItemRepository =
                        InMemoryLearningItemRepository(),
                    memoryStateRepository =
                        InMemoryMemoryStateRepository()
                )
            )

        val finishedSession =
            StudySession.start(
                id =
                    SessionId("session-1"),
                learnerId =
                    LearnerId("learner-1"),
                startedAt =
                    Moment(1_000L),
                policy =
                    SessionPolicy(
                        newItemLimit = 1,
                        reviewItemLimit = 1
                    )
            )
                .finish(
                    Moment(2_000L)
                )

        assertFailsWith<
                IllegalArgumentException
                > {
            planningService.plan(
                finishedSession
            )
        }
    }

    private fun registerItem(
        contentRepository:
        InMemoryContentRepository,
        itemRepository:
        InMemoryLearningItemRepository,
        number: Int
    ) {
        val content =
            Content(
                id =
                    ContentId(
                        "content-$number"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Sentence $number"
                    )
            )

        contentRepository.save(
            content
        )

        itemRepository.save(
            LearningItem(
                id =
                    LearningItemId(
                        "item-$number"
                    ),
                contentId =
                    content.id,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )
        )
    }
}
