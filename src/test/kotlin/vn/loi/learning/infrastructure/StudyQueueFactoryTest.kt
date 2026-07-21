package vn.loi.learning.infrastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository

class StudyQueueFactoryTest {

    @Test
    fun `createInMemory creates usable isolated queue service`() {
        val firstService =
            StudyQueueFactory
                .createInMemory()

        val secondService =
            StudyQueueFactory
                .createInMemory()

        val sessionId =
            SessionId("session-1")

        val snapshot =
            firstService.create(
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
            snapshot,
            firstService.get(sessionId)
        )

        assertNull(
            secondService.get(sessionId)
        )
    }

    @Test
    fun `create uses supplied repository`() {
        val repository =
            InMemoryStudyQueueRepository()

        val service =
            StudyQueueFactory.create(
                repository = repository
            )

        val snapshot =
            StudyQueueSnapshot.create(
                sessionId =
                    SessionId("session-1"),
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

        repository.save(snapshot)

        assertEquals(
            snapshot,
            service.get(
                snapshot.sessionId
            )
        )
    }

    @Test
    fun `service created with supplied repository persists advances`() {
        val repository =
            InMemoryStudyQueueRepository()

        val service =
            StudyQueueFactory.create(
                repository = repository
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
            service.advance(sessionId)

        assertEquals(
            advanced,
            repository.findBySessionId(
                sessionId
            )
        )

        assertEquals(
            LearningItemId("item-2"),
            advanced.currentLearningItemId
        )
    }
}