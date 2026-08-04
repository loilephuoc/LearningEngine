package vn.loi.learning.android.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import vn.loi.learning.android.platform.AndroidStartupTrace
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.memory.model.ReviewRating

sealed interface AndroidStudyEvent {
    data class Start(val entry: AndroidSessionEntry) : AndroidStudyEvent
    data object Resume : AndroidStudyEvent
    data class OpenSession(val sessionId: String) : AndroidStudyEvent
    data class AnswerChanged(val value: String) : AndroidStudyEvent
    data class Choose(val choiceId: String) : AndroidStudyEvent
    data object Submit : AndroidStudyEvent
    data object Reveal : AndroidStudyEvent
    data object Retry : AndroidStudyEvent
    data object Next : AndroidStudyEvent
    data class OverrideRating(val rating: ReviewRating) : AndroidStudyEvent
    data object Undo : AndroidStudyEvent
    data object Home : AndroidStudyEvent
}

class AndroidStudyViewModel(
    private val facade: AndroidStudyFacade,
    private val savedState: SavedStateHandle,
    private val workerDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : ViewModel() {
    private val mutableState = MutableStateFlow<AndroidStudyState>(AndroidStudyState.Loading)
    val state: StateFlow<AndroidStudyState> = mutableState.asStateFlow()

    init { AndroidStartupTrace.mark("study_view_model_constructed");viewModelScope.launch { publish(withContext(workerDispatcher){AndroidStartupTrace.measured("study_initial_load"){facade.load(savedState[SESSION_ID])}}) } }

    fun onEvent(event: AndroidStudyEvent) {
        viewModelScope.launch {
            val current = mutableState.value
            val updated = withContext(workerDispatcher) { AndroidStartupTrace.measured("study_event_${event.javaClass.simpleName}") { when (event) {
                is AndroidStudyEvent.Start -> facade.start(event.entry)
                AndroidStudyEvent.Resume -> facade.load(savedState[SESSION_ID])
                is AndroidStudyEvent.OpenSession -> facade.loadExact(event.sessionId)
                is AndroidStudyEvent.AnswerChanged -> {
                    val runtime = current as? AndroidStudyState.Runtime ?: return@withContext current
                    val edited = facade.updateAnswer(runtime, event.value)
                    if (edited is AndroidStudyState.Typing) facade.submitTypingIfCorrect(edited) else edited
                }
                is AndroidStudyEvent.Choose ->
                    (current as? AndroidStudyState.MultipleChoice)?.let { facade.choose(it, event.choiceId) } ?: current
                AndroidStudyEvent.Submit ->
                    (current as? AndroidStudyState.Runtime)?.let(facade::submitText) ?: current
                AndroidStudyEvent.Reveal ->
                    (current as? AndroidStudyState.Runtime)?.let(facade::reveal) ?: current
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
                is AndroidStudyEvent.OverrideRating ->
                    (current as? AndroidStudyState.Runtime)?.let { facade.overridePracticeRating(it, event.rating) } ?: current
                AndroidStudyEvent.Undo -> facade.undo(current)
                AndroidStudyEvent.Home -> facade.home()
            } } }
            publish(updated)
        }
    }

    private fun publish(state:AndroidStudyState){mutableState.value=state;rememberSession(state)}

    private fun rememberSession(state: AndroidStudyState) {
        savedState[SESSION_ID] = when (state) {
            is AndroidStudyState.Runtime -> state.plan.sessionId.value
            is AndroidStudyState.Completion -> state.sessionId
            else -> return
        }
    }

    private companion object { const val SESSION_ID = "study.sessionId" }
}
