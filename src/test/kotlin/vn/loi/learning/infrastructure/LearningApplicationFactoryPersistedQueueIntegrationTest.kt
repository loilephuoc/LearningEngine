package vn.loi.learning.infrastructure

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.session.ActiveStudySessionRecovery
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class LearningApplicationFactoryPersistedQueueIntegrationTest {

    @Test
    fun `persisted application context restores active session queue after recreation`() {
        val persistenceDirectory =
            Files.createTempDirectory("desktop-context-persisted-queue")

        try {
            val firstContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )

            val imported =
                LegacyJsonImporter().import(
                    sourceName = "desktop-context-test",
                    jsonText =
                        """
                        [
                          {
                            "group": "Desktop",
                            "section": "Beta",
                            "lesson": "Restart",
                            "en": "first",
                            "vi": "thu nhat"
                          },
                          {
                            "group": "Desktop",
                            "section": "Beta",
                            "lesson": "Restart",
                            "en": "second",
                            "vi": "thu hai"
                          }
                        ]
                        """.trimIndent()
                )

            imported.contents.forEach(
                firstContext.engine::registerContent
            )
            imported.learningItems.forEach(
                firstContext.engine::registerLearningItem
            )

            val learnerId =
                LearnerId("default-learner")
            val sessionId =
                SessionId("desktop-context-session")
            val now =
                Moment(1_000L)

            firstContext.engine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId = learnerId,
                    startedAt = now,
                    policy =
                        SessionPolicy(
                            newItemLimit = 2,
                            reviewItemLimit = 0
                        )
                )
            )

            assertTrue(
                Files.exists(
                    persistenceDirectory.resolve(
                        "study-queues.json"
                    )
                ) || Files.exists(
                    persistenceDirectory.resolve(
                        "learning_engine.db"
                    )
                )
            )

            val recreatedContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )

            val recovery =
                recreatedContext.engine.recoverActiveSession(
                    learnerId = learnerId,
                    recoveredAt = Moment(2_000L)
                )

            val resumable =
                assertIs<
                        ActiveStudySessionRecovery.Resumable
                        >(recovery)

            assertTrue(
                resumable.queueProgress.remainingItemCount > 0
            )
        } finally {
            persistenceDirectory
                .toFile()
                .deleteRecursively()
        }
    }
}
