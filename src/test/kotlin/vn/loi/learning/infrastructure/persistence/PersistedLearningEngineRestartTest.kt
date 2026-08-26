package vn.loi.learning.infrastructure.persistence

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.importer.legacy.LegacyImportResult
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class PersistedLearningEngineRestartTest {

    @Test
    fun `engine restores learning state and continues session after restart`() {
        val persistenceDirectory =
            Files.createTempDirectory(
                "learning-engine-restart-test"
            )

        try {
            val importResult =
                LegacyJsonImporter().import(
                    sourceName = "restart-test",
                    jsonText = legacyJson()
                )

            assertEquals(
                expected = 2,
                actual = importResult.importedContentCount
            )

            assertEquals(
                expected = 0,
                actual = importResult.skippedRecordCount
            )

            val learnerId =
                LearnerId("learner-1")

            val sessionId =
                SessionId("session-1")

            val firstReviewTime =
                Moment(1_000_000L)

            /*
             * Engine instance thứ nhất.
             */
            val firstEngine =
                createEngine(
                    persistenceDirectory =
                        persistenceDirectory,
                    importResult =
                        importResult
                )

            firstEngine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId = learnerId,
                    startedAt = firstReviewTime,
                    policy = SessionPolicy(
                        newItemLimit = 10,
                        reviewItemLimit = 10,
                        allowRepeatInSameSession = false
                    )
                )
            )

            val firstNextItem =
                requireNotNull(
                    firstEngine.getNextSessionItem(
                        sessionId = sessionId,
                        now = firstReviewTime
                    )
                )

            val firstLearningItemId =
                firstNextItem.item.learningItem.id

            val firstContentId =
                firstNextItem.item.content.id

            val firstReviewResult =
                firstEngine.reviewSessionItem(
                    ReviewSessionItemCommand(
                        sessionId = sessionId,
                        reviewEventId =
                            ReviewEventId("review-1"),
                        learningItemId =
                            firstLearningItemId,
                        rating =
                            ReviewRating.GOOD,
                        reviewedAt =
                            firstReviewTime
                    )
                )

            assertEquals(
                expected = 1,
                actual = firstReviewResult.session.totalReviews
            )

            assertTrue(
                firstReviewResult.session.reviewedItemIds
                    .contains(firstLearningItemId)
            )

            assertTrue(
                firstReviewResult.session.reviewedContentIds
                    .contains(firstContentId)
            )

            /*
             * Không tái sử dụng Engine hoặc in-memory repository cũ.
             *
             * Engine thứ hai được tạo bằng repository Content và
             * LearningItem hoàn toàn mới, nhưng dùng chung thư mục
             * persistence.
             */
            val secondEngine =
                createEngine(
                    persistenceDirectory =
                        persistenceDirectory,
                    importResult =
                        importResult
                )

            val restoredSession =
                assertNotNull(
                    secondEngine.getSession(sessionId)
                )

            assertEquals(
                expected = 1,
                actual = restoredSession.totalReviews
            )

            assertTrue(
                restoredSession.reviewedItemIds
                    .contains(firstLearningItemId)
            )

            assertTrue(
                restoredSession.reviewedContentIds
                    .contains(firstContentId)
            )

            val restoredMemoryState =
                assertNotNull(
                    secondEngine.getMemoryState(
                        learnerId = learnerId,
                        learningItemId = firstLearningItemId
                    )
                )

            assertEquals(
                expected = 1,
                actual = restoredMemoryState.reviewCount
            )

            assertEquals(
                expected = firstReviewTime,
                actual = restoredMemoryState.lastReviewedAt
            )

            val restoredHistory =
                secondEngine.getReviewHistory(
                    learnerId = learnerId,
                    learningItemId = firstLearningItemId
                )

            assertEquals(
                expected = 1,
                actual = restoredHistory.size
            )

            assertEquals(
                expected = ReviewEventId("review-1"),
                actual = restoredHistory.single().id
            )

            /*
             * Session tiếp tục sau restart.
             *
             * Vì Content đầu tiên đã được ghi nhận trong session,
             * sibling filtering phải chọn Content khác.
             */
            val secondReviewTime =
                Moment(2_000_000L)

            val nextAfterRestart =
                requireNotNull(
                    secondEngine.getNextSessionItem(
                        sessionId = sessionId,
                        now = secondReviewTime
                    )
                )

            assertNotEquals(
                illegal = firstContentId,
                actual = nextAfterRestart.item.content.id
            )

            val continuedResult =
                secondEngine.reviewSessionItem(
                    ReviewSessionItemCommand(
                        sessionId = sessionId,
                        reviewEventId =
                            ReviewEventId("review-2"),
                        learningItemId =
                            nextAfterRestart.item.learningItem.id,
                        rating =
                            ReviewRating.GOOD,
                        reviewedAt =
                            secondReviewTime
                    )
                )

            assertEquals(
                expected = 2,
                actual = continuedResult.session.totalReviews
            )

            assertEquals(
                expected = 2,
                actual =
                    continuedResult.session
                        .reviewedContentIds.size
            )

            val dbExists = persistenceDirectory.resolve("learning_engine.db").toFile().exists()
            assertTrue(dbExists || persistenceDirectory.resolve("memory-states.json").toFile().exists())
            assertTrue(dbExists || persistenceDirectory.resolve("review-events.json").toFile().exists())
            assertTrue(dbExists || persistenceDirectory.resolve("study-sessions.json").toFile().exists())
        } finally {
            deleteDirectoryRecursively(
                persistenceDirectory
            )
        }
    }

    private fun createEngine(
        persistenceDirectory: Path,
        importResult: LegacyImportResult
    ): LearningEngine {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val engine =
            PersistedLearningEngineFactory.create(
                persistenceDirectory =
                    persistenceDirectory,
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository
            )

        importResult.contents.forEach(
            engine::registerContent
        )

        importResult.learningItems.forEach(
            engine::registerLearningItem
        )

        return engine
    }

    private fun legacyJson(): String =
        """
        [
          {
            "group": "Vocabulary",
            "section": "Family",
            "lesson": "Lesson 1",
            "en": "aunt",
            "vi": "cô, dì hoặc bác gái",
            "audio": "aunt.mp3"
          },
          {
            "group": "Vocabulary",
            "section": "Family",
            "lesson": "Lesson 1",
            "en": "brother",
            "vi": "anh hoặc em trai",
            "audio": "brother.mp3"
          }
        ]
        """.trimIndent()

    private fun deleteDirectoryRecursively(
        directory: Path
    ) {
        if (Files.notExists(directory)) return
        directory.toFile().deleteRecursively()
    }
}

