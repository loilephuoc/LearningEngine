package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudyIdlePresentationTest {

    @Test
    fun `idle study state provides actionable start guidance`() {
        val presentation =
            resolveStudyIdlePresentation(
                StudyUiState()
            )

        requireNotNull(presentation)
        assertEquals("Ready to study", presentation.title)
        assertEquals("Start Study", presentation.actionLabel)
        assertEquals("Enter or Space", presentation.shortcutHint)
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
}
