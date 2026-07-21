package vn.loi.learning.infrastructure.persistence.repository

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.json.JsonStudyQueueStore

class StoreBackedStudyQueueRepositoryTest {

    @Test
    fun `queue persists advances across repository instances`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val filePath =
                directory.resolve("study-queues.json")

            val sessionId =
                SessionId("session-1")

            val original =
                StudyQueueSnapshot.create(
                    sessionId = sessionId,
                    createdAt = Moment(1_000L),
                    learningItemIds =
                        listOf(
                            LearningItemId("item-1"),
                            LearningItemId("item-2")
                        )
                )

            val firstRepository =
                StoreBackedStudyQueueRepository(
                    store = JsonStudyQueueStore(filePath)
                )

            firstRepository.save(
                original.advance()
            )

            val reopenedRepository =
                StoreBackedStudyQueueRepository(
                    store = JsonStudyQueueStore(filePath)
                )

            assertEquals(
                expected = 1,
                actual =
                    reopenedRepository
                        .findBySessionId(sessionId)
                        ?.currentIndex
            )

            assertEquals(
                expected = LearningItemId("item-2"),
                actual =
                    reopenedRepository
                        .findBySessionId(sessionId)
                        ?.currentLearningItemId
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    @Test
    fun `delete removes only selected queue`() {
        val directory =
            Files.createTempDirectory("learning-engine-test")

        try {
            val repository =
                StoreBackedStudyQueueRepository(
                    store =
                        JsonStudyQueueStore(
                            directory.resolve(
                                "study-queues.json"
                            )
                        )
                )

            val first =
                createSnapshot("session-1")

            val second =
                createSnapshot("session-2")

            repository.save(first)
            repository.save(second)
            repository.deleteBySessionId(first.sessionId)

            assertNull(
                repository.findBySessionId(first.sessionId)
            )

            assertEquals(
                expected = second,
                actual =
                    repository.findBySessionId(
                        second.sessionId
                    )
            )
        } finally {
            deleteDirectoryRecursively(directory)
        }
    }

    private fun createSnapshot(
        sessionId: String
    ): StudyQueueSnapshot =
        StudyQueueSnapshot.create(
            sessionId = SessionId(sessionId),
            createdAt = Moment(1_000L),
            learningItemIds =
                listOf(
                    LearningItemId("item-$sessionId")
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