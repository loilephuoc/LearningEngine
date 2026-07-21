package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudyRatingGuidanceTest {
    @Test
    fun `rating guidance preserves the keyboard rating order`() {
        val guidance = resolveStudyRatingGuidance()

        assertEquals(
            listOf(
                StudyActionControl.REVIEW_AGAIN,
                StudyActionControl.REVIEW_HARD,
                StudyActionControl.REVIEW_GOOD,
                StudyActionControl.REVIEW_EASY
            ),
            guidance.map(StudyRatingGuidance::control)
        )
        assertEquals(
            listOf("Again", "Hard", "Good", "Easy"),
            guidance.map(StudyRatingGuidance::label)
        )
    }

    @Test
    fun `every rating explains its scheduling consequence`() {
        val guidance = resolveStudyRatingGuidance()

        assertTrue(guidance[0].description.contains("again soon"))
        assertTrue(guidance[1].description.contains("shorter interval"))
        assertTrue(guidance[2].description.contains("normal review interval"))
        assertTrue(guidance[3].description.contains("longer interval"))
    }

    @Test
    fun `combined description includes all numeric shortcuts`() {
        val description = resolveStudyRatingGuidanceDescription()

        listOf("1 Again", "2 Hard", "3 Good", "4 Easy")
            .forEach { expected ->
                assertTrue(description.contains(expected))
            }
    }
}
