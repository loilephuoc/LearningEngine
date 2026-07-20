package vn.loi.learning.infrastructure.persistence.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.infrastructure.persistence.mapper.ReviewEventRecordMapper
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord
import vn.loi.learning.testing.fixtures.ReviewFixtures

class ReviewEventStoreContractTest {

    private val store: ReviewEventStore =
        InMemoryReviewEventStore()

    @Test
    fun `loadAll returns empty list when store has no records`() {
        val records =
            store.loadAll()

        assertTrue(records.isEmpty())
    }

    @Test
    fun `saveAll stores records`() {
        val record =
            createRecord(
                id = ReviewEventId("review-1"),
                learnerId = LearnerId("learner-1"),
                learningItemId = LearningItemId("item-1")
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
        val firstRecord =
            createRecord(
                id = ReviewEventId("review-1"),
                learnerId = LearnerId("learner-1"),
                learningItemId = LearningItemId("item-1")
            )

        val secondRecord =
            createRecord(
                id = ReviewEventId("review-2"),
                learnerId = LearnerId("learner-2"),
                learningItemId = LearningItemId("item-2")
            )

        store.saveAll(
            listOf(firstRecord)
        )

        store.saveAll(
            listOf(secondRecord)
        )

        assertEquals(
            expected = listOf(secondRecord),
            actual = store.loadAll()
        )
    }

    @Test
    fun `saveAll copies input list instead of keeping mutable reference`() {
        val firstRecord =
            createRecord(
                id = ReviewEventId("review-1"),
                learnerId = LearnerId("learner-1"),
                learningItemId = LearningItemId("item-1")
            )

        val secondRecord =
            createRecord(
                id = ReviewEventId("review-2"),
                learnerId = LearnerId("learner-2"),
                learningItemId = LearningItemId("item-2")
            )

        val mutableInput =
            mutableListOf(firstRecord)

        store.saveAll(mutableInput)

        mutableInput += secondRecord

        assertEquals(
            expected = listOf(firstRecord),
            actual = store.loadAll()
        )
    }

    @Test
    fun `changing loaded mutable copy does not change stored snapshot`() {
        val firstRecord =
            createRecord(
                id = ReviewEventId("review-1"),
                learnerId = LearnerId("learner-1"),
                learningItemId = LearningItemId("item-1")
            )

        val secondRecord =
            createRecord(
                id = ReviewEventId("review-2"),
                learnerId = LearnerId("learner-2"),
                learningItemId = LearningItemId("item-2")
            )

        store.saveAll(
            listOf(firstRecord)
        )

        val loadedCopy =
            store.loadAll().toMutableList()

        loadedCopy += secondRecord

        assertEquals(
            expected = listOf(firstRecord),
            actual = store.loadAll()
        )
    }

    private fun createRecord(
        id: ReviewEventId,
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): ReviewEventRecord =
        ReviewEventRecordMapper.toRecord(
            ReviewFixtures.event(
                id = id,
                learnerId = learnerId,
                learningItemId = learningItemId
            )
        )
}