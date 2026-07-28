package vn.loi.learning.desktop.ui.shell

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.infrastructure.LearningApplicationFactory

class Opd3ContentLibraryLessonStudyIntegrationTest {

    @Test
    fun `import browse study grade and resume OPD3 lesson through Desktop presentation`() {
        val persistenceDirectory =
            Files.createTempDirectory(
                "desktop-opd3-content-library"
            )
        val packageDirectory =
            Files.createTempDirectory(
                "desktop-opd3-packages"
            )

        try {
            createPackage(
                packageDirectory.resolve(
                    "desktop-browse-flow.opd3"
                )
            )

            val applicationContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )
            val studyViewModel =
                StudyViewModel(
                    StudyFacade(applicationContext)
                )
            val navigationState =
                NavigationState(
                    NavigationDestination.CONTENT_LIBRARY
                )
            val coordinator =
                LessonStudyNavigationCoordinator(
                    studyViewModel = studyViewModel,
                    navigationState = navigationState
                )
            val contentLibraryViewModel =
                ContentLibraryViewModel(
                    facade =
                        ContentLibraryFacade(
                            applicationContext
                        ),
                    lessonBrowserFacade =
                        LessonBrowserFacade(
                            applicationContext
                        )
                )

            contentLibraryViewModel.importFromDirectory(
                packageDirectory
            )

            assertEquals(
                null,
                contentLibraryViewModel.uiState.importError
            )
            assertNotNull(
                contentLibraryViewModel.uiState.importMessage
            )

            val library =
                assertNotNull(
                    contentLibraryViewModel
                        .uiState
                        .libraries
                        .firstOrNull { item ->
                            item.name ==
                                "Desktop Browse Flow"
                        }
                )

            assertEquals(2, library.contentCount)
            assertEquals(4, library.learningItemCount)

            contentLibraryViewModel.openLibrary(
                library.id
            )

            val browser =
                assertNotNull(
                    contentLibraryViewModel
                        .lessonBrowserUiState
                )

            assertEquals(library.id, browser.libraryId)
            assertEquals(2, browser.lessonCount)
            assertEquals(
                setOf(
                    "Hello from OPD3.",
                    "Where is the station?"
                ),
                browser.lessons
                    .map { item ->
                        item.primaryText
                    }
                    .toSet()
            )

            val selectedLesson =
                browser.lessons.first { item ->
                    item.primaryText ==
                        "Hello from OPD3."
                }

            contentLibraryViewModel.selectLesson(
                selectedLesson.id
            )

            val selectedBrowser =
                assertNotNull(
                    contentLibraryViewModel
                        .lessonBrowserUiState
                )

            assertEquals(
                selectedLesson.id,
                selectedBrowser.selectedLessonId
            )

            coordinator.startLessonStudy(
                assertNotNull(
                    selectedBrowser.selectedLesson
                ).id
            )

            assertEquals(
                NavigationDestination.STUDY,
                navigationState.currentDestination
            )
            assertTrue(
                studyViewModel.uiState.hasActiveSession
            )
            assertTrue(
                studyViewModel.uiState.isLessonStudy
            )
            assertEquals(
                "Greetings",
                studyViewModel.uiState.studyTitle
            )
            assertEquals(
                "Hello from OPD3.",
                studyViewModel.uiState.contentText
            )
            assertFalse(
                studyViewModel.uiState.contentText ==
                    "Where is the station?"
            )
            val installedTopicId =
                assertNotNull(
                    studyViewModel.uiState.topicId
                )

            val plannedTotal =
                studyViewModel.uiState.totalItems

            assertTrue(plannedTotal > 1)
            assertTrue(
                studyViewModel.uiState.canRevealAnswer
            )

            val restartedContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )
            val restartedStudyViewModel =
                StudyViewModel(
                    StudyFacade(restartedContext)
                )
            restartedStudyViewModel.startLessonStudy(selectedLesson.id)

            assertTrue(
                restartedStudyViewModel
                    .uiState
                    .hasActiveSession
            )
            assertTrue(
                restartedStudyViewModel
                    .uiState
                    .isLessonStudy
            )
            assertEquals(
                "Greetings",
                restartedStudyViewModel
                    .uiState
                    .studyTitle
            )
            assertEquals(
                installedTopicId,
                restartedStudyViewModel
                    .uiState
                    .topicId
            )
            assertEquals(
                0,
                restartedStudyViewModel
                    .uiState
                    .reviewedCount
            )
            assertEquals(
                plannedTotal,
                restartedStudyViewModel
                    .uiState
                    .totalItems
            )
            assertEquals(
                1,
                restartedStudyViewModel
                    .uiState
                    .currentItemPosition
            )
            assertEquals(
                "Hello from OPD3.",
                restartedStudyViewModel
                    .uiState
                    .contentText
            )
            assertFalse(
                restartedStudyViewModel
                    .uiState
                    .contentText ==
                    "Where is the station?"
            )

