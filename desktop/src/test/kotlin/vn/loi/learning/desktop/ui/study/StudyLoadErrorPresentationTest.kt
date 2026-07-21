package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudyLoadErrorPresentationTest {

    @Test
    fun `load error provides recovery guidance and shortcut`() {
        val presentation =
            resolveStudyLoadErrorPresentation(
                StudyUiState(
                    loadError =
                        "Study data could not be loaded: invalid queue."
                )
            )

        requireNotNull(presentation)
        assertEquals(
            "Study data needs attention",
            presentation.title
        )
        assertContains(
            presentation.message,
            "invalid queue"
        )
        assertContains(
            presentation.guidance,
            "persisted study data"
        )
        assertEquals(
            "Retry loading",
            presentation.actionLabel
        )
        assertEquals(
            "Enter or Space",
            presentation.shortcutHint
        )
    }

    @Test
    fun `normal and blank error states have no error presentation`() {
        assertNull(
            resolveStudyLoadErrorPresentation(
                StudyUiState()
            )
        )
        assertNull(
            resolveStudyLoadErrorPresentation(
                StudyUiState(loadError = "   ")
            )
        )
    }
}
