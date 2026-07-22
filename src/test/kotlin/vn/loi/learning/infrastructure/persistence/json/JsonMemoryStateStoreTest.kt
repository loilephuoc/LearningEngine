package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.persistence.mapper.MemoryStateRecordMapper
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.testing.fixtures.MemoryFixtures

class JsonMemoryStateStoreTest {

    @Test
    fun `large deterministic snapshot survives store recreation`() {
        val directory =
            Files.createTempDirectory("learning-engine-large-state-test")

        try {
            val filePath = directory.resolve("memory-states.json")
            val recordCount = 5_000

            val records =
                List(recordCount) { index ->
                    MemoryStateRecord(
                        schemaVersion = MemoryStateRecord.CURRENT_SCHEMA_VERSION,
                        learnerId = "learner-${index % 25}",
                        learningItemId = "item-$index",
                        stage = if (index % 2 == 0) "REVIEW" else "LEARNING",
                        difficulty = 1.0 + (index % 900) / 100.0,
                        stabilityDays = 0.5 + index / 10.0,
                        dueAtEpochMillis = 1_000_000L + index,
                        lastReviewedAtEpochMillis =
                            if (index % 3 == 0) null else 900_000L + index,
                        reviewCount = index % 100,
                        lapseCount = index % 7
                    )
                }

            JsonMemoryStateStore(filePath).save(records)

            val restored = JsonMemoryStateStore(filePath).load()

            assertEquals(recordCount, restored.size)
            assertEquals(records.first(), restored.first())
            assertEquals(records[recordCount / 2], restored[recordCount / 2])
            assertEquals(records.last(), restored.last())
            assertEquals(records, restored)
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `corrupt snapshot remains unchanged across store restarts`() {
        val directory =
            Files.createTempDirectory("learning-engine-corrupt-restart-test")

        try {
            val filePath =
                directory.resolve("memory-states.json")

            val corruptBytes =
                "{\"privateNote\":\"do-not-log\"".toByteArray()

            Files.write(
                filePath,
                corruptBytes
            )

            val modifiedBefore =
                Files.getLastModifiedTime(filePath)

            repeat(2) {
                val failure =
                    assertFailsWith<InvalidJsonPersistenceException> {
                        JsonMemoryStateStore(filePath).load()
                    }

                assertEquals(
                    JsonPersistenceFailureKind.TRUNCATED,
                    failure.failureKind
                )

                assertEquals(
                    "memory state",
                    failure.recordType
                )

                assertFalse(
                    failure.message.orEmpty().contains("do-not-log")
                )
            }

            assertContentEquals(
                corruptBytes,
                Files.readAllBytes(filePath)
            )

            assertEquals(
                modifiedBefore,
                Files.getLastModifiedTime(filePath)
            )

            assertEquals(
                listOf("memory-states.json"),
                Files.list(directory).use { files ->
                    files.map { it.fileName.toString() }.toList()
                }
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

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


