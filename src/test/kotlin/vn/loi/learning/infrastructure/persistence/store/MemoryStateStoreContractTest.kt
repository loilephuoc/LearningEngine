package vn.loi.learning.infrastructure.persistence.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.persistence.mapper.MemoryStateRecordMapper
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.testing.fixtures.MemoryFixtures

class MemoryStateStoreContractTest {

    private val store: MemoryStateStore =
        InMemoryMemoryStateStore()

    @Test
    fun `load returns empty list when store has no records`() {
        val records = store.load()

        assertTrue(records.isEmpty())
    }

    @Test
    fun `save stores records`() {
        val record = createRecord(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1")
        )

        store.save(listOf(record))

        assertEquals(
            expected = listOf(record),
            actual = store.load()
        )
    }

    @Test
    fun `save replaces previous snapshot`() {
        val firstRecord = createRecord(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1")
        )

        val secondRecord = createRecord(
            learnerId = LearnerId("learner-2"),
            learningItemId = LearningItemId("item-2")
        )

        store.save(listOf(firstRecord))
        store.save(listOf(secondRecord))

        assertEquals(
            expected = listOf(secondRecord),
            actual = store.load()
        )
    }

    @Test
    fun `save copies input list instead of keeping mutable reference`() {
        val firstRecord = createRecord(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1")
        )

        val secondRecord = createRecord(
            learnerId = LearnerId("learner-2"),
            learningItemId = LearningItemId("item-2")
        )

        val mutableInput =
            mutableListOf(firstRecord)

        store.save(mutableInput)

        mutableInput += secondRecord

        assertEquals(
            expected = listOf(firstRecord),
            actual = store.load()
        )
    }

    @Test
    fun `changing loaded mutable copy does not change stored snapshot`() {
        val firstRecord = createRecord(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1")
        )

        val secondRecord = createRecord(
            learnerId = LearnerId("learner-2"),
            learningItemId = LearningItemId("item-2")
        )

        store.save(listOf(firstRecord))

        val loadedCopy =
            store.load().toMutableList()

        loadedCopy += secondRecord

        assertEquals(
            expected = listOf(firstRecord),
            actual = store.load()
        )
    }

    private fun createRecord(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryStateRecord =
        MemoryStateRecordMapper.toRecord(
            MemoryFixtures.newState(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = Moment(1_000L)
            )
        )
}