package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.persistence.mapper.MemoryStateRecordMapper
import vn.loi.learning.testing.fixtures.MemoryFixtures

class JsonMemoryStateStoreTest {

    @Test
    fun `load returns empty list when file does not exist`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("memory-states.json")

            val store =
                JsonMemoryStateStore(filePath)

            assertTrue(
                store.load().isEmpty()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `save writes records to json file`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("memory-states.json")

            val record =
                MemoryStateRecordMapper.toRecord(
                    MemoryFixtures.newState(
                        learnerId = LearnerId("learner-1"),
                        learningItemId = LearningItemId("item-1"),
                        availableAt = Moment(1_000L)
                    )
                )

            val store =
                JsonMemoryStateStore(filePath)

            store.save(listOf(record))

            assertTrue(filePath.exists())

            assertEquals(
                expected = listOf(record),
                actual = store.load()
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
                directory.resolve("memory-states.json")

            val firstRecord =
                MemoryStateRecordMapper.toRecord(
                    MemoryFixtures.newState(
                        learnerId = LearnerId("learner-1"),
                        learningItemId = LearningItemId("item-1"),
                        availableAt = Moment(1_000L)
                    )
                )

            val secondRecord =
                MemoryStateRecordMapper.toRecord(
                    MemoryFixtures.newState(
                        learnerId = LearnerId("learner-2"),
                        learningItemId = LearningItemId("item-2"),
                        availableAt = Moment(2_000L)
                    )
                )

            JsonMemoryStateStore(filePath).save(
                listOf(
                    firstRecord,
                    secondRecord
                )
            )

            val reopenedStore =
                JsonMemoryStateStore(filePath)

            assertEquals(
                expected = listOf(
                    firstRecord,
                    secondRecord
                ),
                actual = reopenedStore.load()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `save replaces previous file snapshot`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("memory-states.json")

            val firstRecord =
                MemoryStateRecordMapper.toRecord(
                    MemoryFixtures.newState(
                        learnerId = LearnerId("learner-1"),
                        learningItemId = LearningItemId("item-1"),
                        availableAt = Moment(1_000L)
                    )
                )

            val secondRecord =
                MemoryStateRecordMapper.toRecord(
                    MemoryFixtures.newState(
                        learnerId = LearnerId("learner-2"),
                        learningItemId = LearningItemId("item-2"),
                        availableAt = Moment(2_000L)
                    )
                )

            val store =
                JsonMemoryStateStore(filePath)

            store.save(listOf(firstRecord))
            store.save(listOf(secondRecord))

            assertEquals(
                expected = listOf(secondRecord),
                actual = JsonMemoryStateStore(filePath).load()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    private fun deleteDirectoryRecursively(
        directory: java.nio.file.Path
    ) {
        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::deleteIfExists)
    }
}


