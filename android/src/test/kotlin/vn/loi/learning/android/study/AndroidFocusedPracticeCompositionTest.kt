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
        assertTrue(reviewHub.contains("R.string.review_quick_title"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.QUICK_REVIEW"))
        assertTrue(reviewHub.contains("R.string.review_recent_title"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.LATEST_SESSION"))
        assertTrue(reviewHub.contains("R.string.review_difficult_title"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.DIFFICULT"))
        assertTrue(reviewHub.contains("R.string.review_learned_title"))
        assertTrue(reviewHub.contains("AndroidSessionEntry.LEARNED"))
        assertFalse(reviewHub.contains("Adaptive Review"))
        assertFalse(reviewHub.contains("Typing practice"))
        assertFalse(reviewHub.contains("Learned Items"))
        val actionList = reviewHub.substringAfter("val actions = listOf(").substringBefore("actions.forEach")
        assertTrue(Regex("ReviewHubAction\\(").findAll(actionList).count() == 4)
        assertFalse(reviewHub.contains("actions.filter"))
        assertTrue(reviewHub.contains("clickable(enabled = action.available)"))
        val studyLanding = source.substringAfter("fun StudyLanding(").substringBefore("fun ReviewHub(")
        assertFalse(studyLanding.contains("Ôn từ vừa học"))
        assertFalse(studyLanding.contains("Ôn Again / Hard"))
    }

    @Test
    fun `focused entries bypass adaptive due quota gate and no-due skim uses intended policies`() {
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
        assertTrue(createPlan.contains("studyMode = if (next.session.studyMode == StudyMode.LEARN_NEW) StudyMode.ADAPTIVE else next.session.studyMode"))
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
        assertTrue(screen.contains("quickReview -> \"Quick Review\""))
        assertTrue(screen.contains("FocusedPracticeKind.DIFFICULT -> \"Again / Hard\""))
        assertTrue(screen.contains("else -> \"NEW · PACKAGE\""))
        assertTrue(screen.contains("FocusedPracticeKind.QUICK_REVIEW ->\n                            QuickReviewProgressHeader(state, hud)"))
        assertTrue(screen.contains("Endless learned vocabulary review."))
        assertTrue(screen.contains("currentPosition = if (focusedSkimUx) null"))
        assertTrue(screen.contains("totalItems = if (focusedSkimUx) null"))
        assertTrue(screen.contains("else -> LearnNewProgressHeader("))
        assertTrue(screen.contains("onEditNew = { limitEditor = \"new\" }"))
        assertTrue(facade.contains("FocusedPracticeKind.QUICK_REVIEW -> \"Quick Review\""))
    }

    @Test
    fun `quick review keeps canonical ratings while difficult uses accessible skim navigation without large Next`() {
        val screen = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("private fun IntroductionAnswerSection(")
        assertTrue(introduction.contains("if (!difficultSkim) {"))
        assertFalse(introduction.contains("if (difficultSkim || quickReview)"))
        assertTrue(introduction.contains("if (!quickReview || state.revealed) {\n                        StudyRatingBar("))
        assertTrue(introduction.contains("onRating = onRating"))
        assertTrue(introduction.contains("underlinedRating = state.navigation.previousRating"))
        assertTrue(introduction.contains("enabled = introductionRatingInputEnabled("))
        assertFalse(introduction.contains("if (quickReview && state.revealed)"))
        assertFalse(introduction.contains("Text(\"Next\")"))
    }

    @Test
    fun `quick review swipe gates unrated next on question audio only with race guards`() {
        val screen = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        val gate = screen.substringAfter("val startFocusedPracticeQuestionGate:")
            .substringBefore("\n    }\n\n    LaunchedEffect(")
        assertTrue(gate.contains("audioController.stop()"))
        assertTrue(gate.contains("path = introduction.resolvedPromptAudio"))
        assertFalse(gate.contains("resolvedExample"))
        assertFalse(gate.contains("resolvedMeaningAudio"))
        assertFalse(gate.contains("resolvedExpectedAnswerAudio"))
        assertTrue(gate.contains("isLooping = false"))
        assertTrue(gate.contains("quickReviewTransitionPending = true"))
        assertTrue(gate.contains("generation != quickReviewTransitionGeneration"))
        assertTrue(gate.contains("itemKey != acceptedItemKey"))
        assertTrue(gate.contains("onEvent(terminalEvent)"))
        assertTrue(screen.contains("state.revealed -> startFocusedPracticeQuestionGate("))
        assertTrue(screen.contains("gatedUpwardNavigation = focusedSkimUx"))
        assertTrue(screen.contains("revealed = state.revealed"))
        assertTrue(screen.contains("historyPreview = state.historyPreview"))
        val gestureKeys = screen.substringAfter(") = pointerInput(").substringBefore(") {")
        assertTrue(gestureKeys.contains("revealed"))
        assertTrue(gestureKeys.contains("historyPreview"))
        assertTrue(screen.contains("stopAudioAndDispatch(AndroidStudyEvent.PreviousVisited)"))
        assertTrue(screen.contains("imageScale = imageFeedbackScale * quickReviewPulseScale"))
        assertTrue(screen.contains("targetValue = if (quickReviewQuestionPlaying && !reducedMotion) 1.04f else 1f"))
        assertTrue(screen.contains("durationMillis = 900"))
    }

    @Test
    fun `foreground pause stops feedback and invalidates pending question gate`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))
        val lifecycle = screen.substringAfter("val observer = LifecycleEventObserver")
            .substringBefore("lifecycleOwner.lifecycle.addObserver")
        assertTrue(lifecycle.contains("Lifecycle.Event.ON_PAUSE"))
        assertTrue(lifecycle.contains("audioOwnership.stopForForegroundLoss()"))
        assertTrue(lifecycle.contains("feedbackAudioController.stop()"))
        val stop = screen.substringAfter("audioOwnership.registerForegroundStop(foregroundAudioOwner)")
            .substringBefore("onDispose")
        assertTrue(stop.contains("quickReviewTransitionGeneration++"))
        assertTrue(stop.contains("quickReviewTransitionPending = false"))
        assertTrue(stop.contains("quickReviewQuestionPlaying = false"))
        assertTrue(stop.contains("audioController.stop()"))
        assertTrue(screen.contains("Lifecycle.Event.ON_STOP -> onEvent(AndroidStudyEvent.PauseTyping)"))
    }

    @Test
    fun `pending Quick Review gate exclusively owns audio and locks child interactions`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))
        val answer = Files.readString(Path.of(
            "src/main/kotlin/vn/loi/learning/android/study/components/IntroductionAnswerSection.kt"
        ))
        val controls = Files.readString(Path.of(
            "src/main/kotlin/vn/loi/learning/android/study/components/StudyControls.kt"
        ))
        assertTrue(screen.contains("if (!quickReviewTransitionPending && audioOwnership.permitsManualPlayback"))
        assertTrue(screen.contains("introduction.revealed && !quickReviewTransitionPending"))
        assertTrue(screen.contains("interactionEnabled = !quickReviewTransitionPending"))
        assertTrue(screen.contains("enabled = !quickReviewTransitionPending"))
        assertTrue(answer.contains("enabled = interactionEnabled"))
        assertTrue(controls.contains("onClick = onToggleMute,"))
        assertTrue(controls.contains("onClick = { onAutoPlay?.invoke() },"))
        assertTrue(controls.contains("onClick = { onEditItem?.invoke() },"))
        assertTrue(controls.contains("onClick = { onToggleDifficult?.invoke() },"))
        assertTrue(screen.contains("else if (interactionEnabled) onOpenFullscreenImage(image)"))
    }

    @Test
    fun `every non-cancelled gate terminal clears pending before exactly one Next`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))
        val gate = screen.substringAfter("val startFocusedPracticeQuestionGate:")
            .substringBefore("LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen)")
        val finish = gate.substringAfter("val finish: () -> Unit").substringBefore("val initial =")
        assertTrue(finish.contains("if (finished || generation != quickReviewTransitionGeneration"))
        assertTrue(finish.contains("finished = true"))
        assertTrue(finish.indexOf("quickReviewTransitionPending = false") <
            finish.indexOf("onEvent(terminalEvent)"))
        assertTrue(gate.contains("AndroidAudioPlaybackEvent.Completed) finish()"))
        assertTrue(gate.contains("AndroidAudioState.Failed, AndroidAudioState.Unavailable -> finish()"))
        assertTrue(gate.contains("if (initial is AndroidAudioState.Failed || initial is AndroidAudioState.Unavailable) finish()"))
    }

    @Test
    fun `Quick Review front history reveal priority progress and coordinated motion share existing authorities`() {
        val screen = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))
        val facade = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt"))
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("private fun IntroductionInteractionHint(")
        val swipeRoute = screen.substringAfter("onIntroductionSwipeGood = {")
            .substringBefore("onIntroductionPrevious =")

        assertTrue(introduction.contains("IntroductionHeroMedia("))
        assertTrue(introduction.contains("if (focusedSkimUx || state.revealed)"))
        assertTrue(introduction.contains("canPrevious = state.navigation.canPrevious"))
        assertTrue(introduction.contains("canNext = state.navigation.canNext"))
        assertTrue(introduction.contains("onClickLabel = if (!state.revealed) \"Reveal answer\" else null"))
        assertTrue(introduction.contains("interactionEnabled = false"))
        assertTrue(introduction.contains("if (!state.revealed) onReveal()"))
        assertTrue(introduction.contains("AnimatedContent("))
        assertTrue(introduction.contains("label = \"Introduction coordinated reveal\""))
        assertTrue(introduction.contains("if (reducedMotion) EnterTransition.None"))
        assertFalse(introduction.contains("visible = !state.revealed"))
        assertFalse(introduction.contains("visible = state.revealed"))

        assertTrue(swipeRoute.contains("state.historyPreview -> stopAudioAndDispatch(AndroidStudyEvent.NextVisited)"))
        assertTrue(swipeRoute.contains("state.revealed -> startFocusedPracticeQuestionGate("))
        assertTrue(swipeRoute.contains("AndroidStudyEvent.DifficultPracticeAdvance"))
        assertTrue(swipeRoute.contains("AndroidStudyEvent.QuickReviewUnratedAdvance"))
        assertTrue(screen.contains("val focusedSkimUx = state is AndroidStudyState.Introduction && usesFocusedSkimUx"))
        assertTrue(facade.contains("quickReviewPassPosition = quickReviewQueue?.practiceProgress?.position"))
        assertTrue(facade.contains("quickReviewPoolSize = quickReviewQueue?.practiceProgress?.membershipSize"))
        assertFalse(facade.contains("Quick Review · Pass"))
    }
}
