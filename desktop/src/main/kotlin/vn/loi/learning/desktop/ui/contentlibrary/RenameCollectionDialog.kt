package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * Dialog nhập tên mới cho LibraryCollection.
 */
@Composable
fun RenameCollectionDialog(
    state: RenameCollectionDialogState,
    onCollectionNameChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    if (!state.visible) {
        return
    }

    val accessibility =
        resolveRenameCollectionDialogAccessibility(
            state.currentName
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
                    text =
                        "Current name: ${state.currentName}"
                )

                OutlinedTextField(
                    value = state.collectionName,
                    onValueChange =
                        onCollectionNameChanged,
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(
                            "Collection name"
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRename,
                enabled =
                    state.collectionName
                        .isNotBlank()
            ) {
                Text(
                    "Rename"
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