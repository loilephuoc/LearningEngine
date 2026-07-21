package vn.loi.learning.infrastructure.persistence

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy

class Opd3PersistedLearningRestartIntegrationTest {

    @Test
    fun `imported OPD3 session resumes remaining queue item after restart`() {
        val persistenceDirectory =
            Files.createTempDirectory("opd3-restart-persistence")
        val packageDirectory =
            Files.createTempDirectory("opd3-restart-packages")
        val packageFile =
            packageDirectory.resolve("restart-flow.opd3")

        try {
            createPackage(packageFile)

            val firstPlatform =
                PersistedLearningPlatformFactory.createPlatform(
                    persistenceDirectory = persistenceDirectory,
                    packageDirectory = packageDirectory
                )

            val importResults =
                firstPlatform.packageImportService.importAll(
                    PackageCatalogId("restart-flow-catalog")
                )

            assertEquals(1, importResults.size)
            assertEquals(2, importResults.single().importedContentCount)
            assertEquals(2, importResults.single().importedLearningItemCount)

            val sessionId = SessionId("opd3-restart-session")
            val learnerId = LearnerId("opd3-restart-learner")
            val startedAt = Moment(10_000L)

            firstPlatform.learningEngine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId = learnerId,
                    startedAt = startedAt,
                    policy =
                        SessionPolicy(
                            newItemLimit = 2,
                            reviewItemLimit = 1
                        )
                )
            )

            val firstItem =
                assertNotNull(
                    firstPlatform.learningEngine.getNextSessionItem(
                        sessionId,
                        startedAt
                    )
                )

            firstPlatform.learningEngine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("opd3-restart-review-1"),
                    learningItemId = firstItem.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = Moment(11_000L)
                )
            )

            val expectedRemainingItem =
                assertNotNull(
                    firstPlatform.learningEngine.getNextSessionItem(
                        sessionId,
                        Moment(12_000L)
                    )
                )

            assertNotEquals(
                firstItem.item.learningItem.id,
                expectedRemainingItem.item.learningItem.id
            )

            val restartedPlatform =
                PersistedLearningPlatformFactory.createPlatform(
                    persistenceDirectory = persistenceDirectory,
                    packageDirectory = packageDirectory
                )

            val resumedItem =
                assertNotNull(
                    restartedPlatform.learningEngine.getNextSessionItem(
                        sessionId,
                        Moment(13_000L)
                    )
                )

            assertEquals(
                expectedRemainingItem.item.learningItem.id,
                resumedItem.item.learningItem.id
            )
        } finally {
            packageDirectory.toFile().deleteRecursively()
            persistenceDirectory.toFile().deleteRecursively()
        }
    }

    private fun createPackage(file: java.nio.file.Path) {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeEntry(
                zip,
                "manifest.json",
                """
                {
                  "name": "Restart Flow Package",
                  "version": "1.0.0",
                  "format": "OPD3",
                  "contentCount": 2,
                  "learningItemCount": 2
                }
                """.trimIndent()
            )
            writeEntry(
                zip,
                "metadata.json",
                """
                {
                  "name": "Restart Flow Package",
                  "version": "1.0.0",
                  "format": "OPD3"
                }
                """.trimIndent()
            )
            writeEntry(
                zip,
                "contents.json",
                """
                {
                  "contents": [
                    {
                      "id": "restart-content-1",
                      "type": "WORD",
                      "primaryText": "resume",
                      "translatedText": "tiep tuc",
                      "tags": ["restart"],
                      "customFields": {}
                    },
                    {
                      "id": "restart-content-2",
                      "type": "WORD",
                      "primaryText": "recover",
                      "translatedText": "khoi phuc",
                      "tags": ["restart"],
                      "customFields": {}
                    }
                  ]
                }
                """.trimIndent()
            )
            writeEntry(
                zip,
                "learning-items.json",
                """
                {
                  "learningItems": [
                    {
                      "id": "restart-item-1",
                      "contentId": "restart-content-1",
                      "mode": "MEANING_RECOGNITION",
                      "isEnabled": true
                    },
                    {
                      "id": "restart-item-2",
                      "contentId": "restart-content-2",
                      "mode": "MEANING_RECOGNITION",
                      "isEnabled": true
                    }
                  ]
                }
                """.trimIndent()
            )
        }
    }

    private fun writeEntry(
        zip: ZipOutputStream,
        name: String,
        content: String
    ) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
