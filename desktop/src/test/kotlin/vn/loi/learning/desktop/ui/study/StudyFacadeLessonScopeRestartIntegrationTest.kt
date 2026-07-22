package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class StudyFacadeLessonScopeRestartIntegrationTest {

    @Test
    fun `selected imported lesson remains isolated after persisted restart`() {
        val persistenceDirectory =
            Files.createTempDirectory(
                "desktop-lesson-scope-restart"
            )

        try {
            val firstContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )

            val imported =
                LegacyJsonImporter().import(
                    sourceName = "desktop-imported-course",
                    jsonText =
                        """
                        [
                          {
                            "group": "English",
                            "section": "Unit 1",
                            "lesson": "Greetings",
                            "en": "Hello.",
                            "vi": "Xin chao."
                          },
                          {
                            "group": "English",
                            "section": "Unit 1",
                            "lesson": "Greetings",
                            "en": "Good morning.",
                            "vi": "Chao buoi sang."
                          },
                          {
                            "group": "English",
                            "section": "Unit 1",
                            "lesson": "Travel",
                            "en": "Where is the station?",
                            "vi": "Nha ga o dau?"
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

            val greetingsContent =
                imported.contents.first { content ->
                    content.metadata.lesson == "Greetings"
                }

            val selectedTexts =
                setOf(
                    "Hello.",
                    "Good morning."
                )

            val firstFacade =
                StudyFacade(firstContext)

            val started =
                firstFacade.startLessonStudy(
                    greetingsContent.id.toString()
                )


            assertTrue(started.hasActiveSession)
            assertTrue(started.isLessonStudy)
            assertEquals("Greetings", started.studyTitle)
            val plannedTotal =
                started.totalItems

            assertTrue(plannedTotal > 1)
            assertTrue(
                plannedTotal <=
                    imported.learningItems.count { item ->
                        item.contentId in
                            imported.contents
                                .filter { content ->
                                    content.metadata.lesson ==
                                        "Greetings"
                                }
                                .map { content ->
                                    content.id
                                }
                                .toSet()
                    }
            )
            assertEquals(1, started.currentItemPosition)
            assertTrue(started.contentText in selectedTexts)
            assertFalse(
                started.contentText ==
                    "Where is the station?"
            )

            assertFailsWith<IllegalArgumentException> {
                firstFacade.review(ReviewRating.GOOD)
            }

            firstFacade.revealAnswer()

            assertFailsWith<IllegalArgumentException> {
                firstFacade.revealAnswer()
            }

            val afterFirstReview =
                firstFacade.review(
                    ReviewRating.GOOD
                )

            assertTrue(afterFirstReview.hasActiveSession)
            assertEquals(1, afterFirstReview.reviewedCount)
            assertEquals(
                plannedTotal,
                afterFirstReview.totalItems
            )
            assertEquals(2, afterFirstReview.currentItemPosition)
            assertTrue(
                afterFirstReview.contentText in selectedTexts
            )
            assertFalse(
                afterFirstReview.contentText ==
                    "Where is the station?"
            )

            val revealedBeforeRestart = firstFacade.revealAnswer()
            assertTrue(revealedBeforeRestart.canReview)
            assertFalse(revealedBeforeRestart.canRevealAnswer)

            val recreatedContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )
            val recreatedFacade =
                StudyFacade(recreatedContext)

            val restored =
                recreatedFacade.load()

            assertTrue(restored.hasActiveSession)
            assertTrue(restored.isLessonStudy)
            assertEquals("Greetings", restored.studyTitle)
            assertEquals(1, restored.reviewedCount)
            assertEquals(
                plannedTotal,
                restored.totalItems
            )
            assertEquals(2, restored.currentItemPosition)
            assertTrue(restored.contentText in selectedTexts)
            assertFalse(
                restored.contentText ==
                    "Where is the station?"
            )
            assertTrue(restored.canReview)
            assertFalse(restored.canRevealAnswer)
            assertEquals(
                restored.contentText,
                requireNotNull(restored.learningContent)
                    .question
                    .textBlocks
                    .first()
                    .value
            )

            var state =
                restored
            var safetyCounter =
                0

            while (!state.sessionCompleted) {
                assertTrue(state.hasActiveSession)
                assertTrue(state.contentText in selectedTexts)
                assertFalse(
                    state.contentText ==
                        "Where is the station?"
                )
                assertEquals(
                    plannedTotal,
                    state.totalItems
                )

                if (state.canRevealAnswer) {
                    recreatedFacade.revealAnswer()
                }

                state =
                    recreatedFacade.review(
                        ReviewRating.GOOD
                    )

                safetyCounter += 1
                assertTrue(
                    safetyCounter <= plannedTotal,
                    "Session did not complete within its planned queue."
                )
            }

            assertFalse(state.hasActiveSession)
            assertEquals(
                plannedTotal,
                state.reviewedCount
            )
            assertEquals(
                plannedTotal,
                state.totalItems
            )
            assertEquals("Greetings", state.studyTitle)
        } finally {
            persistenceDirectory
                .toFile()
                .deleteRecursively()
        }
    }
}
