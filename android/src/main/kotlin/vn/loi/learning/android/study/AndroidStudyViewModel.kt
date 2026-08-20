package vn.loi.learning.android.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import vn.loi.learning.application.typing.TypingSuccessLifecyclePolicy
import vn.loi.learning.android.platform.AndroidStartupTrace
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.StudyMode

sealed interface AndroidStudyEvent {
    data class Start(val entry: AndroidSessionEntry, val mode: StudyMode = StudyMode.ADAPTIVE) : AndroidStudyEvent
    data object Resume : AndroidStudyEvent
    data class OpenSession(val sessionId: String) : AndroidStudyEvent
    data class AnswerChanged(val value: String) : AndroidStudyEvent
    data class Choose(val choiceId: String) : AndroidStudyEvent
    data class Submit(val typedAnswer: String? = null) : AndroidStudyEvent
    data class Reveal(val typedAnswer: String? = null) : AndroidStudyEvent
    data object RevealIntroduction : AndroidStudyEvent
    data class RateIntroduction(val rating: ReviewRating) : AndroidStudyEvent
    data object Retry : AndroidStudyEvent
    data object Next : AndroidStudyEvent
    data object PreviousVisited : AndroidStudyEvent
    data object NextVisited : AndroidStudyEvent
    data object QuickReviewUnratedAdvance : AndroidStudyEvent
    data object DifficultPracticeAdvance : AndroidStudyEvent
    data object PauseTyping : AndroidStudyEvent
    data object ResumeTyping : AndroidStudyEvent
    data object CheckTypingTimeout : AndroidStudyEvent
    data object ToggleTypingViAutoplayMute : AndroidStudyEvent
    data class OverrideRating(val rating: ReviewRating) : AndroidStudyEvent
    data class SelectTypingRatingOverride(val rating: ReviewRating) : AndroidStudyEvent
    data object TypingSuccessAudioCompleted : AndroidStudyEvent
    data object TypingSuccessDwellCompleted : AndroidStudyEvent
    data object Undo : AndroidStudyEvent
    data object Home : AndroidStudyEvent
    data object EnsureHome : AndroidStudyEvent
    data object ProjectHome : AndroidStudyEvent
    data object RefreshHomeIfIdle : AndroidStudyEvent
    data object RefreshHud : AndroidStudyEvent
    data class ChangeInsightsScope(val scope: vn.loi.learning.android.dashboard.AndroidInsightsScope) : AndroidStudyEvent
}