            restartedStudyViewModel
                .revealAnswer()

            assertTrue(
                restartedStudyViewModel
                    .uiState
                    .canReview
            )
            assertEquals(
                "Xin chao tu OPD3.",
                restartedStudyViewModel
                    .uiState
                    .translationText
            )

            restartedStudyViewModel
                .reviewGood()

            assertTrue(
                restartedStudyViewModel
                    .uiState
                    .sessionCompleted
            )
            assertFalse(
                restartedStudyViewModel
                    .uiState
                    .hasActiveSession
            )
            val completedProgress = requireNotNull(
                restartedStudyViewModel.uiState.sessionProgress
            )
            assertEquals(
                restartedStudyViewModel.uiState.reviewedCount,
                completedProgress.reviewedItemCount
            )
            assertEquals(plannedTotal, completedProgress.completedItemCount)
            assertEquals(
                plannedTotal - restartedStudyViewModel.uiState.reviewedCount,
                completedProgress.skippedItemCount
            )
            assertTrue(completedProgress.isCompleted)
            assertEquals(
                plannedTotal,
                restartedStudyViewModel
                    .uiState
                    .totalItems
            )
            assertEquals(
                "$plannedTotal of $plannedTotal technical experiences completed",
                restartedStudyViewModel
                    .uiState
                    .progressLabel
            )
            assertEquals(
                "Greetings",
                restartedStudyViewModel
                    .uiState
                    .studyTitle
            )

            val completedContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )
            val completedStudyViewModel =
                StudyViewModel(
                    StudyFacade(completedContext)
                )

            assertFalse(
                completedStudyViewModel
                    .uiState
                    .hasActiveSession
            )
            assertEquals(
                null,
                completedStudyViewModel
                    .uiState
                    .loadError
            )
        } finally {
            packageDirectory.toFile().deleteRecursively()
            persistenceDirectory.toFile().deleteRecursively()
        }
    }

    private fun createPackage(file: Path) {
        ZipOutputStream(
            Files.newOutputStream(file)
        ).use { zip ->
            writeEntry(
                zip,
                "manifest.json",
                """
                {
                  "name": "Desktop Browse Flow",
                  "version": "1.0.0",
                  "format": "OPD3",
                  "contentCount": 2,
                  "learningItemCount": 4
                }
                """.trimIndent()
            )
            writeEntry(
                zip,
                "metadata.json",
                """
                {
                  "name": "Desktop Browse Flow",
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
                      "id": "desktop-content-1",
                      "type": "SENTENCE",
                      "primaryText": "Hello from OPD3.",
                      "translatedText": "Xin chao tu OPD3.",
                      "title": "Greeting",
                      "group": "English",
                      "section": "Unit 1",
                      "lesson": "Greetings"
                    },
                    {
                      "id": "desktop-content-2",
                      "type": "SENTENCE",
                      "primaryText": "Where is the station?",
                      "translatedText": "Nha ga o dau?",
                      "title": "Travel question",
                      "group": "English",
                      "section": "Unit 1",
                      "lesson": "Travel"
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
                      "id": "desktop-item-1-recognition",
                      "contentId": "desktop-content-1",
                      "mode": "MEANING_RECOGNITION",
                      "isEnabled": true
                    },
                    {
                      "id": "desktop-item-1-recall",
                      "contentId": "desktop-content-1",
                      "mode": "MEANING_RECALL",
                      "isEnabled": true
                    },
                    {
                      "id": "desktop-item-2-recognition",
                      "contentId": "desktop-content-2",
                      "mode": "MEANING_RECOGNITION",
                      "isEnabled": true
                    },
                    {
                      "id": "desktop-item-2-recall",
                      "contentId": "desktop-content-2",
                      "mode": "MEANING_RECALL",
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
        zip.putNextEntry(
            ZipEntry(name)
        )
        zip.write(
            content.toByteArray(
                Charsets.UTF_8
            )
        )
        zip.closeEntry()
    }
}
