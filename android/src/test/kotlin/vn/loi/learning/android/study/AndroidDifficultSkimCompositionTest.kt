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
    fun `difficult focused practice renders skim reveal and Next without rating dock`() {
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("private fun IntroductionInteractionHint(")
        assertTrue(introduction.contains("val difficultSkim = state.focusedPracticeKind"))
        assertTrue(introduction.contains("ratingEnabled = !difficultSkim && !state.revealed"))
        assertTrue(introduction.contains("if (difficultSkim) \"Xem đáp án\""))
        assertTrue(introduction.contains("if (difficultSkim)"))
        assertTrue(introduction.contains("onClick = { onEvent(AndroidStudyEvent.NextVisited) }"))
        assertTrue(introduction.contains("StudyAnswerSection("))
        assertFalse(introduction.contains("if (difficultSkim) {\n                    StudyRatingBar("))
    }
}