class AndroidStudyViewModel(
    private val facade: AndroidStudyFacade,
    private val savedState: SavedStateHandle,
    private val workerDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    typingViMutedInitially: Boolean = false,
    private val onTypingViMutedChanged: (Boolean) -> Unit = {}
) : ViewModel() {
    private val mutableState = MutableStateFlow<AndroidStudyState>(AndroidStudyState.Loading)
    val state: StateFlow<AndroidStudyState> = mutableState.asStateFlow()
    private val mutableQuickReviewSummary = MutableStateFlow<QuickReviewSessionInsights?>(null)
    val quickReviewSummary: StateFlow<QuickReviewSessionInsights?> = mutableQuickReviewSummary.asStateFlow()
    private val operationMutex = Mutex()
    private val reviewHistory = mutableListOf<AndroidStudyState.Runtime>()
    private var reviewHistoryCursor = -1
    private var reviewHistorySessionId: String? = null
    private val typingAudioCompleted = mutableSetOf<String>()
    private val typingDwellCompleted = mutableSetOf<String>()
    private val typingDwellScheduled = mutableSetOf<String>()
    private val typingBackendStarted = mutableSetOf<String>()
    private val typingPreparedNext = mutableMapOf<String, AndroidStudyState>()
    private var typingViMuted = typingViMutedInitially
    private var homeSnapshotValid = false
    private val quickReviewInsights = QuickReviewInsightsAccumulator()

    private val studyControllerTarget = object : vn.loi.learning.android.controller.StudyControllerTarget {
        override fun currentContext(): vn.loi.learning.android.controller.ControllerContext {
            return when (val s = mutableState.value) {
                is AndroidStudyState.Introduction -> if (!s.revealedStage) vn.loi.learning.android.controller.ControllerContext.STUDY_QUESTION else vn.loi.learning.android.controller.ControllerContext.STUDY_RATING
                is AndroidStudyState.Typing -> if (!s.revealed && !s.completed) vn.loi.learning.android.controller.ControllerContext.STUDY_QUESTION else vn.loi.learning.android.controller.ControllerContext.STUDY_RATING
                is AndroidStudyState.Runtime -> if (!s.completed) vn.loi.learning.android.controller.ControllerContext.STUDY_QUESTION else vn.loi.learning.android.controller.ControllerContext.STUDY_RATING
                else -> vn.loi.learning.android.controller.ControllerContext.GLOBAL
            }
        }

        override suspend fun revealAnswer(): Boolean {
            val s = mutableState.value
            if (s is AndroidStudyState.Introduction && !s.revealedStage) {
                executeEventSync(AndroidStudyEvent.RevealIntroduction)
                return true
            }
            if (s is AndroidStudyState.Typing && !s.revealed && !s.completed) {
                executeEventSync(AndroidStudyEvent.Reveal(s.answer))
                return true
            }
            if (s is AndroidStudyState.Runtime && !s.completed) {
                executeEventSync(AndroidStudyEvent.Reveal())
                return true
            }
            return false
        }

        override suspend fun continueCurrentMode(): Boolean {
            val s = mutableState.value
            val event = StudyContinueCommandResolver.resolveContinueEvent(s)
            if (event != null) {
                executeEventSync(event)
                return true
            }
            return false
        }

        override suspend fun rate(rating: ReviewRating): Boolean {
            val s = mutableState.value
            if (s is AndroidStudyState.Introduction && s.revealedStage && !s.historyPreview) {
                executeEventSync(AndroidStudyEvent.RateIntroduction(rating))
                return true
            }
            if (s is AndroidStudyState.Typing && (s.revealed || s.completed)) {
                executeEventSync(AndroidStudyEvent.OverrideRating(rating))
                return true
            }
            if (s is AndroidStudyState.Runtime && s.completed) {
                executeEventSync(AndroidStudyEvent.OverrideRating(rating))
                return true
            }
            return false
        }

        override suspend fun next(): Boolean {
            val s = mutableState.value
            if (s is AndroidStudyState.Runtime) {
                if (s.navigation.canNext) {
                    executeEventSync(AndroidStudyEvent.NextVisited)
                    return true
                } else if (s.completed || (s is AndroidStudyState.Introduction && s.revealedStage)) {
                    executeEventSync(AndroidStudyEvent.Next)
                    return true
                }
            }
            return false
        }

        override suspend fun previous(): Boolean {
            val s = mutableState.value
            if (s is AndroidStudyState.Runtime && s.navigation.canPrevious) {
                executeEventSync(AndroidStudyEvent.PreviousVisited)
                return true
            }
            return false
        }

        override fun replayAudio(): Boolean {
            return mutableState.value is AndroidStudyState.Runtime
        }

        override fun loopPrimaryAudio(): Boolean {
            val audioPath = primaryEnglishAudio() ?: primaryVietnameseAudio()
            val itemKey = (mutableState.value as? AndroidStudyState.Runtime)?.reviewItemKey()
            return vn.loi.learning.android.controller.StudyControllerBridge.playAudio(
                itemKey,
                audioPath,
                "EXPECTED_ANSWER",
                isLooping = true,
                reason = vn.loi.learning.android.controller.StudyAudioReason.MANUAL_LOOP
            )
        }

        override fun loopExampleEnglish(): Boolean {
            val audioPath = exampleEnglishAudio()
            val itemKey = (mutableState.value as? AndroidStudyState.Runtime)?.reviewItemKey()
            return vn.loi.learning.android.controller.StudyControllerBridge.playAudio(
                itemKey,
                audioPath,
                "EXAMPLE_EN",
                isLooping = true,
                reason = vn.loi.learning.android.controller.StudyAudioReason.MANUAL_LOOP
            )
        }

        override fun primaryEnglishAudio(): String? {
            return when (val s = mutableState.value) {
                is AndroidStudyState.Introduction -> s.resolvedExpectedAnswerAudio ?: s.resolvedPromptAudio
                is AndroidStudyState.Typing -> s.resolvedPromptAudio ?: s.resolvedExpectedAnswerAudio
                is AndroidStudyState.MultipleChoice -> s.resolvedPromptAudio ?: s.resolvedExpectedAnswerAudio
                is AndroidStudyState.Listening -> s.resolvedPromptAudio
                is AndroidStudyState.ImageRecall -> s.resolvedPromptAudio ?: s.resolvedExpectedAnswerAudio
                is AndroidStudyState.ExampleCompletion -> s.resolvedPromptAudio ?: s.resolvedExpectedAnswerAudio
                else -> null
            }
        }

        override fun primaryVietnameseAudio(): String? {
            return when (val s = mutableState.value) {
                is AndroidStudyState.Introduction -> s.resolvedMeaningAudio
                is AndroidStudyState.Typing -> s.resolvedMeaningAudio
                is AndroidStudyState.MultipleChoice -> s.resolvedMeaningAudio
                is AndroidStudyState.Listening -> s.resolvedMeaningAudio
                is AndroidStudyState.ImageRecall -> s.resolvedMeaningAudio
                is AndroidStudyState.ExampleCompletion -> s.resolvedMeaningAudio
                else -> null
            }
        }

        override fun exampleEnglishAudio(): String? {
            return when (val s = mutableState.value) {
                is AndroidStudyState.Introduction -> s.resolvedExampleEnglishAudio
                is AndroidStudyState.Typing -> s.resolvedExampleEnglishAudio
                is AndroidStudyState.MultipleChoice -> s.resolvedExampleEnglishAudio
                is AndroidStudyState.Listening -> s.resolvedExampleEnglishAudio
                is AndroidStudyState.ImageRecall -> s.resolvedExampleEnglishAudio
                is AndroidStudyState.ExampleCompletion -> s.resolvedExampleEnglishAudio
                else -> null
            }
        }

        override fun exampleVietnameseAudio(): String? {
            return when (val s = mutableState.value) {
                is AndroidStudyState.Introduction -> s.resolvedExampleVietnameseAudio
                is AndroidStudyState.Typing -> s.resolvedExampleVietnameseAudio
                is AndroidStudyState.MultipleChoice -> s.resolvedExampleVietnameseAudio
                is AndroidStudyState.Listening -> s.resolvedExampleVietnameseAudio
                is AndroidStudyState.ImageRecall -> s.resolvedExampleVietnameseAudio
                is AndroidStudyState.ExampleCompletion -> s.resolvedExampleVietnameseAudio
                else -> null
            }
        }

        override fun describeState(): String {
            return when (val s = mutableState.value) {
                is AndroidStudyState.Introduction ->
                    "Intro(item=${s.learningItemId}, rev=${s.revealedStage}, hist=${s.historyPreview}, canPrev=${s.navigation.canPrevious}, canNext=${s.navigation.canNext})"
                is AndroidStudyState.Typing ->
                    "Typing(key=${s.reviewItemKey()}, rev=${s.revealed}, comp=${s.completed}, canPrev=${s.navigation.canPrevious}, canNext=${s.navigation.canNext})"
                is AndroidStudyState.Runtime ->
                    "Runtime(key=${s.reviewItemKey()}, comp=${s.completed}, canPrev=${s.navigation.canPrevious}, canNext=${s.navigation.canNext})"
                is AndroidStudyState.Home -> "Home"
                is AndroidStudyState.Completion -> "Completion"
                is AndroidStudyState.Failed -> "Failed"
                else -> s::class.java.simpleName
            }
        }

        override suspend fun awaitIdle() {
            this@AndroidStudyViewModel.awaitIdle()
        }
    }

    suspend fun awaitIdle() {
        operationMutex.withLock { }
    }

    init {
        AndroidStartupTrace.mark("study_view_model_constructed")
        vn.loi.learning.android.controller.StudyControllerBridge.register(studyControllerTarget)
        launchOperation("study_initial_load") { facade.home() }
    }

    override fun onCleared() {
        super.onCleared()
        vn.loi.learning.android.controller.StudyControllerBridge.unregister(studyControllerTarget)
    }

    fun activeStudySessionAutoPlayContentIds(): List<vn.loi.learning.domain.content.model.ContentId> {
        return facade.activeStudySessionAutoPlayContentIds()
    }

    fun resolveReviewEntryAutoPlayContentIds(entry: AndroidSessionEntry): List<vn.loi.learning.domain.content.model.ContentId> {
        return facade.resolveReviewEntryAutoPlayContentIds(entry)
    }

    fun packageContentScopeIds(): List<vn.loi.learning.domain.content.model.ContentId> {
        return facade.packageContentScopeIds()
    }

    fun onEvent(event: AndroidStudyEvent) {
        if (event is AndroidStudyEvent.Start) {
            if (mutableState.value is AndroidStudyState.PreparingMode) return
            mutableState.value = AndroidStudyState.PreparingMode(event.mode)
        }
        viewModelScope.launch {
            executeEventSync(event)
        }
    }

    suspend fun executeEventSync(event: AndroidStudyEvent): AndroidStudyState {
        if (event is AndroidStudyEvent.Start) {
            if (mutableState.value !is AndroidStudyState.PreparingMode) {
                mutableState.value = AndroidStudyState.PreparingMode(event.mode)
            }
        }
        return operationMutex.withLock {
            val current = mutableState.value
            val updated = withContext(workerDispatcher) {
                runCatching { AndroidStartupTrace.measured("study_event_${event.javaClass.simpleName}") { when (event) {
                is AndroidStudyEvent.Start -> {
                    facade.start(event.entry, event.mode)
                }
                AndroidStudyEvent.Resume -> (current as? AndroidStudyState.Home)?.model?.primaryAction
                    .let { it as? AndroidHomePrimaryAction.Resume }
                    ?.let { facade.loadExact(it.sessionId) }
                    ?: facade.load(savedState[SESSION_ID])
                is AndroidStudyEvent.OpenSession -> facade.loadExact(event.sessionId)
                is AndroidStudyEvent.AnswerChanged -> {
                    val runtime = current as? AndroidStudyState.Runtime ?: return@withContext current
                    val edited = facade.updateAnswer(runtime, event.value)
                    if (edited is AndroidStudyState.Typing) facade.submitTypingIfCorrect(edited) else edited
                }
                is AndroidStudyEvent.Choose ->
                    (current as? AndroidStudyState.MultipleChoice)?.let { facade.choose(it, event.choiceId) } ?: current
                is AndroidStudyEvent.Submit ->
                    when (current) {
                        is AndroidStudyState.Typing -> if (current.completionPending) current else {
                            val submitted = facade.submitText(current, event.typedAnswer)
                            if (submitted is AndroidStudyState.Typing && !submitted.completed &&
                                submitted.evaluation == TypingAnswerEvaluationStatus.INCORRECT
                            ) facade.reveal(submitted, event.typedAnswer) else submitted
                        }
                        is AndroidStudyState.Runtime -> facade.submitText(current, event.typedAnswer)
                        else -> current
                    }
                is AndroidStudyEvent.Reveal ->
                    (current as? AndroidStudyState.Runtime)?.let { facade.reveal(it, event.typedAnswer) } ?: current
                AndroidStudyEvent.RevealIntroduction ->
                    (current as? AndroidStudyState.Introduction)?.let(facade::revealIntroduction) ?: current
                is AndroidStudyEvent.RateIntroduction ->
                    (current as? AndroidStudyState.Introduction)?.takeIf { !it.historyPreview }?.let {
                        facade.rateIntroduction(it, event.rating, deferHud = true)
                    } ?: current
                AndroidStudyEvent.Retry -> when (current) {
                    is AndroidStudyState.Typing -> current.copy(answer = "", evaluation = TypingAnswerEvaluationStatus.EMPTY)
                    is AndroidStudyState.Listening -> current.copy(answer = "")
                    is AndroidStudyState.ImageRecall -> current.copy(answer = "")
                    is AndroidStudyState.ExampleCompletion -> current.copy(answer = "")
                    is AndroidStudyState.Failed -> current.retrySessionId?.let(facade::loadExact) ?: facade.load(savedState[SESSION_ID])
                    else -> current
                }
                AndroidStudyEvent.Next ->
                    (current as? AndroidStudyState.Runtime)?.let(facade::next) ?: current
                AndroidStudyEvent.PreviousVisited -> previousVisited(current)
                AndroidStudyEvent.NextVisited -> nextVisited(current)
                AndroidStudyEvent.QuickReviewUnratedAdvance -> nextVisited(current)
                AndroidStudyEvent.DifficultPracticeAdvance -> nextVisited(current)
                AndroidStudyEvent.PauseTyping -> (current as? AndroidStudyState.Typing)?.let(facade::pauseTyping) ?: current
                AndroidStudyEvent.ResumeTyping -> (current as? AndroidStudyState.Typing)?.let(facade::resumeTyping) ?: current
                AndroidStudyEvent.CheckTypingTimeout -> {
                    val typing = current as? AndroidStudyState.Typing
                    if (typing?.attempt?.let {
                            vn.loi.learning.application.typing.TypingForcedAgainPolicy.hasTimedOut(
                                it, vn.loi.learning.application.typing.TypingAttemptTimeSource.MONOTONIC.nowMillis()
                            )
                        } == true
                    ) facade.reveal(typing, typing.answer) else current
                }
                AndroidStudyEvent.ToggleTypingViAutoplayMute -> {
                    typingViMuted = !typingViMuted
                    onTypingViMutedChanged(typingViMuted)
                    (current as? AndroidStudyState.Typing)?.copy(viAutoplayMuted = typingViMuted) ?: current
                }
                is AndroidStudyEvent.OverrideRating ->
                    when (current) {
                        is AndroidStudyState.Typing -> current
                        is AndroidStudyState.Runtime -> facade.overridePracticeRating(current, event.rating)
                        else -> current
                    }
                is AndroidStudyEvent.SelectTypingRatingOverride ->
                    (current as? AndroidStudyState.Typing)?.takeIf { !it.revealed && !it.completed }
                        ?.copy(manualRating = event.rating) ?: current
                AndroidStudyEvent.TypingSuccessAudioCompleted -> {
                    val typing = current as? AndroidStudyState.Typing
                    typing?.plan?.planId?.value?.let(typingAudioCompleted::add)
                    finalizeTypingIfReady(typing) ?: current
                }
                AndroidStudyEvent.TypingSuccessDwellCompleted -> {
                    val typing = current as? AndroidStudyState.Typing
                    typing?.plan?.planId?.value?.let(typingDwellCompleted::add)
                    typing?.let {
                        AndroidTypingSuccessTrace.event(
                            "dwellComplete",
                            it.plan.planId.value,
                            it.plan.planId.value in typingAudioCompleted,
                            true
                        )
                    }
                    finalizeTypingIfReady(typing) ?: current
                }
                AndroidStudyEvent.Undo -> facade.undo(current)
                AndroidStudyEvent.Home -> facade.home()
                AndroidStudyEvent.EnsureHome ->
                    if (current is AndroidStudyState.Home && !homeSnapshotValid) facade.home() else current
                AndroidStudyEvent.ProjectHome -> facade.home()
                AndroidStudyEvent.RefreshHomeIfIdle ->
                    if (current is AndroidStudyState.Home) facade.home() else current
                AndroidStudyEvent.RefreshHud ->
                    (current as? AndroidStudyState.Runtime)?.let(facade::refreshHud) ?: current
                is AndroidStudyEvent.ChangeInsightsScope -> {
                    facade.updateInsightsScope(event.scope)
                    facade.home()
                }
            } } }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    AndroidStartupTrace.write(false, "phase=study_event_failed event=${event.javaClass.simpleName} error=${error.javaClass.simpleName}")
                    AndroidStudyState.Failed("Study action failed.")
                }
            }
            recordQuickReviewCompletion(event, current, updated)
            publish(updated)
            if ((event is AndroidStudyEvent.Start || event is AndroidStudyEvent.RateIntroduction) &&
                updated is AndroidStudyState.Runtime
            ) {
                viewModelScope.launch { onEvent(AndroidStudyEvent.RefreshHud) }
            }
            if (event == AndroidStudyEvent.TypingSuccessAudioCompleted ||
                event == AndroidStudyEvent.TypingSuccessDwellCompleted
            ) {
                (current as? AndroidStudyState.Typing)?.let { typing ->
                    val updatedPlanId = (updated as? AndroidStudyState.Runtime)?.plan?.planId?.value
                    if (updatedPlanId != typing.plan.planId.value) {
                        AndroidTypingSuccessTrace.event(
                            "nextStatePublished",
                            typing.plan.planId.value,
                            true,
                            true,
                            detail = "state=${updated::class.simpleName} nextPlan=${updatedPlanId.orEmpty()}"
                        )
                    }
                }
            }
            if (event is AndroidStudyEvent.AnswerChanged && updated is AndroidStudyState.Typing &&
                updated.completionPending && typingDwellScheduled.add(updated.plan.planId.value)
            ) {
                viewModelScope.launch {
                    delay(AndroidTypingSuccessPresentationPolicy.minimumDwellMillis)
                    AndroidTypingSuccessTrace.event(
                        "dwellTimerFired",
                        updated.plan.planId.value,
                        false,
                        true
                    )
                    onEvent(AndroidStudyEvent.TypingSuccessDwellCompleted)
                }
            }
            if (event is AndroidStudyEvent.AnswerChanged && updated is AndroidStudyState.Typing &&
                updated.completionPending && typingBackendStarted.add(updated.plan.planId.value)
            ) {
                val key = updated.plan.planId.value
                AndroidTypingSuccessTrace.event("backendPrepareStart", key, false, false)
                val prepared = withContext(workerDispatcher) {
                    facade.commitTypingRatingAndPrepareNext(updated, updated.manualRating)
                }
                typingPreparedNext[key] = prepared
                AndroidTypingSuccessTrace.event(
                    "backendPrepareComplete", key, false, false,
                    detail = "state=${prepared::class.simpleName} nextPlan=${(prepared as? AndroidStudyState.Runtime)?.plan?.planId?.value.orEmpty()}"
                )
            }
            updated
        }
    }

    private fun recordQuickReviewCompletion(
        event: AndroidStudyEvent,
        before: AndroidStudyState,
        after: AndroidStudyState
    ) {
        val exposure = before as? AndroidStudyState.Introduction ?: return
        if (exposure.focusedPracticeKind != vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW ||
            exposure.historyPreview
        ) return
        if (event is AndroidStudyEvent.RateIntroduction && !exposure.revealed) return
        val visitId = exposure.presentationVisitId ?: return
        val next = after as? AndroidStudyState.Runtime ?: return
        if (next.reviewSessionId() != exposure.sessionId || next.reviewItemKey() == visitId) return
        val updated = when (event) {
            AndroidStudyEvent.QuickReviewUnratedAdvance ->
                quickReviewInsights.recordSkip(exposure.sessionId, visitId)
            is AndroidStudyEvent.RateIntroduction ->
                quickReviewInsights.recordRating(exposure.sessionId, visitId, event.rating)
            else -> return
        } ?: return
        mutableQuickReviewSummary.value = updated
    }

    private fun finalizeTypingIfReady(state: AndroidStudyState.Typing?): AndroidStudyState? {
        state ?: return null
        val key = state.plan.planId.value
        if (!typingSuccessReady(
                state.completionPending,
                key in typingAudioCompleted,
                key in typingDwellCompleted,
                key in typingPreparedNext
            )
        ) return state
        val prepared = typingPreparedNext[key] ?: return state
        typingAudioCompleted -= key
        typingDwellCompleted -= key
        typingDwellScheduled -= key
        typingBackendStarted -= key
        typingPreparedNext -= key
        AndroidTypingSuccessTrace.event("nextRequested", key, true, true, detail = "prepared=true")
        return prepared
    }

    private fun launchOperation(phase: String, action: () -> AndroidStudyState) {
        viewModelScope.launch {
            operationMutex.withLock {
                val updated = withContext(workerDispatcher) {
                    runCatching { AndroidStartupTrace.measured(phase, action) }
                        .getOrElse { AndroidStudyState.Failed("Learning overview unavailable.") }
                }
                publish(updated)
            }
        }
    }

    private fun previousVisited(current: AndroidStudyState): AndroidStudyState {
        if (current !is AndroidStudyState.Runtime || reviewHistoryCursor <= 0) return current
        reviewHistory[reviewHistoryCursor] = current.withNavigation(AndroidReviewNavigation())
        reviewHistoryCursor--
        return decorateHistoryState(reviewHistory[reviewHistoryCursor])
    }

    private fun nextVisited(current: AndroidStudyState): AndroidStudyState {
        if (current !is AndroidStudyState.Runtime) return current
        if (reviewHistoryCursor < reviewHistory.lastIndex) {
            reviewHistory[reviewHistoryCursor] = current.withNavigation(AndroidReviewNavigation())
            reviewHistoryCursor++
            return decorateHistoryState(reviewHistory[reviewHistoryCursor])
        }
        return facade.next(current)
    }

    private fun publish(state: AndroidStudyState) {
        val previousState = mutableState.value
        val previousItemKey = (previousState as? AndroidStudyState.Runtime)?.reviewItemKey()

        if (state is AndroidStudyState.Runtime) {
            val sessionId = state.reviewSessionId()
            if (state is AndroidStudyState.Introduction &&
                state.focusedPracticeKind == vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW &&
                quickReviewInsights.summary?.sessionId != sessionId
            ) {
                quickReviewInsights.begin(sessionId)
            }
            if (reviewHistorySessionId != sessionId) {
                reviewHistory.clear()
                reviewHistoryCursor = -1
                reviewHistorySessionId = sessionId
            }
            val key = state.reviewItemKey()
            val existing = reviewHistory.indexOfFirst { it.reviewItemKey() == key }
            if (existing >= 0) {
                reviewHistory[existing] = state.withNavigation(AndroidReviewNavigation())
                reviewHistoryCursor = existing
            } else {
                (mutableState.value as? AndroidStudyState.Typing)
                    ?.takeIf { it.completionPending }
                    ?.let { completedTyping ->
                        val prior = reviewHistory.indexOfFirst {
                            it.reviewItemKey() == completedTyping.reviewItemKey()
                        }
                        if (prior >= 0) reviewHistory[prior] = completedTyping.copy(
                            completionPending = false,
                            navigation = AndroidReviewNavigation()
                        )
                    }
                if (reviewHistoryCursor < reviewHistory.lastIndex) {
                    reviewHistory.subList(reviewHistoryCursor + 1, reviewHistory.size).clear()
                }
                reviewHistory += state.withNavigation(AndroidReviewNavigation())
                reviewHistoryCursor = reviewHistory.lastIndex
            }
            mutableState.value = decorateHistoryState(state).let {
                if (it is AndroidStudyState.Typing) it.copy(viAutoplayMuted = typingViMuted) else it
            }
        } else {
            reviewHistory.clear()
            reviewHistoryCursor = -1
            reviewHistorySessionId = null
            mutableState.value = state
        }
        homeSnapshotValid = state is AndroidStudyState.Home
        rememberSession(state)

        val newItemKey = (state as? AndroidStudyState.Runtime)?.reviewItemKey()
        if (newItemKey != null) {
            if (newItemKey != previousItemKey) {
                if (state is AndroidStudyState.Introduction && !state.revealedStage) {
                    val entryAudio = state.resolvedMeaningAudio ?: state.resolvedPromptAudio
                    if (!entryAudio.isNullOrBlank() && vn.loi.learning.android.controller.StudyControllerBridge.claimAutoplay(newItemKey, "MEANING")) {
                        vn.loi.learning.android.controller.StudyControllerBridge.playAudio(
                            itemKey = newItemKey,
                            path = entryAudio,
                            role = "MEANING",
                            isLooping = false,
                            reason = vn.loi.learning.android.controller.StudyAudioReason.ITEM_ENTRY
                        )
                    }
                } else if (previousItemKey != null) {
                    vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                        vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                    )
                }
            } else {
                val intro = state as? AndroidStudyState.Introduction
                val prevRevealed = (previousState as? AndroidStudyState.Introduction)?.revealedStage == true
                val nowRevealed = intro?.revealedStage == true
                if (!prevRevealed && nowRevealed && intro != null) {
                    val revealAudio = intro.resolvedExpectedAnswerAudio ?: intro.resolvedPromptAudio
                    if (!revealAudio.isNullOrBlank() && vn.loi.learning.android.controller.StudyControllerBridge.claimAutoplay(newItemKey, "EXPECTED_ANSWER")) {
                        vn.loi.learning.android.controller.StudyControllerBridge.playAudio(
                            itemKey = newItemKey,
                            path = revealAudio,
                            role = "EXPECTED_ANSWER",
                            isLooping = true,
                            reason = vn.loi.learning.android.controller.StudyAudioReason.REVEAL
                        )
                    }
                }
            }
        } else {
            vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
            )
        }
    }

    private fun decorateHistoryState(state: AndroidStudyState.Runtime): AndroidStudyState.Runtime =
        state.withNavigation(AndroidReviewNavigation(
            canPrevious = reviewHistoryCursor > 0,
            canNext = reviewHistoryCursor < reviewHistory.lastIndex || reviewNavigationCanAdvance(state),
            historyPreview = reviewHistoryCursor < reviewHistory.lastIndex
        ))

    private fun rememberSession(state: AndroidStudyState) {
        savedState[SESSION_ID] = when (state) {
            is AndroidStudyState.Introduction -> state.sessionId
            is AndroidStudyState.Runtime -> state.plan?.sessionId?.value ?: return
            is AndroidStudyState.Completion -> state.sessionId
            else -> return
        }
    }

    private companion object {
        const val SESSION_ID = "study.sessionId"
    }
}

