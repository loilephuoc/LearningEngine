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
        assertEquals("Bạn muốn học gì?", presentation.title)
        assertEquals("Học tiếp", presentation.actionLabel)
        assertEquals("Enter or Space", presentation.shortcutHint)
        assertEquals(4, presentation.actions.size)
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
        assertEquals("Tiếp tục phiên đang học", presentation.actionLabel)
        assertEquals("Tiếp tục phiên đang học", presentation.actions.first().label)
    }
}
