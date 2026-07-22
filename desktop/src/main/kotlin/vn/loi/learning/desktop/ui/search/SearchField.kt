package vn.loi.learning.desktop.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SearchField(
    query: String,
    label: String,
    summary: SearchResultSummary,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    keyboardPresentation: SearchKeyboardPresentation? = null,
    guidance: SearchQueryGuidancePresentation? = null
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        buildString {
                            append("$label. ${summary.contentDescription}")
                            guidance?.let { append(" ${it.contentDescription}") }
                        }
                },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier =
                    Modifier
                        .weight(1f)
                        .let { fieldModifier ->
                            if (focusRequester == null) fieldModifier
                            else fieldModifier.focusRequester(focusRequester)
                        },
                label = { Text(label) },
                placeholder = guidance?.let { presentation ->
                    { Text(presentation.placeholder) }
                },
                supportingText = guidance?.let { presentation ->
                    { Text(presentation.supportingText) }
                },
                singleLine = true
            )

            OutlinedButton(
                onClick = onClearQuery,
                enabled = query.isNotBlank()
            ) {
                Text("Clear")
            }
        }

        Text(summary.label)

        keyboardPresentation?.let { presentation ->
            Text(
                text = presentation.visibleLabel,
                modifier = Modifier.semantics {
                    contentDescription = presentation.contentDescription
                }
            )
        }
    }
}
