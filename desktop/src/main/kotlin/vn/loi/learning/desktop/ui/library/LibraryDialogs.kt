package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.loi.learning.domain.library.model.InstalledPackageId

@Composable
fun LibraryDialogHost(
    dialogState: LibraryDialogState,
    isBusy: Boolean,
    onClose: () -> Unit,
    onSubmitCreateCollection: (name: String, description: String) -> Unit,
    onSubmitRenameCollection: (newName: String) -> Unit,
    onSubmitDeleteCollection: () -> Unit,
    onSubmitAssignPackage: (installedPackageId: InstalledPackageId) -> Unit,
    onSubmitRemoveAssignment: () -> Unit,
    onSubmitArchivePackage: () -> Unit,
    onSubmitRestorePackage: () -> Unit,
    onSubmitResetPackageProgress: () -> Unit = {}
) {
    when (dialogState) {
        LibraryDialogState.None -> {}

        is LibraryDialogState.CreateCollection -> {
            var name by remember(dialogState) { mutableStateOf(dialogState.nameInput) }
            var description by remember(dialogState) { mutableStateOf(dialogState.descriptionInput) }

            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Create Collection") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Collection Name") },
                            singleLine = true,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (Optional)") },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onSubmitCreateCollection(name, description) },
                        enabled = !isBusy && name.isNotBlank()
                    ) {
                        Text(if (isBusy) "Creating..." else "Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        is LibraryDialogState.RenameCollection -> {
            var newName by remember(dialogState) { mutableStateOf(dialogState.newNameInput) }

            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Rename Collection") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("New Name") },
                            singleLine = true,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onSubmitRenameCollection(newName) },
                        enabled = !isBusy && newName.isNotBlank()
                    ) {
                        Text(if (isBusy) "Renaming..." else "Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        is LibraryDialogState.DeleteCollectionConfirm -> {
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Delete Collection") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Are you sure you want to remove '${dialogState.collectionName}' from active collections?")
                        Text(
                            text = "This soft-deletes the collection from active navigation. Installed packages are NOT uninstalled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onSubmitDeleteCollection,
                        enabled = !isBusy,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (isBusy) "Deleting..." else "Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        is LibraryDialogState.AssignPackage -> {
            var selectedPkgId by remember(dialogState) { mutableStateOf(dialogState.selectedPackageId) }

            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Assign Package to '${dialogState.collectionName}'") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (dialogState.candidatePackages.isEmpty()) {
                            Text("No candidate active packages available to assign.")
                        } else {
                            Text("Select an active package:")
                            dialogState.candidatePackages.forEach { candidate ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(!isBusy) { selectedPkgId = candidate.id }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedPkgId == candidate.id,
                                        onClick = { selectedPkgId = candidate.id },
                                        enabled = !isBusy
                                    )
                                    Text(
                                        text = "${candidate.name} (v${candidate.version})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedPkgId?.let(onSubmitAssignPackage) },
                        enabled = !isBusy && selectedPkgId != null
                    ) {
                        Text(if (isBusy) "Assigning..." else "Assign")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        is LibraryDialogState.RemoveAssignmentConfirm -> {
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Remove Package Assignment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Remove package '${dialogState.packageName}' from collection '${dialogState.collectionName}'?")
                        Text(
                            text = "The package will remain installed in your library.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onSubmitRemoveAssignment,
                        enabled = !isBusy
                    ) {
                        Text(if (isBusy) "Removing..." else "Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        is LibraryDialogState.ArchivePackageConfirm -> {
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Archive Package") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Archive '${dialogState.packageName}'?")
                        Text(
                            text = "Archiving hides the package from active learning lists while preserving all study history. You can restore it anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onSubmitArchivePackage,
                        enabled = !isBusy
                    ) {
                        Text(if (isBusy) "Archiving..." else "Archive")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        is LibraryDialogState.RestorePackageConfirm -> {
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Restore Package") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Restore '${dialogState.packageName}' to active status?")
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onSubmitRestorePackage,
                        enabled = !isBusy
                    ) {
                        Text(if (isBusy) "Restoring..." else "Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Cancel")
                    }
                }
            )
        }

        is LibraryDialogState.ResetPackageProgressConfirm -> {
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text("Đặt lại tiến độ học?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Toàn bộ lịch sử học và lịch ôn của chủ đề này sẽ bị xóa. Nội dung, hình ảnh và âm thanh vẫn được giữ lại. Thao tác này không thể hoàn tác.")
                        if (dialogState.errorMessage != null) {
                            Text(
                                text = dialogState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onSubmitResetPackageProgress,
                        enabled = !isBusy
                    ) {
                        Text(if (isBusy) "Đang đặt lại..." else "Đặt lại")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose, enabled = !isBusy) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}
