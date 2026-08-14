package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidDifficultSkimCompositionTest {
    private val screen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
    )

    @Test
    fun `difficult focused practice renders reveal first skim navigation without rating dock`() {
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("private fun IntroductionInteractionHint(")
        assertTrue(introduction.contains("val difficultSkim = state.focusedPracticeKind"))
        assertTrue(introduction.contains("ratingEnabled = !focusedSkimUx && introductionRatingInputEnabled("))
        assertTrue(introduction.contains("if (difficultSkim) \"Xem đáp án\""))
        assertTrue(introduction.contains("focusedSkimUx = focusedSkimUx"))
        assertTrue(introduction.contains("gatedUpwardNavigation = focusedSkimUx"))
        assertTrue(introduction.contains("interactionEnabled = false"))
        assertFalse(introduction.contains("Text(\"Next\")"))
        assertTrue(screen.contains("FocusedPracticeKind.DIFFICULT -> \"Again / Hard\""))
        assertTrue(introduction.contains("StudyAnswerSection("))
        assertFalse(introduction.contains("if (difficultSkim) {\n                    StudyRatingBar("))
    }
}
