package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.infrastructure.persistence.mapper.ReviewEventRecordMapper
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord
import vn.loi.learning.testing.fixtures.ReviewFixtures

class JsonReviewEventStoreTest {

    @Test
    fun `loadAll returns empty list when file does not exist`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("review-events.json")

            val store =
                JsonReviewEventStore(filePath)

            assertTrue(
                store.loadAll().isEmpty()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `loadAll returns empty list when file is blank`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("review-events.json")

            Files.writeString(
                filePath,
                "   "
            )

            val store =
                JsonReviewEventStore(filePath)

            assertTrue(
                store.loadAll().isEmpty()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `saveAll writes records to json file`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("review-events.json")

            val record =
                createRecord(
                    id = ReviewEventId("review-1"),
                    learnerId = LearnerId("learner-1"),
                    learningItemId = LearningItemId("item-1")
                )

            val store =
                JsonReviewEventStore(filePath)

            store.saveAll(
                listOf(record)
            )

            assertTrue(filePath.exists())

            assertEquals(
                expected = listOf(record),
                actual = store.loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `records survive creating a new store instance`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("review-events.json")

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

            JsonReviewEventStore(filePath).saveAll(
                listOf(
                    firstRecord,
                    secondRecord
                )
            )

            val reopenedStore =
                JsonReviewEventStore(filePath)

            assertEquals(
                expected = listOf(
                    firstRecord,
                    secondRecord
                ),
                actual = reopenedStore.loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `saveAll replaces previous file snapshot`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("review-events.json")

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

            val store =
                JsonReviewEventStore(filePath)

            store.saveAll(
                listOf(firstRecord)
            )

            store.saveAll(
                listOf(secondRecord)
            )

            assertEquals(
                expected = listOf(secondRecord),
                actual =
                    JsonReviewEventStore(filePath).loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
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

    private fun deleteDirectoryRecursively(
        directory: java.nio.file.Path
    ) {
        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::deleteIfExists)
    }
}