package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository

class StudyQueueRepositoryContractTest {

    private fun createRepository():
            StudyQueueRepository =
        InMemoryStudyQueueRepository()

    @Test
    fun `findBySessionId returns null when queue does not exist`() {
        val repository =
            createRepository()

        assertNull(
            repository.findBySessionId(
                SessionId(
                    "missing-session"
                )
            )
        )
    }

    @Test
    fun `saved queue can be found by session id`() {
        val repository =
            createRepository()

        val snapshot =
            createSnapshot(
                sessionId =
                    SessionId("session-1")
            )

        repository.save(snapshot)

        assertEquals(
            snapshot,
            repository.findBySessionId(
                snapshot.sessionId
            )
        )
    }

    @Test
    fun `saving same session id replaces previous snapshot`() {
        val repository =
            createRepository()

        val sessionId =
            SessionId("session-1")

        val original =
            createSnapshot(
                sessionId = sessionId
            )

        val advanced =
            original.advance()

        repository.save(original)
        repository.save(advanced)

        assertEquals(
            advanced,
            repository.findBySessionId(
                sessionId
            )
        )
    }

    @Test
    fun `queues from different sessions do not overwrite each other`() {
        val repository =
            createRepository()

        val first =
            createSnapshot(
                sessionId =
                    SessionId("session-1"),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-1"
                        )
                    )
            )

        val second =
            createSnapshot(
                sessionId =
                    SessionId("session-2"),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-2"
                        )
                    )
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            first,
            repository.findBySessionId(
                first.sessionId
            )
        )

        assertEquals(
            second,
            repository.findBySessionId(
                second.sessionId
            )
        )
    }

    @Test
    fun `deleteBySessionId removes selected queue`() {
        val repository =
            createRepository()

        val first =
            createSnapshot(
                sessionId =
                    SessionId("session-1")
            )

        val preserved =
            createSnapshot(
                sessionId =
                    SessionId("session-2")
            )

        repository.save(first)
        repository.save(preserved)

        repository.deleteBySessionId(
            first.sessionId
        )

        assertNull(
            repository.findBySessionId(
                first.sessionId
            )
        )

        assertEquals(
            preserved,
            repository.findBySessionId(
                preserved.sessionId
            )
        )
    }

    @Test
    fun `deleteBySessionId accepts missing session`() {
        val repository =
            createRepository()

        val preserved =
            createSnapshot(
                sessionId =
                    SessionId("session-1")
            )

        repository.save(preserved)

        repository.deleteBySessionId(
            SessionId(
                "missing-session"
            )
        )

        assertEquals(
            preserved,
            repository.findBySessionId(
                preserved.sessionId
            )
        )
    }

    private fun createSnapshot(
        sessionId: SessionId,
        learningItemIds:
        List<LearningItemId> =
            listOf(
                LearningItemId("item-1"),
                LearningItemId("item-2")
            )
    ): StudyQueueSnapshot =
        StudyQueueSnapshot.create(
            sessionId = sessionId,
            createdAt =
                Moment(1_000L),
            learningItemIds =
                learningItemIds
        )
}