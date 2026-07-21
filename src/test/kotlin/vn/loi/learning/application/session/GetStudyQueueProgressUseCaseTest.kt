package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository

class GetStudyQueueProgressUseCaseTest {

    @Test
    fun `execute returns null when queue does not exist`() {
        val useCase =
            createUseCase()

        assertNull(
            useCase.execute(
                SessionId(
                    "missing-session"
                )
            )
        )
    }

    @Test
    fun `execute returns current persisted queue progress`() {
        val repository =
            InMemoryStudyQueueRepository()

        val service =
            StudyQueueService(
                repository
            )

        val useCase =
            GetStudyQueueProgressUseCase(
                service
            )

        val sessionId =
            SessionId("session-1")

        service.create(
            sessionId = sessionId,
            createdAt =
                Moment(1_000L),
            learningItemIds =
                listOf(
                    LearningItemId("item-1"),
                    LearningItemId("item-2"),
                    LearningItemId("item-3")
                )
        )

        service.advance(
            sessionId
        )

        val progress =
            assertNotNull(
                useCase.execute(
                    sessionId
                )
            )

        assertEquals(
            expected = 1,
            actual =
                progress.currentIndex
        )

        assertEquals(
            expected = 1,
            actual =
                progress.completedItemCount
        )

        assertEquals(
            expected = 2,
            actual =
                progress.remainingItemCount
        )

        assertEquals(
            expected =
                LearningItemId("item-2"),
            actual =
                progress
                    .currentLearningItemId
        )

        assertEquals(
            expected =
                LearningItemId("item-1"),
            actual =
                progress
                    .previousLearningItemId
        )

        assertEquals(
            expected =
                LearningItemId("item-3"),
            actual =
                progress
                    .nextLearningItemId
        )

        assertEquals(
            expected = 33,
            actual =
                progress.percentComplete
        )
    }

    @Test
    fun `require rejects missing queue`() {
        val useCase =
            createUseCase()

        assertFailsWith<
                IllegalArgumentException
                > {
            useCase.require(
                SessionId(
                    "missing-session"
                )
            )
        }
    }

    @Test
    fun `query does not advance or modify persisted queue`() {
        val repository =
            InMemoryStudyQueueRepository()

        val service =
            StudyQueueService(
                repository
            )

        val useCase =
            GetStudyQueueProgressUseCase(
                service
            )

        val sessionId =
            SessionId("session-1")

        service.create(
            sessionId = sessionId,
            createdAt =
                Moment(1_000L),
            learningItemIds =
                listOf(
                    LearningItemId("item-1"),
                    LearningItemId("item-2")
                )
        )

        val firstRead =
            useCase.require(
                sessionId
            )

        val secondRead =
            useCase.require(
                sessionId
            )

        val persisted =
            service.require(
                sessionId
            )

        assertEquals(
            expected = 0,
            actual =
                firstRead.currentIndex
        )

        assertEquals(
            expected = firstRead,
            actual = secondRead
        )

        assertEquals(
            expected = 0,
            actual =
                persisted.currentIndex
        )

        assertEquals(
            expected =
                LearningItemId("item-1"),
            actual =
                persisted
                    .currentLearningItemId
        )
    }

    private fun createUseCase():
            GetStudyQueueProgressUseCase =
        GetStudyQueueProgressUseCase(
            StudyQueueService(
                InMemoryStudyQueueRepository()
            )
        )
}