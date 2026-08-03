package vn.loi.learning.android.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus

sealed interface AndroidStudyEvent {
    data class AnswerChanged(val value: String) : AndroidStudyEvent
    data object Reveal : AndroidStudyEvent
    data object Retry : AndroidStudyEvent
    data object Next : AndroidStudyEvent
}

class AndroidStudyViewModel(
    private val facade: AndroidStudyFacade,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow<AndroidStudyState>(AndroidStudyState.Empty)
    val state: StateFlow<AndroidStudyState> = mutableState.asStateFlow()

    init {
        mutableState.value = facade.load(savedState[SESSION_ID])
        rememberSession(mutableState.value)
    }

    fun onEvent(event: AndroidStudyEvent) {
        viewModelScope.launch {
            val current = mutableState.value
            val updated = when (event) {
                is AndroidStudyEvent.AnswerChanged -> {
                    val typing = current as? AndroidStudyState.Typing ?: return@launch
                    val edited = facade.updateAnswer(typing, event.value)
                    facade.submitIfCorrect(edited)
                }
                AndroidStudyEvent.Reveal ->
                    (current as? AndroidStudyState.Typing)?.let(facade::reveal) ?: current
                AndroidStudyEvent.Retry ->
                    (current as? AndroidStudyState.Typing)?.copy(
                        answer = "",
                        evaluation = TypingAnswerEvaluationStatus.EMPTY
                    ) ?: current
                AndroidStudyEvent.Next ->
                    (current as? AndroidStudyState.Typing)?.let(facade::next) ?: current
            }
            mutableState.value = updated
            rememberSession(updated)
        }
    }

    private fun rememberSession(state: AndroidStudyState) {
        savedState[SESSION_ID] = (state as? AndroidStudyState.Typing)?.plan?.sessionId?.value
    }

    private companion object {
        const val SESSION_ID = "study.sessionId"
    }
}
