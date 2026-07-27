package vn.loi.learning.desktop.ui.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration

class StudyPresentationSettingsTest {
    @Test
    fun `draft leaves active configuration unchanged until apply`() {
        val configuration = DesktopRuntimeConfiguration()
        val edited = StudyPresentationPreferences(
            controlMode = StudyPresentationControlMode.MANUAL,
            showVietnamese = false
        )
        val state = StudyPresentationSettingsState(configuration.studyPresentation).edit(edited)

        assertTrue(configuration.studyPresentation != state.draft)
        assertTrue(configuration.studyPresentation == state.active)
        assertTrue(state.applyTo(configuration).studyPresentation == edited)
    }

    @Test
    fun `adaptive disables controls and preview never autoplays`() {
        val presentation = resolveStudyPresentationSettings(
            StudyPresentationPreferences(
                showEnglish = false,
                showVietnamese = false,
                autoplayEnglish = true,
                autoplayVietnamese = true
            )
        )

        assertFalse(presentation.controlsEnabled)
        assertTrue(presentation.showEnglishPreview)
        assertTrue(presentation.showVietnamesePreview)
        assertFalse(presentation.autoplayPreview)
    }

    @Test
    fun `guided and manual previews map visibility preferences without audio`() {
        listOf(
            StudyPresentationControlMode.PREFERENCE_GUIDED,
            StudyPresentationControlMode.MANUAL
        ).forEach { mode ->
            val presentation = resolveStudyPresentationSettings(
                StudyPresentationPreferences(
                    controlMode = mode,
                    showEnglish = false,
                    showVietnamese = true
                )
            )
            assertTrue(presentation.controlsEnabled)
            assertFalse(presentation.showEnglishPreview)
            assertTrue(presentation.showVietnamesePreview)
            assertFalse(presentation.autoplayPreview)
        }
    }
}
