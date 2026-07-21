package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun CreateCollectionDialog(
    state: CreateCollectionDialogState,
    onCollectionNameChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    if (!state.visible) {
        return
    }

    val accessibility =
        resolveCreateCollectionDialogAccessibility(
            state.libraryName
        )

    AlertDialog(
        modifier =
            Modifier.semantics {
                contentDescription =
                    accessibility.contentDescription
            },
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = accessibility.title,
                modifier =
                    Modifier.semantics {
                        heading()
                    }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Library: ${state.libraryName}"
                )

                OutlinedTextField(
                    value = state.collectionName,
                    onValueChange = onCollectionNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Collection name")
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}