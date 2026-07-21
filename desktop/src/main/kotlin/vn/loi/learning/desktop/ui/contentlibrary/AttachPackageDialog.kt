package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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

    val accessibility =
        resolveAttachPackageDialogAccessibility(
            collectionName = state.collectionName,
            availablePackageCount =
                state.availablePackages.size
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
                    "Collection: ${state.collectionName}"
                )

                state.availablePackages.forEach { pkg ->
                    val optionAccessibility =
                        resolveAttachPackageOptionAccessibility(
                            packageName = pkg.name,
                            selected =
                                pkg.id ==
                                    state.selectedPackageId
                        )

                    OutlinedButton(
                        modifier =
                            Modifier.semantics {
                                selected =
                                    optionAccessibility.selected
                                contentDescription =
                                    optionAccessibility.contentDescription
                            },
                        onClick = {
                            onPackageSelected(pkg.id)
                        }
                    ) {
                        Text(
                            if (optionAccessibility.selected) {
                                "✓ ${optionAccessibility.name}"
                            } else {
                                optionAccessibility.name
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