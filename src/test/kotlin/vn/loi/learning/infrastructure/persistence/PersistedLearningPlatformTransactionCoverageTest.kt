package vn.loi.learning.infrastructure.persistence

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class PersistedLearningPlatformTransactionCoverageTest {

    @Test
    fun `platform persists study queue alongside reviewed session state`() {
        val persistenceDirectory =
            Files.createTempDirectory("platform-transaction-persistence")
        val packageDirectory =
            Files.createTempDirectory("platform-transaction-packages")

        try {
            val platform =
                PersistedLearningPlatformFactory.createPlatform(
                    persistenceDirectory = persistenceDirectory,
                    packageDirectory = packageDirectory
                )

            val imported =
                LegacyJsonImporter().import(
                    sourceName = "transaction-test",
                    jsonText =
                        """
                        [
                          {
                            "group": "Vocabulary",
                            "section": "Core",
                            "lesson": "Transaction",
                            "en": "atomic",
                            "vi": "nguyen tu"
                          }
                        ]
                        """.trimIndent()
                )

            imported.contents.forEach(platform.learningEngine::registerContent)
            imported.learningItems.forEach(platform.learningEngine::registerLearningItem)

            val sessionId = SessionId("transaction-session")
            val now = Moment(1_000L)

            platform.learningEngine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId = LearnerId("transaction-learner"),
                    startedAt = now,
                    policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 1)
                )
            )

            val next =
                requireNotNull(
                    platform.learningEngine.getNextSessionItem(sessionId, now)
                )

            platform.learningEngine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("transaction-review"),
                    learningItemId = next.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = now
                )
            )

            val dbExists = Files.exists(persistenceDirectory.resolve("learning_engine.db"))
            assertTrue(dbExists || Files.exists(persistenceDirectory.resolve("study-sessions.json")))
            assertTrue(dbExists || Files.exists(persistenceDirectory.resolve("study-queues.json")))
            assertTrue(dbExists || Files.exists(persistenceDirectory.resolve("memory-states.json")))
            assertTrue(dbExists || Files.exists(persistenceDirectory.resolve("review-events.json")))
        } finally {
            packageDirectory.toFile().deleteRecursively()
            persistenceDirectory.toFile().deleteRecursively()
        }
    }
}
