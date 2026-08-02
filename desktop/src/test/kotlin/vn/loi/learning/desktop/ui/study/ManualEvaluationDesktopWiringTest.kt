package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvaluativeRatingDockDesktopWiringTest {
    @Test
    fun `evaluative dock is direct and obsolete header dialog entry is removed`() {
        val screen = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
        )
        val facade = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt")
        )

        assertFalse(screen.contains("ManualEvaluationDialog"))
        assertFalse(screen.contains("Đánh giá thủ công"))
        assertTrue(screen.contains("dockMode == StudyActionDockMode.ANSWER_ACTIONS"))
        assertTrue(facade.contains("!item.session.answerRevealed"))
        assertTrue(facade.contains("revealAnswer()"))
        assertTrue(facade.contains("RatingSource.MANUAL_USER"))
        assertFalse(
            facade.substringAfter("fun review(").substringBefore("fun revealTypingRecall")
                .contains("MANUAL_USER_OVERRIDE")
        )
    }
}
