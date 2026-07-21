package vn.loi.learning.desktop.ui.shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class LessonStudyNavigationCoordinatorIntegrationTest {

    @Test
    fun `valid lesson starts scoped study and navigates to Study`() {
        withFixture { fixture ->
            val selectedContent =
                fixture.imported.contents.first { content ->
                    content.metadata.lesson == "Greetings"
                }

            fixture.coordinator.startLessonStudy(
                selectedContent.id.toString()
            )

            assertEquals(
                NavigationDestination.STUDY,
                fixture.navigationState.currentDestination
            )
            assertTrue(fixture.studyViewModel.uiState.hasActiveSession)
            assertTrue(fixture.studyViewModel.uiState.isLessonStudy)
            assertEquals(
                "Greetings",
                fixture.studyViewModel.uiState.studyTitle
            )
            assertTrue(
                fixture.studyViewModel.uiState.contentText in
                    setOf("Hello.", "Good morning.")
            )
            assertFalse(
                fixture.studyViewModel.uiState.contentText ==
                    "Where is the station?"
            )
        }
    }

    @Test
    fun `failed lesson start keeps Content Library visible`() {
        withFixture { fixture ->
            fixture.coordinator.startLessonStudy(
                "missing-content"
            )

            assertEquals(
                NavigationDestination.CONTENT_LIBRARY,
                fixture.navigationState.currentDestination
            )
            assertFalse(fixture.studyViewModel.uiState.hasActiveSession)
            assertNotNull(fixture.studyViewModel.uiState.loadError)
            assertEquals(
                "Study data needs attention.",
                fixture.studyViewModel.uiState.message
            )
        }
    }

    private fun withFixture(
        block: (Fixture) -> Unit
    ) {
        val persistenceDirectory =
            Files.createTempDirectory(
                "lesson-study-navigation"
            )

        try {
            val applicationContext =
                LearningApplicationFactory.createPersisted(
                    persistenceDirectory
                )
            val imported =
                LegacyJsonImporter().import(
                    sourceName = "desktop-navigation-course",
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
                applicationContext.engine::registerContent
            )
            imported.learningItems.forEach(
                applicationContext.engine::registerLearningItem
            )

            val studyViewModel =
                StudyViewModel(
                    StudyFacade(applicationContext)
                )
            val navigationState =
                NavigationState(
                    NavigationDestination.CONTENT_LIBRARY
                )

            block(
                Fixture(
                    imported = imported,
                    studyViewModel = studyViewModel,
                    navigationState = navigationState,
                    coordinator =
                        LessonStudyNavigationCoordinator(
                            studyViewModel = studyViewModel,
                            navigationState = navigationState
                        )
                )
            )
        } finally {
            persistenceDirectory
                .toFile()
                .deleteRecursively()
        }
    }

    private data class Fixture(
        val imported:
        vn.loi.learning.infrastructure.importer.legacy.LegacyImportResult,
        val studyViewModel: StudyViewModel,
        val navigationState: NavigationState,
        val coordinator: LessonStudyNavigationCoordinator
    )
}
