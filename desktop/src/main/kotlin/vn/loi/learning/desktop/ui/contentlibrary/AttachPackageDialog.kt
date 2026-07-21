package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun AttachPackageDialog(
    state: AttachPackageDialogState,
    onPackageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onAttach: () -> Unit
) {
    if (!state.visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Attach Package")
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Collection: ${state.collectionName}"
                )

                state.availablePackages.forEach { pkg ->
                    OutlinedButton(
                        onClick = {
                            onPackageSelected(pkg.id)
                        }
                    ) {
                        Text(
                            if (pkg.id == state.selectedPackageId) {
                                "✓ ${pkg.name}"
                            } else {
                                pkg.name
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled =
                    state.selectedPackageId.isNotBlank(),
                onClick = onAttach
            ) {
                Text("Attach")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}