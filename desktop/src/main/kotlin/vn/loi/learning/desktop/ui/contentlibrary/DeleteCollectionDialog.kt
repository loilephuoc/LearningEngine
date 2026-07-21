package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun DeleteCollectionDialog(
    state: DeleteCollectionDialogState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    if (!state.visible) {
        return
    }

    val accessibility =
        resolveDeleteCollectionDialogAccessibility(
            state.collectionName
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
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Are you sure you want to delete this collection?"
                )

                Text(
                    state.collectionName
                )

                Text(
                    "This action cannot be undone."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete
            ) {
                Text(
                    "Delete"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "Cancel"
                )
            }
        }
    )
}