package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopPackageLearningFlowIntegrationTest {

    @Test
    fun `OPD3 import reaches deterministic Desktop completion across restart and final undo`() {
        val root = Files.createTempDirectory("desktop-package-learning-flow")
        val persistenceDirectory = root.resolve("data")
        val packageDirectory = root.resolve("packages")
        Files.createDirectories(packageDirectory)

        try {
            createPackage(packageDirectory.resolve("desktop-flow.opd3"))
            val firstContext = LearningApplicationFactory.createPersisted(persistenceDirectory)

            val importResult = firstContext.packageImporter(packageDirectory).importAllDetailed(
                PackageCatalogId("desktop-flow-catalog")
            )

            assertTrue(importResult.failures.isEmpty())
            assertEquals(1, importResult.successfulImports.size)
            assertEquals(2, importResult.successfulImports.single().importedContentCount)
            assertEquals(2, importResult.successfulImports.single().importedLearningItemCount)
            assertEquals(listOf("Desktop Flow Package"), firstContext.installedPackages.query().map { it.name })
            val importedPackage = firstContext.contentPackageRepository!!.findAll().single()
            firstContext.installedPackageRepository!!.save(
                vn.loi.learning.domain.library.model.InstalledPackage.reconstitute(
                    id = vn.loi.learning.domain.library.model.InstalledPackageId(importedPackage.id.value),
                    libraryId = vn.loi.learning.domain.library.model.LibraryId("default-library"),
                    packageId = importedPackage.id,
                    topicId = importedPackage.topicId,
                    name = vn.loi.learning.domain.library.model.PackageName(importedPackage.name),
                    version = vn.loi.learning.domain.library.model.PackageVersion(importedPackage.version),
                    state = vn.loi.learning.domain.library.model.PackageState.ACTIVE,
                    installedAt = java.time.Instant.now(),
                    contentCount = 2,
                    learningItemCount = 2
                )
            )

            val firstFacade = StudyFacade(firstContext)
            val firstQuestion = firstFacade.startStudy()
            assertTrue(firstQuestion.hasActiveSession)
            assertEquals(2, firstQuestion.totalItems)
            assertEquals(0, firstQuestion.sessionProgress?.completedItemCount)
            assertEquals(2, firstQuestion.sessionProgress?.remainingItemCount)

            val firstItemText = firstQuestion.contentText
            assertTrue(firstFacade.revealAnswer().canReview)
            val secondQuestion = firstFacade.review(ReviewRating.GOOD)
            assertTrue(secondQuestion.hasActiveSession)
            assertNotEquals(firstItemText, secondQuestion.contentText)
            assertEquals(1, secondQuestion.reviewedCount)
            assertEquals(1, secondQuestion.sessionProgress?.completedItemCount)
            assertEquals(1, secondQuestion.sessionProgress?.remainingItemCount)

            val restartedContext = LearningApplicationFactory.createPersisted(persistenceDirectory)
            val restartedFacade = StudyFacade(restartedContext)
            val restoredQuestion = restartedFacade.load()
            assertEquals(secondQuestion.contentText, restoredQuestion.contentText)
            assertTrue(restoredQuestion.canRevealAnswer)
            assertFalse(restoredQuestion.canReview)
            assertEquals(1, restoredQuestion.reviewedCount)

            restartedFacade.revealAnswer()
            val completed = restartedFacade.review(ReviewRating.EASY)
            assertTrue(completed.sessionCompleted)
            assertFalse(completed.hasActiveSession)
            assertEquals(2, completed.reviewedCount)
            assertEquals(2, completed.sessionProgress?.completedItemCount)
            assertEquals(0, completed.sessionProgress?.remainingItemCount)
            assertTrue(completed.sessionProgress?.isCompleted == true)
            assertTrue(completed.canUndo)

            val reopened = restartedFacade.undoLatestReview()
            assertTrue(reopened.hasActiveSession)
            assertFalse(reopened.sessionCompleted)
            assertTrue(reopened.canReview)
            assertEquals(1, reopened.reviewedCount)
            assertEquals(1, reopened.sessionProgress?.completedItemCount)
            assertFalse(reopened.canUndo)

            val completedAgain = restartedFacade.review(ReviewRating.HARD)
            assertTrue(completedAgain.sessionCompleted)
            assertEquals(2, completedAgain.reviewedCount)
            assertEquals(2, completedAgain.sessionProgress?.completedItemCount)

            val finalContext = LearningApplicationFactory.createPersisted(persistenceDirectory)
            val recoveredCompletion = StudyFacade(finalContext).load()
            assertTrue(recoveredCompletion.sessionCompleted)
            assertEquals(2, recoveredCompletion.reviewedCount)
            val learnerId = LearnerId("default-learner")
            val persistedReviews =
                listOf("desktop-flow-item-1", "desktop-flow-item-2")
                    .sumOf { itemId ->
                        finalContext.engine.getReviewHistory(
                            learnerId,
                            LearningItemId(itemId)
                        ).size
                    }
            assertEquals(2, persistedReviews)
            assertEquals("Desktop Flow Package", finalContext.installedPackages.query().single().name)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun createPackage(file: Path) {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeEntry(zip, "manifest.json", """
                {
                  "name": "Desktop Flow Package",
                  "version": "1.0.0",
                  "format": "OPD3",
                  "contentCount": 2,
                  "learningItemCount": 2
                }
            """.trimIndent())
            writeEntry(zip, "metadata.json", """
                {
                  "name": "Desktop Flow Package",
                  "version": "1.0.0",
                  "format": "OPD3"
                }
            """.trimIndent())
            writeEntry(zip, "contents.json", """
                {
                  "contents": [
                    {
                      "id": "desktop-flow-content-1",
                      "type": "WORD",
                      "primaryText": "Question one",
                      "translatedText": "Answer one",
                      "tags": ["desktop-flow"],
                      "customFields": {}
                    },
                    {
                      "id": "desktop-flow-content-2",
                      "type": "WORD",
                      "primaryText": "Question two",
                      "translatedText": "Answer two",
                      "tags": ["desktop-flow"],
                      "customFields": {}
                    }
                  ]
                }
            """.trimIndent())
            writeEntry(zip, "learning-items.json", """
                {
                  "learningItems": [
                    {
                      "id": "desktop-flow-item-1",
                      "contentId": "desktop-flow-content-1",
                      "mode": "MEANING_RECOGNITION",
                      "isEnabled": true
                    },
                    {
                      "id": "desktop-flow-item-2",
                      "contentId": "desktop-flow-content-2",
                      "mode": "MEANING_RECOGNITION",
                      "isEnabled": true
                    }
                  ]
                }
            """.trimIndent())
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
