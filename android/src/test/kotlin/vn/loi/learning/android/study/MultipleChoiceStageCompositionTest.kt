package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultipleChoiceStageCompositionTest {
    private val stage = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/MultipleChoiceStage.kt")
    )
    private val foundation = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/components/StudyFoundationComponents.kt")
    )
    private val screen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
    )
    private val answerSection = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/components/IntroductionAnswerSection.kt")
    )

    @Test
    fun `MCQ delegates composition and reuses Study foundation`() {
        assertTrue(screen.contains("MultipleChoiceStudyStage("))
        assertTrue(stage.contains("StudyStageCard("))
        assertTrue(stage.contains("StudyPrompt("))
        assertTrue(stage.contains("StudyMedia("))
        assertTrue(stage.contains("StudyChoiceTile("))
        assertTrue(stage.contains("feedbackContent()"))
    }

    @Test
    fun `one pending selection locks choices and emits one event route`() {
        assertTrue(stage.contains("pendingChoiceId == null && !state.completed"))
        assertEquals(1, Regex("onEvent\\(AndroidStudyEvent\\.Choose").findAll(stage).count())
        assertTrue(stage.contains("pendingChoiceId = choice.id"))
    }

    @Test
    fun `choice tile is full width wrapping accessible and non radio`() {
        val tile = foundation.substringAfter("internal fun StudyChoiceTile(")
            .substringBefore("internal fun StudyAnswerInput(")
        assertTrue(tile.contains("fillMaxWidth().defaultMinSize(minHeight = 56.dp)"))
        assertTrue(tile.contains("stateDescription = stateDescriptionText"))
        assertTrue(tile.contains("StudyTypography.choice"))
        assertFalse(tile.contains("requiredHeight"))
        assertFalse(tile.contains("maxLines"))
        assertFalse(tile.contains("RadioButton"))
    }

    @Test
    fun `prompt audio and answer reveal keep established routes`() {
        assertEquals(1, Regex("playAudio\\(AudioRole\\.PROMPT").findAll(stage).count())
        assertTrue(stage.contains("textAlign = TextAlign.Center"))
        assertTrue(stage.contains("showFullAnswer"))
        assertTrue(stage.contains("if (showFullAnswer) feedbackContent()"))
        assertTrue(screen.contains("StudyAnswerSection("))
        assertTrue(screen.contains("audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.PROMPT)"))
        assertTrue(screen.contains("MULTIPLE_CHOICE_AUDIO_WATCHDOG_MILLIS"))
        assertTrue(screen.contains("onEvent(AndroidStudyEvent.NextVisited)"))
        assertTrue(screen.contains("multipleChoiceAllowsManualRating()"))
        assertTrue(screen.contains("R.string.study_mcq_you_selected"))
        assertTrue(screen.contains("AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio"))
        assertTrue(screen.contains("AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio"))
        assertTrue(answerSection.contains("englishExampleSectionLabel"))
        assertTrue(answerSection.contains("vietnameseExampleSectionLabel"))
        assertTrue(answerSection.contains("visibleLabel"))
        assertFalse(stage.contains("Snackbar"))
        assertFalse(stage.contains("Dialog("))
    }
}
