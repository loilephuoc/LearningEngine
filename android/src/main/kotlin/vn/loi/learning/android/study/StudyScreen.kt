package vn.loi.learning.android.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus

@Composable
fun StudyScreen(
    state: AndroidStudyState,
    onEvent: (AndroidStudyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        when (state) {
            AndroidStudyState.Empty -> Text("No active study session")
            AndroidStudyState.Complete -> Text("Session complete", style = MaterialTheme.typography.headlineMedium)
            is AndroidStudyState.Unsupported -> Text("${state.mode.wireId} is not available on Android yet")
            is AndroidStudyState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
            is AndroidStudyState.Typing -> TypingRuntime(state, onEvent)
        }
    }
}

@Composable
private fun TypingRuntime(state: AndroidStudyState.Typing, onEvent: (AndroidStudyEvent) -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Typing", style = MaterialTheme.typography.labelLarge)
        Text(state.prompt, style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.answer,
            onValueChange = { onEvent(AndroidStudyEvent.AnswerChanged(it)) },
            enabled = !state.completed && !state.revealed,
            singleLine = true,
            isError = state.evaluation == TypingAnswerEvaluationStatus.INCORRECT,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(AndroidStudyEvent.AnswerChanged(state.answer)) }),
            label = { Text("Answer") },
            modifier = Modifier.fillMaxWidth()
        )
        if (state.evaluation == TypingAnswerEvaluationStatus.INCORRECT && !state.completed) {
            Text("Keep trying", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { onEvent(AndroidStudyEvent.Retry) }) { Text("Retry") }
        }
        if (state.revealed || state.completed) {
            Column(
                Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (state.revealed) "Answer revealed" else "Correct")
                Text("Your answer: ${state.answer.ifBlank { "—" }}")
                Text("Expected: ${state.plan.answerContract.canonicalAnswer}")
            }
            Button(onClick = { onEvent(AndroidStudyEvent.Next) }) { Text("Continue") }
        } else {
            TextButton(onClick = { onEvent(AndroidStudyEvent.Reveal) }) { Text("Reveal answer") }
        }
    }
}
