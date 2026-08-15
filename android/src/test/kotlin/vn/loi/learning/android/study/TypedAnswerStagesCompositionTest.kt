package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypedAnswerStagesCompositionTest {
    @Test
    fun `Listening autoplay and manual replay are plan keyed and never loop`() {
        val autoplay = screen.substringAfter("state is AndroidStudyState.Listening").substringBefore("LaunchedEffect(")
        assertTrue(autoplay.contains("restartAudio(AudioRole.PROMPT, state.resolvedPromptAudio, false)"))
        val listening = modes
            .substringAfter("internal fun ListeningStudyStage(").substringBefore("private fun TypedAnswerStageFrame(")
        assertTrue(listening.contains("playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, false)"))
        assertFalse(listening.contains("state.resolvedPromptAudio, true"))
    }

    @Test
    fun `Introduction rating stops runtime audio before feedback ownership starts`() {
        val rating = screen.substringAfter("val submitIntroductionRating:")
            .substringBefore("LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen)")
        assertTrue(rating.indexOf("audioController.stop()") < rating.indexOf("onIntroductionRatingWithFeedback("))
        assertTrue(screen.contains("audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.PROMPT)"))
    }
    private val modes = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/TypedAnswerStages.kt")
    )
    private val screen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
    )
    private val viewModel = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyViewModel.kt")
    )
    private val facade = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt")
    )
    private val trace = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidTypingSuccessTrace.kt")
    )
    private val audio = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/media/AndroidAudioController.kt")
    )
    private val foundation = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/components/StudyFoundationComponents.kt")
    )
    private val answerSection = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/components/IntroductionAnswerSection.kt")
    )
    private val layoutTrace = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidTypingLayoutTrace.kt")
    )
    private val policy = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyPresentationPolicy.kt")
    )

    @Test
    fun `typed modes reuse one input and feedback foundation`() {
        assertTrue(modes.contains("internal fun TypingStudyStage("))
        assertTrue(modes.contains("internal fun ListeningStudyStage("))
        assertEquals(2, Regex("StudyAnswerInput\\(").findAll(modes).count())
        assertTrue(modes.contains("TypedAnswerStageFrame"))
        assertTrue(screen.contains("StudyAnswerSection("))
    }

    @Test
    fun `typing is IME aware and keeps media subordinate`() {
        assertTrue(modes.contains("WindowInsets.ime.getBottom"))
        assertTrue(modes.contains("inputSessionActive = inputSessionActive"))
        assertTrue(modes.contains("availableMediaHeightDp"))
        assertTrue(modes.contains("BringIntoView") || screen.contains("bringIntoViewRequester"))
    }

    @Test
    fun `listening has one replay route and restrained audio state`() {
        val listeningPrompt = modes.substringAfter("internal fun StudyListeningAudioPrompt(")
            .substringBefore("private fun TypedInputActions(")
        assertTrue(modes.contains("StudyListeningAudioPrompt("))
        assertEquals(1, Regex("playAudio\\(AudioRole\\.PROMPT").findAll(
            modes.substringAfter("internal fun ListeningStudyStage(").substringBefore("private fun TypedAnswerStageFrame(")
        ).count())
        assertTrue(listeningPrompt.contains("StudyMotionRole.AUDIO_PULSE"))
        assertTrue(listeningPrompt.contains("if (isPlaying && !reducedMotion)"))
        assertFalse(modes.contains("Snackbar"))
        assertFalse(modes.contains("Dialog("))
    }

    @Test
    fun `StudyScreen delegates typed mode presentation`() {
        assertTrue(screen.contains("TypingStudyStage("))
        assertTrue(screen.contains("ListeningStudyStage("))
        assertFalse(screen.contains("private fun StudyModeInputArea("))
        assertFalse(screen.contains("private fun StudyPromptHeader("))
    }

    @Test
    fun `Typing success is audio gated and wrong state owns one nearby Reveal action`() {
        val typing = modes.substringAfter("internal fun TypingStudyStage(")
            .substringBefore("private fun formatTypingSeconds")
        val activeTyping = typing.substringBefore("internal fun TypingDifferenceComparison(")
        val genericFeedback = screen.substringAfter("private fun StudyRevealAndFeedbackContent(")
            .substringBefore("private fun Completion(")

        assertTrue(screen.contains("TypingSuccessAudioCompleted"))
        assertTrue(screen.contains("isLooping = false"))
        assertTrue(modes.contains("SelectTypingRatingOverride"))
        assertTrue(typing.contains("onReveal = { onEvent(AndroidStudyEvent.Reveal(currentInput)) }"))
        assertTrue(modes.contains("OutlinedButton("))
        assertTrue(modes.contains("Text(\"Reveal answer\")"))
        assertTrue(genericFeedback.contains("is AndroidStudyState.ExampleCompletion -> true"))
        assertFalse(genericFeedback.contains("is AndroidStudyState.Typing, is AndroidStudyState.ExampleCompletion -> true"))
        assertFalse(activeTyping.contains("TypingDifferenceComparison("))
        val frontTyping = activeTyping.substringAfter("} else {\n        if (!feedbackVisible)")
        assertFalse(frontTyping.substringBefore("feedbackContent()").contains("answerContract.canonicalAnswer"))
        assertTrue(typing.contains("StudyRatingBar("))
        assertTrue(typing.contains("selectedRating = state.manualRating"))
        assertTrue(typing.contains("TypingInputActions("))
        assertTrue(typing.contains("modifier = Modifier.bringIntoViewRequester(stableActionsRequester)"))
        assertFalse(typing.contains("stableActionsRequester.bringIntoView()"))
        assertTrue(typing.contains("fillViewport = true"))
        assertTrue(modes.contains("Modifier.fillMaxSize().verticalScroll(rememberScrollState())"))
        assertFalse(typing.contains("Spacer("))
        assertTrue(typing.contains("normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation)"))
        assertFalse(genericFeedback.substringAfter("if (typingSuccessPending)").substringBefore("} else {").contains("StudyRatingBar("))
        assertTrue(typing.contains("if (state.completionPending)"))
        assertTrue(typing.contains("englishExample = null"))
        assertFalse(typing.contains("pronunciation = null"))
        assertFalse(typing.substringAfter("if (state.completionPending)").substringBefore("} else {").contains("feedbackContent()"))
        assertTrue(viewModel.contains("delay(AndroidTypingSuccessPresentationPolicy.minimumDwellMillis)"))
        assertTrue(viewModel.contains("typingSuccessReady("))
        val finalize = viewModel.substringAfter("private fun finalizeTypingIfReady(")
            .substringBefore("private fun launchOperation(")
        assertFalse(finalize.contains("delay("))
        assertTrue(finalize.contains("typingPreparedNext[key]"))
        assertFalse(finalize.contains("commitTypingRating"))
        assertFalse(finalize.contains("facade::next"))
        assertTrue(screen.contains("typingLeadContent = if (state is AndroidStudyState.Typing && state.revealed)"))
        assertTrue(screen.indexOf("TypingDifferenceComparison(state.answer") < screen.indexOf("ReviewImageNavigationOverlay("))
        assertTrue(genericFeedback.contains("typingLeadContent?.invoke()"))
        assertTrue(genericFeedback.indexOf("typingLeadContent?.invoke()") < genericFeedback.indexOf("StudyAnswerSection("))
        assertTrue(typing.contains("if (!feedbackVisible)"))
        assertTrue(typing.contains("if (!state.revealed)"))
        assertTrue(genericFeedback.contains("partOfSpeech = partOfSpeechPresentation(state.partOfSpeech)"))
    }

    @Test
    fun `revealed Typing comparison is a two-line annotated hero without visual labels`() {
        val comparison = modes.substringAfter("internal fun TypingDifferenceComparison(")
            .substringBefore("private fun formatTypingSeconds")

        assertEquals(2, Regex("Text\\(\\s+androidTypingComparisonAnnotatedText\\(").findAll(comparison).count())
        assertFalse(comparison.contains("Text(\"Your answer\""))
        assertFalse(comparison.contains("Text(\"Expected\""))
        assertTrue(comparison.contains("HorizontalDivider("))
        assertTrue(comparison.contains("TextDecoration.LineThrough"))
        assertTrue(comparison.contains("TextDecoration.Underline"))
        assertTrue(comparison.contains("offsetByCodePoints"))
        assertTrue(comparison.contains("clearAndSetSemantics"))
    }

    @Test
    fun `forced Typing reveal restores projected examples without changing compact success`() {
        val reveal = screen.substringAfter("private fun StudyRevealAndFeedbackContent(")
            .substringBefore("private fun Completion(")
        val compactSuccess = modes.substringAfter("if (state.completionPending)")
            .substringBefore("} else {")

        assertTrue(reveal.contains("state.revealed && !state.completionPending"))
        assertTrue(reveal.contains("forcedTypingReveal -> state.example"))
        assertTrue(reveal.contains("forcedTypingReveal -> state.translation"))
        assertTrue(reveal.contains("englishExampleAudioPath = state.resolvedExampleEnglishAudio"))
        assertFalse(reveal.contains("vietnameseExampleAudioPath = state.resolvedExampleVietnameseAudio"))
        assertTrue(reveal.contains("allowStandaloneVietnameseExample = forcedTypingReveal"))
        assertTrue(reveal.contains("normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation)"))
        assertTrue(compactSuccess.contains("englishExample = null"))
        assertTrue(compactSuccess.contains("vietnameseExample = null"))
        assertTrue(compactSuccess.contains("StudyMediaRole.COMPACT"))
        assertTrue(compactSuccess.contains("normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation)"))
        assertFalse(compactSuccess.contains("state.example"))
        assertFalse(compactSuccess.contains("state.translation"))
        assertTrue(answerSection.contains("allowStandaloneVietnameseExample && !vietnameseExample.isNullOrBlank()"))
        assertTrue(answerSection.contains("englishExample?.takeIf(String::isNotBlank)?.let"))
        assertTrue(answerSection.contains("vietnameseExample?.takeIf(String::isNotBlank)?.let"))
    }

    @Test
    fun `forced Typing reveal owns one-shot answer autoplay without advancing`() {
        val revealAutoplay = screen.substringAfter("LaunchedEffect(\n        itemKey,\n        (state as? AndroidStudyState.Typing)?.revealed")
            .substringBefore("LaunchedEffect(itemKey, (state as? AndroidStudyState.Typing)?.completionPending)")

        assertTrue(revealAutoplay.contains("shouldStartTypingRevealAnswerAutoplay("))
        assertTrue(screen.contains("var revealAudioStarted by rememberSaveable(itemKey)"))
        assertTrue(revealAutoplay.indexOf("audioController.stop()") < revealAutoplay.indexOf("restartAudio("))
        assertTrue(revealAutoplay.contains("activeRole = null"))
        assertTrue(revealAutoplay.contains("AudioRole.EXPECTED_ANSWER"))
        assertTrue(revealAutoplay.contains("typing.resolvedExpectedAnswerAudio"))
        assertTrue(revealAutoplay.contains("false"))
        assertFalse(revealAutoplay.contains("AndroidStudyEvent.Next"))
        assertFalse(revealAutoplay.contains("AudioRole.EXAMPLE_ENGLISH"))
        assertTrue(revealAutoplay.indexOf("revealAudioStarted = true") < revealAutoplay.indexOf("restartAudio("))
    }

    @Test
    fun `Typing reveal autoplay guard is transition keyed and excludes exact success`() {
        assertTrue(shouldStartTypingRevealAnswerAutoplay(true, false, false))
        assertFalse(shouldStartTypingRevealAnswerAutoplay(true, false, true))
        assertFalse(shouldStartTypingRevealAnswerAutoplay(false, false, false))
        assertFalse(shouldStartTypingRevealAnswerAutoplay(true, true, false))
    }

    @Test
    fun `Typing success trace covers every monotonic transaction boundary in debug only`() {
        listOf(
            "exactMatch", "compactSuccessVisible", "audioStart", "audioCompletionCallback",
            "dwellComplete", "commitStart", "commitEnd", "nextRequested",
            "nextStatePublished", "nextVisible", "backendPrepareStart", "backendPrepareComplete"
        ).forEach { event ->
            assertTrue(listOf(facade, screen, viewModel, trace).any { it.contains("\"$event\"") }, event)
        }
        assertTrue(trace.contains("BuildConfig.DEBUG"))
        assertTrue(trace.contains("SystemClock.elapsedRealtime()"))
        assertTrue(trace.contains("pendingAudio="))
        assertTrue(trace.contains("pendingDwell="))
        assertTrue(trace.contains("activeAudioRole="))
        assertTrue(audio.contains("AndroidAudioPlaybackEvent.Prepared"))
        assertTrue(audio.contains("AndroidAudioPlaybackEvent.Completed"))
        assertTrue(audio.contains("durationMillis"))
        assertTrue(audio.contains("positionMillis"))
    }

    @Test
    fun `Typing backend commit and next preparation overlap audio but publication remains gated`() {
        assertTrue(viewModel.contains("typingPreparedNext"))
        assertTrue(viewModel.contains("facade.commitTypingRatingAndPrepareNext(updated, updated.manualRating)"))
        assertTrue(viewModel.indexOf("publish(updated)") < viewModel.indexOf("backendPrepareStart"))
        val finalize = viewModel.substringAfter("private fun finalizeTypingIfReady(")
            .substringBefore("private fun launchOperation(")
        assertTrue(finalize.contains("key in typingPreparedNext"))
        assertTrue(finalize.contains("typingPreparedNext[key] ?: return state"))
        assertTrue(finalize.contains("return prepared"))
        assertFalse(finalize.contains("commitTypingRating"))
        assertFalse(finalize.contains("facade::next"))
        assertTrue(facade.contains("commitTypingRatingAndPrepareNext"))
        assertTrue(facade.contains("prepareTypingNext"))
        assertTrue(facade.contains("skipped=true combinedAdvance=true"))
    }

    @Test
    fun `exact Typing success preserves IME while Reveal still dismisses it`() {
        val ending = screen.substringAfter("val preserveTypingIme =")
            .substringBefore("val layoutPolicy =")
        assertTrue(ending.contains("state.completionPending && state.outcome == RecallOutcome.CORRECT"))
        assertTrue(ending.contains("if (isEnded && !preserveTypingIme)"))
        assertTrue(ending.contains("keyboardController?.hide()"))
        assertTrue(ending.contains("focusManager.clearFocus()"))
        assertTrue(ending.contains("bringIntoViewRequester.bringIntoView()"))
        assertTrue(foundation.contains("LaunchedEffect(planId, enabled)"))
        assertTrue(foundation.contains("focusRequester.requestFocus()"))
        assertTrue(modes.contains("TypingImeContinuityAnchor(state.plan.planId.value)"))
        val anchor = modes.substringAfter("private fun TypingImeContinuityAnchor(")
            .substringBefore("private fun TypingInputActions(")
        assertTrue(anchor.contains("BasicTextField("))
        assertTrue(anchor.contains("focusRequester.requestFocus()"))
    }

    @Test
    fun `continuation Typing starts with stable input-active media geometry and debug trace`() {
        val typing = modes.substringAfter("internal fun TypingStudyStage(")
            .substringBefore("private fun TypingInputActions(")
        assertTrue(typing.contains("val inputSessionActive = !feedbackVisible"))
        assertTrue(typing.contains("inputSessionActive = inputSessionActive"))
        assertTrue(typing.contains("mediaRole = mediaRole"))
        assertTrue(layoutTrace.contains("TRACE_WINDOW_MILLIS = 500L"))
        assertTrue(layoutTrace.contains("TYPING_LAYOUT"))
        listOf("imeVisible", "inputFocused", "density", "mediaRole", "availableMediaHeightDp").forEach {
            assertTrue(layoutTrace.contains(it), it)
        }
        assertTrue(layoutTrace.contains("if (trace.lastSignature == signature) return"))
        assertTrue(layoutTrace.contains("BuildConfig.DEBUG"))
    }

    @Test
    fun `Typing nested backend trace covers commit and next preparation phases`() {
        listOf(
            "resolveRating", "executeRecallLearningStart", "schedulerEnd", "persistenceStart",
            "persistenceEnd", "projectionRefreshStart", "projectionRefreshEnd", "complete"
        ).forEach { assertTrue(facade.contains("\"$it\""), it) }
        listOf(
            "queueAdvanceStart", "queueAdvanceEnd", "sessionReloadStart", "sessionReloadEnd",
            "packageReadStart", "packageReadEnd", "planResolveStart", "planResolveEnd",
            "mediaResolveStart", "mediaResolveEnd", "stateProjectionStart", "stateProjectionEnd",
            "published"
        ).forEach { assertTrue(facade.contains("\"$it\""), it) }
    }

    @Test
    fun `Typing normal success has no multi-second timeout or post-gate delay`() {
        val successSources = listOf(viewModel, screen, policy).joinToString("\n")
        listOf("3_000L", "3_500L", "4_000L", "3000L", "3500L", "4000L").forEach {
            assertFalse(successSources.contains(it), it)
        }
        assertTrue(policy.contains("audioWatchdogMillis = 120_000L"))
        assertTrue(screen.contains("withTimeoutOrNull(AndroidTypingSuccessPresentationPolicy.audioWatchdogMillis)"))
        assertTrue(screen.contains("audioFinished.complete(Unit)"))
        val finalize = viewModel.substringAfter("private fun finalizeTypingIfReady(")
            .substringBefore("private fun launchOperation(")
        assertFalse(finalize.contains("delay("))
        assertTrue(finalize.contains("return prepared"))
    }

    @Test
    fun `READY and wrong Typing share one stable action skeleton`() {
        val actions = modes.substringAfter("private fun TypingInputActions(")
            .substringBefore("@Composable\ninternal fun TypingDifferenceComparison(")

        assertTrue(actions.contains("heightIn(min = 24.dp)"))
        assertTrue(actions.contains("if (showRetry)"))
        assertTrue(actions.contains("Text(\"Retry\")"))
        assertTrue(actions.contains("Text(\"Check\")"))
        assertTrue(actions.contains("Text(\"Reveal answer\")"))
        assertEquals(2, Regex("Row\\(").findAll(actions).count())
    }
}
