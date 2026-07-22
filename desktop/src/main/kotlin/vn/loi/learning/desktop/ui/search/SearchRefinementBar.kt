package vn.loi.learning.desktop.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SearchRefinementBar(
    presentation: SearchRefinementPresentation,
    resetEnabled: Boolean,
    onAction: (SearchRefinementAction) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = presentation.contentDescription
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(presentation.label)
            OutlinedButton(
                onClick = onReset,
                enabled = resetEnabled,
                modifier = Modifier.semantics {
                    contentDescription = presentation.resetDescription
                }
            ) {
                Text("Reset view")
            }
        }

        if (presentation.actions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presentation.actions.forEach { action ->
                    OutlinedButton(
                        onClick = { onAction(action.action) },
                        modifier = Modifier.semantics {
                            contentDescription = action.contentDescription
                        }
                    ) {
                        Text(action.label)
                    }
                }
            }
        }
    }
}
