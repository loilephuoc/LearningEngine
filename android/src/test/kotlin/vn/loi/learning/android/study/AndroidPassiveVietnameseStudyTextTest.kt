package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidPassiveVietnameseStudyTextTest {
    private val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
    private val answerSection = source(
        "vn/loi/learning/android/study/components/IntroductionAnswerSection.kt"
    )

    @Test
    fun `Quick Review front Vietnamese meaning delegates tap to the reveal card`() {
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("private fun IntroductionHeroMedia(")
        val front = introduction.substringAfter("if (!revealed) {").substringBefore("} else {")

        assertTrue(introduction.contains("onClick = onGenericStageTap"))
        assertTrue(front.contains("interaction = StudyTextInteraction.PASSIVE"))
        assertTrue(front.contains("onToggleAudio = null"))
        assertFalse(front.contains("playAudio(AudioRole.MEANING"))
        assertEquals(
            IntroductionStageGesture.TAP,
            resolveIntroductionStageGesture(
                deltaX = 2f,
                deltaY = 3f,
                swipeThresholdPx = 44f,
                tapSlopPx = 12f,
                scrollRequired = false,
                childConsumed = false,
                alreadySubmitted = false,
                ratingEnabled = false
            )
        )
    }

    @Test
    fun `revealed Vietnamese meaning and example have no audio callbacks or button contract`() {
        val sectionContract = answerSection.substringAfter("internal fun StudyAnswerSection(")
            .substringBefore(") {")
        val meaning = answerSection.substringAfter("vietnameseAnswer?.takeIf")
            .substringBefore("if (!englishExample")
        val vietnameseExample = answerSection.substringAfter("vietnameseExample?.takeIf")
            .substringBefore("            }")

        assertFalse(sectionContract.contains("onVietnameseAudio"))
        assertFalse(sectionContract.contains("onVietnameseExampleAudio"))
        assertFalse(sectionContract.contains("vietnameseAudioPath"))
        assertFalse(sectionContract.contains("vietnameseExampleAudioPath"))
        assertTrue(meaning.contains("interaction = StudyTextInteraction.PASSIVE"))
        assertTrue(meaning.contains("onToggleAudio") || meaning.contains("null,"))
        assertTrue(vietnameseExample.contains("null"))
        assertFalse(screen.contains("playAudio(AudioRole.EXAMPLE_VIETNAMESE"))
    }

    @Test
    fun `passive target bypasses clickable Surface and audio accessibility semantics`() {
        val target = answerSection.substringAfter("internal fun StudyAudioTextTarget(")
        val surfaceGate = target.substringAfter("Box(Modifier.fillMaxWidth()")

        assertTrue(answerSection.contains("enum class StudyTextInteraction"))
        assertTrue(surfaceGate.contains("interaction == StudyTextInteraction.AUDIO"))
        assertTrue(surfaceGate.contains("Surface("))
        assertTrue(surfaceGate.contains("role = Role.Button"))
        assertTrue(surfaceGate.contains("Tap to play audio"))
        assertTrue(surfaceGate.contains("else target()"))
    }

    @Test
    fun `gestures beginning over passive Vietnamese text retain horizontal and upward ownership`() {
        assertEquals(
            IntroductionStageGesture.NEXT,
            gesture(deltaX = -100f, deltaY = 8f, ratingEnabled = false, navigationEnabled = true)
        )
        assertEquals(
            IntroductionStageGesture.SWIPE_GOOD,
            gesture(deltaX = 8f, deltaY = -100f, ratingEnabled = true, navigationEnabled = true)
        )
    }

    @Test
    fun `English headword and example audio remain interactive in every shared answer section`() {
        assertTrue(answerSection.contains("onAnswerAudio: () -> Unit"))
        assertTrue(answerSection.contains("onEnglishExampleAudio: () -> Unit"))
        assertTrue(answerSection.contains("answerAudioPath, isPlayingAnswer, true"))
        assertTrue(answerSection.contains("\"English example\""))
        assertTrue(answerSection.contains("interaction = if (onAudio == null)"))
        assertTrue(screen.contains("restartAudio(AudioRole.EXAMPLE_ENGLISH"))
    }

    @Test
    fun `all production Android Study modes use the passive shared reveal boundary`() {
        listOf("Introduction", "Typing", "Listening", "MultipleChoice", "ImageRecall", "ExampleCompletion")
            .forEach { mode -> assertTrue(screen.contains("is AndroidStudyState.$mode"), mode) }
        assertTrue(screen.contains("StudyRevealAndFeedbackContent("))
        assertTrue(screen.contains("StudyAnswerSection("))
        val typed = source("vn/loi/learning/android/study/modes/TypedAnswerStages.kt")
        assertTrue(typed.contains("StudyAnswerSection("))
        assertFalse(typed.contains("onVietnameseAudio"))
        assertFalse(typed.contains("onVietnameseExampleAudio"))
    }

    private fun gesture(
        deltaX: Float,
        deltaY: Float,
        ratingEnabled: Boolean,
        navigationEnabled: Boolean
    ) = resolveIntroductionStageGesture(
        deltaX = deltaX,
        deltaY = deltaY,
        swipeThresholdPx = 44f,
        tapSlopPx = 12f,
        scrollRequired = false,
        childConsumed = false,
        alreadySubmitted = false,
        ratingEnabled = ratingEnabled,
        navigationEnabled = navigationEnabled
    )

    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin").resolve(relative))
}
