package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidFocusedPracticeCompositionTest {
    @Test
    fun `Review Hub exposes exactly two focused modes and evaluative all learned`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        )
        val reviewHub = source.substringAfter("fun ReviewHub(").substringBefore("fun SettingsScreen(")
        assertTrue(reviewHub.contains("Ôn từ vừa học"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.LATEST_SESSION"))
        assertTrue(reviewHub.contains("Ôn Again / Hard"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.DIFFICULT"))
        assertTrue(reviewHub.contains("Ôn tất cả đã học"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.LEARNED"))
        assertFalse(reviewHub.contains("Adaptive Review"))
        assertFalse(reviewHub.contains("Typing practice"))
        assertFalse(reviewHub.contains("Learned Items"))
        val actionList = reviewHub.substringAfter("val actions = listOf(").substringBefore("actions.forEach")
        assertTrue(Regex("ReviewHubAction\\(").findAll(actionList).count() == 3)
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
        assertTrue(start.contains("AndroidSessionEntry.LEARNED -> reviewAvailability.learnedItems"))
        assertTrue(start.contains("!hasScheduledAdaptiveWork"))
        assertTrue(start.contains("PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED"))
        assertTrue(start.indexOf("if (!canStartRequestedMode)") < start.indexOf("finishSession("))
        assertTrue(start.contains("entry == AndroidSessionEntry.REVIEW && active.studyMode == mode"))
    }

    @Test
    fun `all learned availability ignores due quota and uses production adaptive resolver`() {
        val facade = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt")
        )
        val home = facade.substringAfter("fun home()").substringBefore("fun start(entry:")
        val learnedAvailability = home.lineSequence()
            .first { it.contains("canStartLearnedReview =") }
        assertFalse(learnedAvailability.contains("daily"))
        assertFalse(learnedAvailability.contains("active"))
        val createPlan = facade.substringAfter("private fun createPlan(").substringBefore("private fun currentScope(")
        assertTrue(createPlan.contains("createProductionRecallPlan"))
        assertTrue(createPlan.contains("studyMode = next.session.studyMode"))
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
