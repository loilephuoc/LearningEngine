package vn.loi.learning.android.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.memory.model.ReviewRating

sealed interface AndroidStudyEvent {
    data class Start(val entry: AndroidSessionEntry) : AndroidStudyEvent
    data object Resume : AndroidStudyEvent
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
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow<AndroidStudyState>(facade.load(savedState[SESSION_ID]))
    val state: StateFlow<AndroidStudyState> = mutableState.asStateFlow()

    init { rememberSession(mutableState.value) }

    fun onEvent(event: AndroidStudyEvent) {
        viewModelScope.launch {
            val current = mutableState.value
            val updated = when (event) {
                is AndroidStudyEvent.Start -> facade.start(event.entry)
                AndroidStudyEvent.Resume -> facade.load(savedState[SESSION_ID])
                is AndroidStudyEvent.AnswerChanged -> {
                    val runtime = current as? AndroidStudyState.Runtime ?: return@launch
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
                    else -> current
                }
                AndroidStudyEvent.Next ->
                    (current as? AndroidStudyState.Runtime)?.let(facade::next) ?: current
                is AndroidStudyEvent.OverrideRating ->
                    (current as? AndroidStudyState.Runtime)?.let { facade.overridePracticeRating(it, event.rating) } ?: current
                AndroidStudyEvent.Undo -> facade.undo(current)
                AndroidStudyEvent.Home -> facade.home()
            }
            mutableState.value = updated
            rememberSession(updated)
        }
    }

    private fun rememberSession(state: AndroidStudyState) {
        savedState[SESSION_ID] = when (state) {
            is AndroidStudyState.Runtime -> state.plan.sessionId.value
            is AndroidStudyState.Completion -> state.sessionId
            else -> return
        }
    }

    private companion object { const val SESSION_ID = "study.sessionId" }
}
