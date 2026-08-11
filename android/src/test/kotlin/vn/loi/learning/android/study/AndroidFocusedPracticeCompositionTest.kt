package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidFocusedPracticeCompositionTest {
    @Test
    fun `Review Hub exposes both focused practice entries outside Learn New`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        )
        val reviewHub = source.substringAfter("fun ReviewHub(").substringBefore("fun SettingsScreen(")
        assertTrue(reviewHub.contains("Ôn từ vừa học"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.LATEST_SESSION"))
        assertTrue(reviewHub.contains("Ôn Again / Hard"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.DIFFICULT"))
        assertFalse(reviewHub.contains("actions.filter"))
        assertTrue(reviewHub.contains("enabled = action.available"))
        assertTrue(reviewHub.contains("hasActiveSession && action.available"))
        val studyLanding = source.substringAfter("fun StudyLanding(").substringBefore("fun ReviewHub(")
        assertFalse(studyLanding.contains("Ôn từ vừa học"))
        assertFalse(studyLanding.contains("Ôn Again / Hard"))
    }

    @Test
    fun `focused entries bypass adaptive due quota gate and no-due skim uses practice`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt")
        )
        val start = source.substringAfter("fun start(entry:").substringBefore("fun load(")
        assertTrue(start.contains("when (entry)"))
        assertTrue(start.contains("reviewAvailability.latestCompletedNewItems"))
        assertTrue(start.contains("reviewAvailability.difficultItems"))
        assertTrue(start.contains("!hasScheduledAdaptiveWork"))
        assertTrue(start.contains("PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED"))
        assertTrue(start.indexOf("if (!canStartRequestedMode)") < start.indexOf("finishSession("))
        assertTrue(start.contains("entry == AndroidSessionEntry.REVIEW && active.studyMode == mode"))
    }

    @Test
    fun `focused practice hides canonical scheduler rating controls`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        assertTrue(
            source.contains(
                "plan.provenance == RecallProvenance.PRACTICE && state.hud?.focusedPractice != true"
            )
        )
    }
}
