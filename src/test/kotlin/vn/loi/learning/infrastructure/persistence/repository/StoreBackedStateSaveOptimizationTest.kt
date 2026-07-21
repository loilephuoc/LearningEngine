package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord
import vn.loi.learning.infrastructure.persistence.store.MemoryStateStore
import vn.loi.learning.infrastructure.persistence.store.StudySessionStore
import vn.loi.learning.testing.fixtures.MemoryFixtures

class StoreBackedStateSaveOptimizationTest {

    @Test
    fun `saving identical memory state does not write store again`() {
        val store =
            CountingMemoryStateStore()

        val repository =
            StoreBackedMemoryStateRepository(
                store
            )

        val state =
            MemoryFixtures.newState(
                learnerId = LearnerId("learner-1"),
                learningItemId = LearningItemId("item-1"),
                availableAt = Moment(1_000L)
            )

        repository.save(
            state
        )

        repository.save(
            state
        )

        assertEquals(
            expected = 1,
            actual = store.saveCount
        )
    }

    @Test
    fun `changing memory state writes store once more`() {
        val store =
            CountingMemoryStateStore()

        val repository =
            StoreBackedMemoryStateRepository(
                store
            )

        val learnerId =
            LearnerId("learner-1")

        val learningItemId =
            LearningItemId("item-1")

        val initialState =
            MemoryFixtures.newState(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = Moment(1_000L)
            )

        val reviewedState =
            MemoryState(
                learnerId = learnerId,
                learningItemId = learningItemId,
                stage = LearningStage.REVIEW,
                difficulty = 4.5,
                stabilityDays = 3.0,
                dueAt = Moment(2_000L),
                lastReviewedAt = Moment(1_500L),
                reviewCount = 1,
                lapseCount = 0
            )

        repository.save(
            initialState
        )

        repository.save(
            reviewedState
        )

        assertEquals(
            expected = 2,
            actual = store.saveCount
        )
    }

    @Test
    fun `saving identical study session does not write store again`() {
        val store =
            CountingStudySessionStore()

        val repository =
            StoreBackedStudySessionRepository(
                store
            )

        val session =
            createSession()

        repository.save(
            session
        )

        repository.save(
            session
        )

        assertEquals(
            expected = 1,
            actual = store.saveCount
        )
    }

    @Test
    fun `changing study session writes store once more`() {
        val store =
            CountingStudySessionStore()

        val repository =
            StoreBackedStudySessionRepository(
                store
            )

        val initialSession =
            createSession()

        val finishedSession =
            initialSession.finish(
                at = Moment(2_000L)
            )

        repository.save(
            initialSession
        )

        repository.save(
            finishedSession
        )

        assertEquals(
            expected = 2,
            actual = store.saveCount
        )
    }

    private fun createSession(): StudySession =
        StudySession.start(
            id = SessionId("session-1"),
            learnerId = LearnerId("learner-1"),
            startedAt = Moment(1_000L),
            policy = SessionPolicy(
                newItemLimit = 10,
                reviewItemLimit = 10
            )
        )

    private class CountingMemoryStateStore :
        MemoryStateStore {

        private var records:
                List<MemoryStateRecord> =
            emptyList()

        var saveCount: Int = 0
            private set

        override fun load(): List<MemoryStateRecord> =
            records

        override fun save(
            records: List<MemoryStateRecord>
        ) {
            saveCount += 1
            this.records =
                records
        }
    }

    private class CountingStudySessionStore :
        StudySessionStore {

        private var records:
                List<StudySessionRecord> =
            emptyList()

        var saveCount: Int = 0
            private set

        override fun loadAll(): List<StudySessionRecord> =
            records

        override fun saveAll(
            records: List<StudySessionRecord>
        ) {
            saveCount += 1
            this.records =
                records
        }
    }
}