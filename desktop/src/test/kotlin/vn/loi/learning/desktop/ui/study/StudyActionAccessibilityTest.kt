package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyActionAccessibilityTest {
    @Test
    fun `retry and primary study actions expose state specific descriptions`() {
        val retry =
            resolveStudyActionAccessibility(
                StudyActionControl.RETRY_LOAD
            )
        val start =
            resolveStudyActionAccessibility(
                StudyActionControl.START_STUDY
            )
        val general =
            resolveStudyActionAccessibility(
                StudyActionControl.START_GENERAL_STUDY
            )

        assertEquals("Retry Load", retry.visibleLabel)
        assertTrue(retry.contentDescription.contains("Space"))
        assertEquals("Start Study", start.visibleLabel)
        assertTrue(start.contentDescription.startsWith("Start Study"))
        assertEquals("Start General Study", general.visibleLabel)
        assertTrue(general.contentDescription.contains("Start General Study"))
    }

    @Test
    fun `reveal action describes configured default keyboard key`() {
        val presentation =
            resolveStudyActionAccessibility(
                StudyActionControl.REVEAL_ANSWER
            )

        assertEquals("Reveal Answer", presentation.visibleLabel)
        assertEquals("Space", presentation.shortcutHint)
        assertTrue(presentation.contentDescription.contains("Space"))
    }

    @Test
    fun `each review rating exposes its exact numeric shortcut`() {
        val expected =
            listOf(
                StudyActionControl.REVIEW_AGAIN to ("Again" to "1"),
                StudyActionControl.REVIEW_HARD to ("Hard" to "2"),
                StudyActionControl.REVIEW_GOOD to ("Good" to "3"),
                StudyActionControl.REVIEW_EASY to ("Easy" to "4")
            )

        expected.forEach { (control, labelAndShortcut) ->
            val presentation =
                resolveStudyActionAccessibility(control)

            assertEquals(labelAndShortcut.first, presentation.visibleLabel)
            assertEquals(labelAndShortcut.second, presentation.shortcutHint)
            assertTrue(
                presentation.contentDescription.contains(
                    if (control == StudyActionControl.REVIEW_GOOD) {
                        "Keyboard shortcut: ${labelAndShortcut.second} or Space."
                    } else {
                        "Keyboard shortcut: ${labelAndShortcut.second}."
                    }
                )
            )
        }
    }

    @Test
    fun `Good rating main label keeps numeric shortcut while accessibility exposes Space`() {
        val good = resolveStudyActionAccessibility(StudyActionControl.REVIEW_GOOD)
        val again = resolveStudyActionAccessibility(StudyActionControl.REVIEW_AGAIN)

        assertTrue(ratingButtonLabel(StudyActionControl.REVIEW_GOOD, good).contains("[3]"))
        assertFalse(ratingButtonLabel(StudyActionControl.REVIEW_GOOD, good).contains("Space"))
        assertTrue(good.contentDescription.contains("3 or Space"))
        assertEquals("[1]  Again", ratingButtonLabel(StudyActionControl.REVIEW_AGAIN, again))
    }
}
