package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeReviewExperienceRefinementTest {
    private val studyScreen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
    )
    private val answerSurface = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/FocusedAnswerSurface.kt")
    )
    private val frontSurface = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/DiscoveryFrontSurface.kt")
    )

    @Test
    fun `practice removes answer status while retaining polished typing comparison`() {
        assertTrue(studyScreen.contains("practiceMode = uiState.practiceProgress != null"))
        assertTrue(answerSurface.containsCodeIgnoringWhitespace("compactForPractice = practiceMode"))
        assertTrue(answerSurface.contains("typingComparisonForCanonicalWord("))
    }

    @Test
    fun `practice actions use compact feedback controls and revised navigation copy`() {
        assertTrue(studyScreen.contains("PracticeFeedbackButton("))
        assertTrue(studyScreen.contains("modifier = modifier.height(30.dp)"))
        assertTrue(studyScreen.contains("Đánh giá vào SRS…"))
        assertTrue(studyScreen.contains("← Quay lại học chính"))
        assertFalse(studyScreen.contains("Chỉ là phản hồi luyện tập — không thay đổi đánh giá."))
    }

    @Test
    fun `front translation is larger and closer to its image`() {
        assertTrue(frontSurface.contains("fontSize = meaningStyle.fontSize * 1.19f"))
        assertTrue(frontSurface.contains("Arrangement.spacedBy(LETheme.spacing.space2)"))
    }

    @Test
    fun `expanded practice example uses reclaimed vertical space`() {
        assertTrue(answerSurface.containsCodeIgnoringWhitespace("compactForPractice = practiceMode"))
        assertTrue(answerSurface.containsCodeIgnoringWhitespace("bottom = if (compactForPractice) 6.dp else 14.dp"))
    }
}
