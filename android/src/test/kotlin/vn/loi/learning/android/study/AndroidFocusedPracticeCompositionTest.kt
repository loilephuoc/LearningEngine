package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidFocusedPracticeCompositionTest {
    @Test
    fun `Review Hub exposes quick review two focused modes and evaluative all learned`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt")
        )
        val reviewHub = source.substringAfter("fun ReviewHub(").substringBefore("fun SettingsScreen(")
        assertTrue(reviewHub.contains("Quick Review"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.QUICK_REVIEW"))
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
        assertTrue(Regex("ReviewHubAction\\(").findAll(actionList).count() == 4)
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
        assertTrue(start.contains("AndroidSessionEntry.QUICK_REVIEW -> reviewAvailability.learnedItems"))
        assertTrue(start.contains("PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW"))
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

    @Test
    fun `quick review owns endless presentation identity while Learn New keeps its header`() {
        val screen = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        val facade = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt")
        )
        assertTrue(screen.contains("if (quickReview) \"Quick Review\" else \"NEW\""))
        assertTrue(screen.contains("FocusedPracticeKind.QUICK_REVIEW ->\n                            QuickReviewProgressHeader(hud)"))
        assertTrue(screen.contains("contentDescription = \"Quick Review. Endless learned vocabulary review.\""))
        assertTrue(screen.contains("currentPosition = if (quickReview) null"))
        assertTrue(screen.contains("totalItems = if (quickReview) null"))
        assertTrue(screen.contains("else -> LearnNewProgressHeader(state, hud, pendingIntroductionHudRating)"))
        assertTrue(facade.contains("\"Quick Review · Pass ${'$'}{queueSnapshot.practiceRound}\""))
    }

    @Test
    fun `quick review reveal offers canonical ratings without a visible Next while difficult stays Next only`() {
        val screen = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("private fun IntroductionAnswerSection(")
        assertTrue(introduction.contains("if (difficultSkim) {"))
        assertFalse(introduction.contains("if (difficultSkim || quickReview)"))
        assertTrue(introduction.contains("if (!quickReview || state.revealed) {\n                        StudyRatingBar("))
        assertTrue(introduction.contains("onRating = onRating"))
        assertTrue(introduction.contains("underlinedRating = if (quickReview) state.latestEffectiveRating else null"))
        assertTrue(introduction.contains("enabled = !state.historyPreview && !quickReviewTransitionPending"))
        assertFalse(introduction.contains("if (quickReview && state.revealed)"))
        assertTrue(introduction.windowed("Text(\"Next\")".length).count { it == "Text(\"Next\")" } == 1)
    }

    @Test
    fun `quick review swipe gates unrated next on question audio only with race guards`() {
        val screen = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        val gate = screen.substringAfter("val startQuickReviewQuestionGate:")
            .substringBefore("LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen)")
        assertTrue(gate.contains("audioController.stop()"))
        assertTrue(gate.contains("path = introduction.resolvedPromptAudio"))
        assertFalse(gate.contains("resolvedExample"))
        assertFalse(gate.contains("resolvedMeaningAudio"))
        assertFalse(gate.contains("resolvedExpectedAnswerAudio"))
        assertTrue(gate.contains("isLooping = false"))
        assertTrue(gate.contains("quickReviewTransitionPending = true"))
        assertTrue(gate.contains("generation != quickReviewTransitionGeneration"))
        assertTrue(gate.contains("itemKey != acceptedItemKey"))
        assertTrue(gate.contains("onEvent(AndroidStudyEvent.NextVisited)"))
        assertTrue(screen.contains("if (quickReview) startQuickReviewQuestionGate()"))
        assertTrue(screen.contains("!quickReviewTransitionPending) stopAudioAndDispatch(AndroidStudyEvent.PreviousVisited)"))
        assertTrue(screen.contains("scaleX = imageFeedbackScale * quickReviewPulseScale"))
        assertTrue(screen.contains("targetValue = if (quickReviewQuestionPlaying && !reducedMotion) 1.04f else 1f"))
        assertTrue(screen.contains("durationMillis = 900"))
    }
}
