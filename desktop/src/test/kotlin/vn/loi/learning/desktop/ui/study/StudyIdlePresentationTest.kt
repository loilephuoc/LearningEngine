package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.library.model.InstalledPackageId

class StudyIdlePresentationTest {

    @Test
    fun `idle study state provides actionable start guidance`() {
        val presentation =
            resolveStudyIdlePresentation(
                StudyUiState(activeInstalledPackageId = InstalledPackageId("pkg"))
            )

        requireNotNull(presentation)
        assertEquals("What would you like to learn?", presentation.title)
        assertEquals("Continue learning", presentation.actionLabel)
        assertEquals("Enter or Space", presentation.shortcutHint)
        assertEquals(5, presentation.actions.size)
    }

    @Test
    fun `active completed and error states do not use idle presentation`() {
        assertNull(
            resolveStudyIdlePresentation(
                StudyUiState(hasActiveSession = true)
            )
        )
        assertNull(
            resolveStudyIdlePresentation(
                StudyUiState(sessionCompleted = true)
            )
        )
        assertNull(
            resolveStudyIdlePresentation(
                StudyUiState(loadError = "Retry loading data")
            )
        )
    }

    @Test
    fun `explicit Learn entry presents chooser without hiding active-session availability`() {
        val presentation =
            resolveStudyIdlePresentation(
                StudyUiState(
                    hasActiveSession = true,
                    learnEntryChooserVisible = true
                )
            )

        requireNotNull(presentation)
        assertEquals("Resume active session", presentation.actionLabel)
        assertEquals("Resume active session", presentation.actions.first().label)
    }
}
