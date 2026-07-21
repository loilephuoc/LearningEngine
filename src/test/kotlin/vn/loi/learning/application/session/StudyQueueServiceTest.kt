package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository

class StudyQueueServiceTest {

    @Test
    fun `create persists and returns queue snapshot`() {
        val repository =
            InMemoryStudyQueueRepository()

        val service =
            StudyQueueService(
                repository
            )

        val sessionId =
            SessionId("session-1")

        val snapshot =
            service.create(
                sessionId = sessionId,
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-1"
                        ),
                        LearningItemId(
                            "item-2"
                        )
                    )
            )

        assertEquals(
            snapshot,
            repository.findBySessionId(
                sessionId
            )
        )

        assertEquals(
            LearningItemId("item-1"),
            snapshot.currentLearningItemId
        )

        assertEquals(
            2,
            snapshot.totalItemCount
        )
    }

    @Test
    fun `create rejects second queue for same session`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        val sessionId =
            SessionId("session-1")

        service.create(
            sessionId = sessionId,
            createdAt =
                Moment(1_000L),
            learningItemIds =
                listOf(
                    LearningItemId(
                        "item-1"
                    )
                )
        )

        assertFailsWith<
                IllegalArgumentException
                > {
            service.create(
                sessionId = sessionId,
                createdAt =
                    Moment(2_000L),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-2"
                        )
                    )
            )
        }
    }

    @Test
    fun `get returns persisted queue`() {
        val repository =
            InMemoryStudyQueueRepository()

        val service =
            StudyQueueService(
                repository
            )

        val sessionId =
            SessionId("session-1")

        val created =
            service.create(
                sessionId = sessionId,
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-1"
                        )
                    )
            )

        assertSame(
            created,
            service.get(sessionId)
        )
    }

    @Test
    fun `get returns null when queue does not exist`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        assertNull(
            service.get(
                SessionId(
                    "missing-session"
                )
            )
        )
    }

    @Test
    fun `require returns queue when it exists`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        val sessionId =
            SessionId("session-1")

        val created =
            service.create(
                sessionId = sessionId,
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-1"
                        )
                    )
            )

        assertEquals(
            created,
            service.require(sessionId)
        )
    }

    @Test
    fun `require fails when queue does not exist`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        assertFailsWith<
                IllegalArgumentException
                > {
            service.require(
                SessionId(
                    "missing-session"
                )
            )
        }
    }

    @Test
    fun `advance persists next queue position`() {
        val repository =
            InMemoryStudyQueueRepository()

        val service =
            StudyQueueService(
                repository
            )

        val sessionId =
            SessionId("session-1")

        service.create(
            sessionId = sessionId,
            createdAt =
                Moment(1_000L),
            learningItemIds =
                listOf(
                    LearningItemId(
                        "item-1"
                    ),
                    LearningItemId(
                        "item-2"
                    )
                )
        )

        val advanced =
            service.advance(
                sessionId
            )

        assertEquals(
            1,
            advanced.currentIndex
        )

        assertEquals(
            LearningItemId("item-2"),
            advanced.currentLearningItemId
        )

        assertEquals(
            advanced,
            repository.findBySessionId(
                sessionId
            )
        )
    }

    @Test
    fun `advance fails when queue does not exist`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        assertFailsWith<
                IllegalArgumentException
                > {
            service.advance(
                SessionId(
                    "missing-session"
                )
            )
        }
    }

    @Test
    fun `advance completes queue after final item`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        val sessionId =
            SessionId("session-1")

        service.create(
            sessionId = sessionId,
            createdAt =
                Moment(1_000L),
            learningItemIds =
                listOf(
                    LearningItemId(
                        "item-1"
                    )
                )
        )

        val completed =
            service.advance(
                sessionId
            )

        assertTrue(
            completed.isCompleted
        )

        assertNull(
            completed.currentLearningItemId
        )
    }

    @Test
    fun `delete removes persisted queue`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        val sessionId =
            SessionId("session-1")

        service.create(
            sessionId = sessionId,
            createdAt =
                Moment(1_000L),
            learningItemIds =
                listOf(
                    LearningItemId(
                        "item-1"
                    )
                )
        )

        service.delete(sessionId)

        assertNull(
            service.get(sessionId)
        )
    }

    @Test
    fun `delete accepts missing queue`() {
        val service =
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )

        val sessionId =
            SessionId("missing-session")

        service.delete(sessionId)

        assertNull(
            service.get(sessionId)
        )
    }
}