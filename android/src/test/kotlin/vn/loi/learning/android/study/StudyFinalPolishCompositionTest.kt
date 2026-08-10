package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyFinalPolishCompositionTest {
    private fun source(path: String) = Files.readString(Path.of("src/main/kotlin/$path"))
    private val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
    private val foundation = source("vn/loi/learning/android/study/components/StudyFoundationComponents.kt")
    private val typed = source("vn/loi/learning/android/study/modes/TypedAnswerStages.kt")
    private val image = source("vn/loi/learning/android/study/modes/ImageRecallStage.kt")

    @Test fun `all six modes delegate presentation without dead generic renderer`() {
        listOf(
            "IntroductionLearningStage(", "TypingStudyStage(", "ListeningStudyStage(",
            "MultipleChoiceStudyStage(", "ImageRecallStudyStage(", "ExampleCompletionStudyStage("
        ).forEach { assertTrue(screen.contains(it), it) }
        assertFalse(screen.contains("private fun StudyModeInputArea("))
        assertFalse(screen.contains("private fun StudyPromptHeader("))
    }

    @Test fun `missing required media is compact explicit and accessible`() {
        val notice = foundation.substringAfter("internal fun StudyUnavailableNotice(")
            .substringBefore("internal fun StudyChoiceTile(")
        assertTrue(notice.contains("StudyTypography.metadata"))
        assertTrue(notice.contains("stateDescription = text"))
        assertTrue(typed.contains("Listening audio unavailable"))
        assertTrue(image.contains("Recall image unavailable"))
    }

    @Test fun `Introduction motion uses semantic roles and has no decorative image loop`() {
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("fun StudyAudioButton(")
        assertTrue(introduction.contains("StudyMotionRole.PRESS"))
        assertTrue(introduction.contains("StudyMotionRole.MEDIA_RESIZE"))
        assertTrue(introduction.contains("StudyMotionRole.REVEAL"))
        assertTrue(introduction.contains("StudyMotionRole.CARD_EXIT"))
        assertFalse(introduction.contains("learn new front motion"))
        assertFalse(introduction.contains("durationMillis = 1400"))
    }

    @Test fun `shared shell remains width bounded IME safe and scroll compatible`() {
        assertTrue(foundation.contains("widthIn(max = policy.maxContentWidthDp.dp)"))
        assertTrue(foundation.contains("imePadding()"))
        assertFalse(typed.contains("requiredHeight"))
        assertFalse(image.contains("requiredHeight"))
    }
}
