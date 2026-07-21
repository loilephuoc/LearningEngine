package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals

class StudyStageTransitionPresentationTest {
    @Test
    fun `formats scheduler stages with a real transition arrow`() {
        assertEquals(
            "Learning → Review",
            formatStudyStageTransition(
                beforeStage = "LEARNING",
                afterStage = "REVIEW"
            )
        )
    }

    @Test
    fun `formats multi-word enum names for people`() {
        assertEquals(
            "New item → Re learning",
            formatStudyStageTransition(
                beforeStage = "NEW_ITEM",
                afterStage = "RE_LEARNING"
            )
        )
    }

    @Test
    fun `normalizes whitespace and blank stage names`() {
        assertEquals(
            "Learning → Unknown",
            formatStudyStageTransition(
                beforeStage = "  LEARNING  ",
                afterStage = "   "
            )
        )
    }
}
