package vn.loi.learning.desktop.ui.shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.importer.legacy.LegacyImportResult
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class TopicSwitchResumeIntegrationTest {

    @Test
    fun `select A study switch B study and return to A restores independent checkpoint after restart`() {
        val persistenceDirectory =
            Files.createTempDirectory(
                "topic-switch-resume"
            )

        try {
            val imported =
                importFixture()
            val firstContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )
            imported.contents.forEach(
                firstContext.engine::registerContent
            )
            imported.learningItems.forEach(
                firstContext.engine::registerLearningItem
            )

            val topicAContent =
                imported.contents.first { content ->
                    content.metadata.lesson == "Topic A"
                }
            val topicBContent =
                imported.contents.first { content ->
                    content.metadata.lesson == "Topic B"
                }
            val topicATexts =
                imported.contents
                    .filter { content ->
                        content.metadata.lesson == "Topic A"
                    }
                    .map { content ->
                        content.text.primaryText
                    }
                    .toSet()
            val topicBTexts =
                imported.contents
                    .filter { content ->
                        content.metadata.lesson == "Topic B"
                    }
                    .map { content ->
                        content.text.primaryText
                    }
                    .toSet()

            val firstViewModel =
                StudyViewModel(
                    StudyFacade(firstContext)
                )
            val firstCoordinator =
                LessonStudyNavigationCoordinator(
                    studyViewModel = firstViewModel,
                    navigationState =
                        NavigationState(
                            NavigationDestination.CONTENT_LIBRARY
                        )
                )

            firstCoordinator.startLessonStudy(
                topicAContent.id.value
            )
            val topicAId =
                requireNotNull(
                    firstViewModel.uiState.topicId
                )
            assertTrue(
                firstViewModel.uiState.contentText in topicATexts
            )
            firstViewModel.revealAnswer()
            firstViewModel.reviewGood()
            val transitionToken = requireNotNull(
                firstViewModel.uiState.sessionContinuityTransition
            ).token
            repeat(3) { firstViewModel.advanceSessionContinuity(transitionToken) }
            val topicACheckpoint =
                firstViewModel.uiState
            assertEquals(
                1,
                topicACheckpoint.reviewedCount
            )

            firstCoordinator.startLessonStudy(
                topicBContent.id.value
            )
            val topicBId =
                requireNotNull(
                    firstViewModel.uiState.topicId
                )
            assertNotEquals(topicAId, topicBId)
            assertEquals(0, firstViewModel.uiState.reviewedCount)
            assertTrue(
                firstViewModel.uiState.contentText in topicBTexts
            )
            firstViewModel.revealAnswer()
            firstViewModel.reviewGood()
            val topicBTransitionToken = requireNotNull(
                firstViewModel.uiState.sessionContinuityTransition
            ).token
            repeat(3) { firstViewModel.advanceSessionContinuity(topicBTransitionToken) }
            val topicBCheckpoint =
                firstViewModel.uiState
            assertEquals(
                1,
                topicBCheckpoint.reviewedCount
            )

            firstCoordinator.startLessonStudy(
                topicAContent.id.value
            )
            assertEquals(topicAId, firstViewModel.uiState.topicId)
            assertEquals(
                topicACheckpoint.currentLearningItemId,
                firstViewModel.uiState.currentLearningItemId
            )
            assertEquals(
                topicACheckpoint.currentItemPosition,
                firstViewModel.uiState.currentItemPosition
            )
            assertEquals(1, firstViewModel.uiState.reviewedCount)
            assertTrue(
                firstViewModel.uiState.contentText in topicATexts
            )

            val restartedContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )
            val restartedViewModel =
                StudyViewModel(
                    StudyFacade(restartedContext)
                )
            val restartedCoordinator =
                LessonStudyNavigationCoordinator(
                    studyViewModel = restartedViewModel,
                    navigationState =
                        NavigationState(
                            NavigationDestination.CONTENT_LIBRARY
                        )
                )

            restartedCoordinator.startLessonStudy(
                topicAContent.id.value
            )
            assertEquals(topicAId, restartedViewModel.uiState.topicId)
            assertEquals(1, restartedViewModel.uiState.reviewedCount)
            assertEquals(
                topicACheckpoint.currentLearningItemId,
                restartedViewModel.uiState.currentLearningItemId
            )
            assertTrue(
                restartedViewModel.uiState.contentText in topicATexts
            )

            restartedCoordinator.startLessonStudy(
                topicBContent.id.value
            )
            assertEquals(topicBId, restartedViewModel.uiState.topicId)
            assertEquals(1, restartedViewModel.uiState.reviewedCount)
            assertEquals(
                topicBCheckpoint.currentLearningItemId,
                restartedViewModel.uiState.currentLearningItemId
            )
            assertTrue(
                restartedViewModel.uiState.contentText in topicBTexts
            )
        } finally {
            persistenceDirectory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun importFixture(): LegacyImportResult =
        LegacyJsonImporter().import(
            sourceName = "topic-switch-course",
            jsonText =
                """
                [
                  {
                    "group": "English",
                    "section": "Unit 1",
                    "lesson": "Topic A",
                    "en": "Alpha one.",
                    "vi": "Alpha mot."
                  },
                  {
                    "group": "English",
                    "section": "Unit 1",
                    "lesson": "Topic A",
                    "en": "Alpha two.",
                    "vi": "Alpha hai."
                  },
                  {
                    "group": "English",
                    "section": "Unit 2",
                    "lesson": "Topic B",
                    "en": "Beta one.",
                    "vi": "Beta mot."
                  },
                  {
                    "group": "English",
                    "section": "Unit 2",
                    "lesson": "Topic B",
                    "en": "Beta two.",
                    "vi": "Beta hai."
                  }
                ]
                """.trimIndent()
        )
}
