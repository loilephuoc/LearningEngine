package vn.loi.learning.infrastructure.persistence

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class PersistedStudyQueueLifecycleRestartTest {

    @Test
    fun `queue position survives restart and finish after restart deletes queue`() {
        val persistenceDirectory =
            Files.createTempDirectory(
                "persisted-study-queue-lifecycle"
            )

        try {
            val sessionId =
                SessionId("session-1")

            val learnerId =
                LearnerId("learner-1")

            val startedAt =
                Moment(1_000L)

            /*
             * Engine thứ nhất tạo session, tạo queue
             * và review item đầu tiên.
             */
            val firstEngine =
                createEngineWithItems(
                    persistenceDirectory =
                        persistenceDirectory,
                    itemCount = 3
                )

            firstEngine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId = learnerId,
                    startedAt = startedAt,
                    policy =
                        SessionPolicy(
                            newItemLimit = 3,
                            reviewItemLimit = 3,
                            allowRepeatInSameSession =
                                false
                        )
                )
            )

            val initialQueue =
                assertNotNull(
                    firstEngine.getStudyQueue(
                        sessionId
                    )
                )

            assertEquals(
                expected = 3,
                actual =
                    initialQueue.totalItemCount
            )

            assertEquals(
                expected = 0,
                actual =
                    initialQueue.currentIndex
            )

            val firstItem =
                assertNotNull(
                    firstEngine.getNextSessionItem(
                        sessionId = sessionId,
                        now = startedAt
                    )
                )

            assertEquals(
                expected =
                    initialQueue
                        .currentLearningItemId,
                actual =
                    firstItem.item
                        .learningItem.id
            )

            firstEngine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId =
                        ReviewEventId("review-1"),
                    learningItemId =
                        firstItem.item
                            .learningItem.id,
                    rating =
                        ReviewRating.GOOD,
                    reviewedAt =
                        startedAt
                )
            )

            val queueAfterReview =
                assertNotNull(
                    firstEngine.getStudyQueue(
                        sessionId
                    )
                )

            assertEquals(
                expected = 1,
                actual =
                    queueAfterReview.currentIndex
            )

            val expectedSecondItemId =
                assertNotNull(
                    queueAfterReview
                        .currentLearningItemId
                )

            /*
             * Engine thứ hai không dùng lại bất kỳ
             * in-memory repository nào của engine đầu.
             */
            val secondEngine =
                createEngineWithItems(
                    persistenceDirectory =
                        persistenceDirectory,
                    itemCount = 3
                )

            val restoredSession =
                assertNotNull(
                    secondEngine.getSession(
                        sessionId
                    )
                )

            assertEquals(
                expected = 1,
                actual =
                    restoredSession.totalReviews
            )

            assertEquals(
                expected =
                    SessionStatus.ACTIVE,
                actual =
                    restoredSession.status
            )

            val restoredQueue =
                assertNotNull(
                    secondEngine.getStudyQueue(
                        sessionId
                    )
                )

            assertEquals(
                expected =
                    initialQueue.learningItemIds,
                actual =
                    restoredQueue.learningItemIds
            )

            assertEquals(
                expected = 1,
                actual =
                    restoredQueue.currentIndex
            )

            assertEquals(
                expected =
                    expectedSecondItemId,
                actual =
                    restoredQueue
                        .currentLearningItemId
            )

            val restoredNextItem =
                assertNotNull(
                    secondEngine.getNextSessionItem(
                        sessionId = sessionId,
                        now = startedAt
                    )
                )

            assertEquals(
                expected =
                    expectedSecondItemId,
                actual =
                    restoredNextItem.item
                        .learningItem.id
            )

            /*
             * Finish bằng engine đã restart.
             * Session history còn tồn tại,
             * queue hoạt động phải bị xóa.
             */
            val finishedSession =
                secondEngine.finishSession(
                    sessionId = sessionId,
                    finishedAt =
                        Moment(2_000L)
                )

            assertEquals(
                expected =
                    SessionStatus.FINISHED,
                actual =
                    finishedSession.status
            )

            assertNull(
                secondEngine.getStudyQueue(
                    sessionId
                )
            )

            /*
             * Engine thứ ba xác nhận cả hai thay đổi
             * đều đã được persist:
             * session FINISHED và queue không tái xuất hiện.
             */
            val thirdEngine =
                createEngineWithItems(
                    persistenceDirectory =
                        persistenceDirectory,
                    itemCount = 3
                )

            val restoredFinishedSession =
                assertNotNull(
                    thirdEngine.getSession(
                        sessionId
                    )
                )

            assertEquals(
                expected =
                    SessionStatus.FINISHED,
                actual =
                    restoredFinishedSession.status
            )

            assertEquals(
                expected =
                    Moment(2_000L),
                actual =
                    restoredFinishedSession
                        .finishedAt
            )

            assertNull(
                thirdEngine.getStudyQueue(
                    sessionId
                )
            )
        } finally {
            persistenceDirectory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `completed queue remains completed after restart`() {
        val persistenceDirectory =
            Files.createTempDirectory(
                "persisted-completed-study-queue"
            )

        try {
            val sessionId =
                SessionId(
                    "completed-session"
                )

            val startedAt =
                Moment(1_000L)

            val firstEngine =
                createEngineWithItems(
                    persistenceDirectory =
                        persistenceDirectory,
                    itemCount = 1
                )

            firstEngine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId =
                        LearnerId("learner-1"),
                    startedAt = startedAt,
                    policy =
                        SessionPolicy(
                            newItemLimit = 1,
                            reviewItemLimit = 1
                        )
                )
            )

            val nextItem =
                assertNotNull(
                    firstEngine.getNextSessionItem(
                        sessionId = sessionId,
                        now = startedAt
                    )
                )

            firstEngine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId =
                        ReviewEventId("review-1"),
                    learningItemId =
                        nextItem.item
                            .learningItem.id,
                    rating =
                        ReviewRating.GOOD,
                    reviewedAt =
                        startedAt
                )
            )

            val completedQueue =
                assertNotNull(
                    firstEngine.getStudyQueue(
                        sessionId
                    )
                )

            assertTrue(
                completedQueue.isCompleted
            )

            assertEquals(
                expected = 1,
                actual =
                    completedQueue.currentIndex
            )

            assertNull(
                completedQueue
                    .currentLearningItemId
            )

            val secondEngine =
                createEngineWithItems(
                    persistenceDirectory =
                        persistenceDirectory,
                    itemCount = 1
                )

            val restoredQueue =
                assertNotNull(
                    secondEngine.getStudyQueue(
                        sessionId
                    )
                )

            assertTrue(
                restoredQueue.isCompleted
            )

            assertEquals(
                expected = 1,
                actual =
                    restoredQueue.currentIndex
            )

            assertEquals(
                expected = 0,
                actual =
                    restoredQueue
                        .remainingItemCount
            )

            assertNull(
                restoredQueue
                    .currentLearningItemId
            )

            assertNull(
                secondEngine.getNextSessionItem(
                    sessionId = sessionId,
                    now = startedAt
                )
            )
        } finally {
            persistenceDirectory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun createEngineWithItems(
        persistenceDirectory: Path,
        itemCount: Int
    ): LearningEngine {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val engine =
            PersistedLearningEngineFactory
                .create(
                    persistenceDirectory =
                        persistenceDirectory,
                    contentRepository =
                        contentRepository,
                    learningItemRepository =
                        learningItemRepository
                )

        repeat(itemCount) { index ->
            registerItem(
                engine = engine,
                number = index + 1
            )
        }

        return engine
    }

    private fun registerItem(
        engine: LearningEngine,
        number: Int
    ) {
        val content =
            Content(
                id =
                    ContentId(
                        "content-$number"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Sentence $number"
                    )
            )

        engine.registerContent(
            content
        )

        engine.registerLearningItem(
            LearningItem(
                id =
                    LearningItemId(
                        "item-$number"
                    ),
                contentId =
                    content.id,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )
        )
    }
}