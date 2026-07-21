package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun DetachPackageDialog(
    state: DetachPackageDialogState,
    onDismiss: () -> Unit,
    onDetach: () -> Unit
) {
    if (!state.visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Detach Package"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Remove this package from the collection?"
                )

                Text(
                    "Package: ${state.packageName}"
                )

                Text(
                    "Collection: ${state.collectionName}"
                )

                Text(
                    "The installed package itself will not be deleted."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDetach
            ) {
                Text(
                    "Detach"
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