private fun AndroidStudyState.Runtime.reviewSessionId(): String =
    plan?.sessionId?.value ?: (this as AndroidStudyState.Introduction).sessionId

private fun AndroidStudyState.Runtime.reviewItemKey(): String =
    plan?.planId?.value ?: (this as AndroidStudyState.Introduction).let {
        it.presentationVisitId ?: it.learningItemId
    }

private fun reviewNavigationCanAdvance(state: AndroidStudyState.Runtime): Boolean = when (state) {
    is AndroidStudyState.Introduction -> state.revealed
    is AndroidStudyState.Typing -> state.completed && !state.completionPending
    is AndroidStudyState.ExampleCompletion -> state.completed || state.revealed
    else -> state.completed
}

private fun AndroidStudyState.Runtime.withNavigation(value: AndroidReviewNavigation): AndroidStudyState.Runtime = when (this) {
    is AndroidStudyState.Introduction -> copy(navigation = value, historyPreview = value.historyPreview)
    is AndroidStudyState.Typing -> copy(navigation = value)
    is AndroidStudyState.MultipleChoice -> copy(navigation = value)
    is AndroidStudyState.Listening -> copy(navigation = value)
    is AndroidStudyState.ImageRecall -> copy(navigation = value)
    is AndroidStudyState.ExampleCompletion -> copy(navigation = value)
}

internal fun typingSuccessReady(
    completionPending: Boolean,
    audioCompleted: Boolean,
    dwellCompleted: Boolean,
    backendPrepared: Boolean
): Boolean = completionPending && audioCompleted && dwellCompleted && backendPrepared
