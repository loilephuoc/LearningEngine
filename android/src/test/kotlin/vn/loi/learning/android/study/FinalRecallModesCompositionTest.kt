package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinalRecallModesCompositionTest {
    private fun source(path: String) = Files.readString(Path.of("src/main/kotlin/$path"))
    private val image = source("vn/loi/learning/android/study/modes/ImageRecallStage.kt")
    private val example = source("vn/loi/learning/android/study/modes/ExampleCompletionStage.kt")
    private val cloze = source("vn/loi/learning/android/study/components/StudyClozeSentence.kt")
    private val screen = source("vn/loi/learning/android/study/StudyScreen.kt")

    @Test fun `Image Recall delegates a hero media first typed composition`() {
        assertTrue(screen.contains("ImageRecallStudyStage("))
        assertTrue(image.indexOf("StudyMedia(") < image.indexOf("StudyAnswerInput("))
        assertTrue(image.contains("imageRecallMediaRole(imeVisible, false)"))
        assertTrue(image.contains("WindowInsets.ime"))
        assertTrue(image.contains("remember(state.plan.planId.value)"))
    }

    @Test fun `Image Recall preserves one submit and fullscreen route`() {
        assertEquals(1, Regex("onEvent\\(AndroidStudyEvent\\.Submit").findAll(image).count())
        assertTrue(screen.contains("ReviewImageNavigationOverlay("))
        assertTrue(image.contains("availableMediaHeightDp, onOpenFullscreenImage"))
        assertTrue(screen.contains("StudyAnswerSection("))
        assertFalse(image.contains("requiredHeight"))
    }

    @Test fun `Example Completion uses semantic cloze plus shared input`() {
        assertTrue(screen.contains("ExampleCompletionStudyStage("))
        assertTrue(example.indexOf("StudyClozeSentence(") < example.indexOf("StudyAnswerInput("))
        assertTrue(example.contains("WindowInsets.ime"))
        assertTrue(example.contains("remember(state.plan.planId.value)"))
        assertTrue(cloze.contains("missing answer"))
        assertFalse(cloze.contains("maxLines"))
    }

    @Test fun `Example Completion keeps one event path per affordance and prompt audio route`() {
        assertEquals(2, Regex("onEvent\\(AndroidStudyEvent\\.Submit").findAll(example).count())
        assertEquals(1, Regex("playAudio\\(AudioRole\\.PROMPT").findAll(example).count())
        assertTrue(screen.contains("resolveClozePresentation("))
        assertFalse(example.contains("Snackbar"))
        assertFalse(example.contains("Dialog("))
    }
}
