package vn.loi.learning.infrastructure.persistence.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.mapper.StudySessionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

class StudySessionStoreContractTest {

    private val store: StudySessionStore =
        InMemoryStudySessionStore()

    @Test
    fun `loadAll returns empty list when store has no records`() {
        assertTrue(
            store.loadAll().isEmpty()
        )
    }

    @Test
    fun `saveAll stores records`() {
        val record =
            createRecord(
                SessionId("session-1")
            )

        store.saveAll(
            listOf(record)
        )

        assertEquals(
            expected = listOf(record),
            actual = store.loadAll()
        )
    }

    @Test
    fun `saveAll replaces previous snapshot`() {
        val first =
            createRecord(
                SessionId("session-1")
            )

        val second =
            createRecord(
                SessionId("session-2")
            )

        store.saveAll(
            listOf(first)
        )

        store.saveAll(
            listOf(second)
        )

        assertEquals(
            expected = listOf(second),
            actual = store.loadAll()
        )
    }

    @Test
    fun `saveAll copies input list`() {
        val first =
            createRecord(
                SessionId("session-1")
            )

        val second =
            createRecord(
                SessionId("session-2")
            )

        val input =
            mutableListOf(first)

        store.saveAll(input)

        input += second

        assertEquals(
            expected = listOf(first),
            actual = store.loadAll()
        )
    }

    @Test
    fun `changing loaded mutable copy does not change stored snapshot`() {
        val first =
            createRecord(
                SessionId("session-1")
            )

        val second =
            createRecord(
                SessionId("session-2")
            )

        store.saveAll(
            listOf(first)
        )

        val loaded =
            store.loadAll().toMutableList()

        loaded += second

        assertEquals(
            expected = listOf(first),
            actual = store.loadAll()
        )
    }

    private fun createRecord(
        sessionId: SessionId
    ): StudySessionRecord =
        StudySessionRecordMapper.toRecord(
            StudySession.start(
                id = sessionId,
                learnerId = LearnerId("learner-1"),
                startedAt = Moment(1_000L),
                policy = SessionPolicy(
                    newItemLimit = 10,
                    reviewItemLimit = 10
                )
            )
        )
}