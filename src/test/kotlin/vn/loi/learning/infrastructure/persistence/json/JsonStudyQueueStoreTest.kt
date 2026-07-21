package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.infrastructure.persistence.record.StudyQueueRecord

class JsonStudyQueueStoreTest {

    @Test
    fun `loadAll returns empty list when file does not exist`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-queues.json")

            val store =
                JsonStudyQueueStore(filePath)

            assertTrue(store.loadAll().isEmpty())
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `saved records survive creating a new store instance`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-queues.json")

            val records =
                listOf(
                    createRecord("session-1", 0),
                    createRecord("session-2", 1)
                )

            JsonStudyQueueStore(filePath)
                .saveAll(records)

            assertTrue(filePath.exists())

            assertEquals(
                expected = records,
                actual =
                    JsonStudyQueueStore(filePath)
                        .loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `saveAll replaces previous snapshot`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-queues.json")

            val store =
                JsonStudyQueueStore(filePath)

            store.saveAll(
                listOf(createRecord("session-1", 0))
            )

            val replacement =
                listOf(createRecord("session-2", 1))

            store.saveAll(replacement)

            assertEquals(
                expected = replacement,
                actual = store.loadAll()
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    private fun createRecord(
        sessionId: String,
        currentIndex: Int
    ): StudyQueueRecord =
        StudyQueueRecord(
            schemaVersion =
                StudyQueueRecord.CURRENT_SCHEMA_VERSION,
            sessionId = sessionId,
            createdAtEpochMillis = 1_000L,
            learningItemIds =
                listOf("item-1", "item-2"),
            currentIndex = currentIndex
        )

    private fun deleteDirectoryRecursively(
        directory: java.nio.file.Path
    ) {
        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::deleteIfExists)
    }
}