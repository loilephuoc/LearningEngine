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
    data object PauseTyping : AndroidStudyEvent
    data object ResumeTyping : AndroidStudyEvent
    data class OverrideRating(val rating: ReviewRating) : AndroidStudyEvent
    data class CommitTypingRating(val manualRating: ReviewRating? = null) : AndroidStudyEvent
    data object Undo : AndroidStudyEvent
    data object Home : AndroidStudyEvent
    data object RefreshHomeIfIdle : AndroidStudyEvent
}

class AndroidStudyViewModel(
    private val facade: AndroidStudyFacade,
    private val savedState: SavedStateHandle,
    private val workerDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : ViewModel() {
    private val mutableState = MutableStateFlow<AndroidStudyState>(AndroidStudyState.Loading)
    val state: StateFlow<AndroidStudyState> = mutableState.asStateFlow()
    private val operationMutex = Mutex()
    private val introductionHistory = mutableListOf<AndroidStudyState.Introduction>()
    private var introductionHistoryCursor = -1

    init {
        AndroidStartupTrace.mark("study_view_model_constructed")
        launchOperation("study_initial_load") { facade.home() }
    }

    fun onEvent(event: AndroidStudyEvent) {
        if (event is AndroidStudyEvent.Start) {
            if (mutableState.value is AndroidStudyState.PreparingMode) return
            mutableState.value = AndroidStudyState.PreparingMode(event.mode)
        }
        viewModelScope.launch {
            operationMutex.withLock {
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
                            is AndroidStudyState.Typing -> if (current.completionPending) current else facade.submitText(current, event.typedAnswer)
                            is AndroidStudyState.Runtime -> facade.submitText(current, event.typedAnswer)
                            else -> current
                        }
                    is AndroidStudyEvent.Reveal ->
                        (current as? AndroidStudyState.Runtime)?.let { facade.reveal(it, event.typedAnswer) } ?: current
                    AndroidStudyEvent.RevealIntroduction ->
                        (current as? AndroidStudyState.Introduction)?.let(facade::revealIntroduction) ?: current
                    is AndroidStudyEvent.RateIntroduction ->
                        (current as? AndroidStudyState.Introduction)?.let { facade.rateIntroduction(it, event.rating) } ?: current
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
                    AndroidStudyEvent.PauseTyping -> (current as? AndroidStudyState.Typing)?.let(facade::pauseTyping) ?: current
                    AndroidStudyEvent.ResumeTyping -> (current as? AndroidStudyState.Typing)?.let(facade::resumeTyping) ?: current
                    is AndroidStudyEvent.OverrideRating ->
                        when (current) {
                            is AndroidStudyState.Typing -> if (current.completionPending) {
                                facade.commitTypingRating(current, event.rating).let { committed ->
                                    (committed as? AndroidStudyState.Runtime)?.let(facade::next) ?: committed
                                }
                            } else current
                            is AndroidStudyState.Runtime -> facade.overridePracticeRating(current, event.rating)
                            else -> current
                        }
                    is AndroidStudyEvent.CommitTypingRating ->
                        (current as? AndroidStudyState.Typing)
                            ?.takeIf { it.completionPending }
                            ?.let { facade.commitTypingRating(it, event.manualRating) }
                            ?.let { committed -> (committed as? AndroidStudyState.Runtime)?.let(facade::next) ?: committed }
                            ?: current
                    AndroidStudyEvent.Undo -> facade.undo(current)
                    AndroidStudyEvent.Home -> facade.home()
                    AndroidStudyEvent.RefreshHomeIfIdle ->
                        if (current is AndroidStudyState.Home) facade.home() else current
                } } }.getOrElse { error ->
                        if (error is CancellationException) throw error
                        AndroidStartupTrace.write(false, "phase=study_event_failed event=${event.javaClass.simpleName} error=${error.javaClass.simpleName}")
                        AndroidStudyState.Failed("Study action failed.")
                    }
                }
                publish(updated)
                if (event is AndroidStudyEvent.AnswerChanged && updated is AndroidStudyState.Typing && updated.completionPending) {
                    viewModelScope.launch {
                        delay(TypingSuccessLifecyclePolicy.TARGET_TOTAL_MILLIS)
                        onEvent(AndroidStudyEvent.CommitTypingRating())
                    }
                }
            }
        }
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
        if (current !is AndroidStudyState.Introduction || introductionHistoryCursor <= 0) return current
        introductionHistoryCursor--
        return introductionHistory[introductionHistoryCursor].copy(revealedStage = true, historyPreview = true)
    }

    private fun nextVisited(current: AndroidStudyState): AndroidStudyState {
        if (current !is AndroidStudyState.Introduction) return current
        if (introductionHistoryCursor < introductionHistory.lastIndex) {
            introductionHistoryCursor++
            val saved = introductionHistory[introductionHistoryCursor]
            val atTail = introductionHistoryCursor == introductionHistory.lastIndex
            return saved.copy(revealedStage = if (atTail) saved.revealed else true, historyPreview = !atTail)
        }
        return facade.deferIntroduction(current)
    }

    private fun publish(state:AndroidStudyState){
        if (state is AndroidStudyState.Introduction && !state.historyPreview) {
            val existing = introductionHistory.indexOfFirst { it.learningItemId == state.learningItemId }
            if (existing >= 0) introductionHistory[existing] = state else introductionHistory += state
            introductionHistoryCursor = introductionHistory.indexOfFirst { it.learningItemId == state.learningItemId }
        }
        mutableState.value=state
        rememberSession(state)
    }

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
