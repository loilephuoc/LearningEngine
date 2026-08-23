package vn.loi.learning.android.study.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.packageexperience.AndroidPackageQuickEditDraft

@Composable
internal fun StudyQuickEditDialog(
    draft: AndroidPackageQuickEditDraft,
    onDismiss: () -> Unit,
    onSave: (AndroidPackageQuickEditDraft, (Result<Unit>) -> Unit) -> Unit
) {
    var values by remember(draft.contentId) { mutableStateOf(listOf(draft.question, draft.answer, draft.pronunciation, draft.partOfSpeech, draft.example, draft.translation)) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val labels = listOf("Câu hỏi", "Đáp án", "Phiên âm (IPA)", "Từ loại (POS)", "Ví dụ (Example)", "Dịch nghĩa ví dụ")
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Sửa từ") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                values.forEachIndexed { index, value ->
                    OutlinedTextField(value = value, onValueChange = { updated -> values = values.toMutableList().also { it[index] = updated } },
                        label = { Text(labels[index]) }, enabled = !saving)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                saving = true
                error = null
                onSave(AndroidPackageQuickEditDraft(draft.contentId, values[0], values[1], values[2], values[3], values[4], values[5])) { result ->
                    saving = false
                    error = result.exceptionOrNull()?.message
                }
            }) { Text(if (saving) "Đang lưu…" else "Lưu") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Hủy") } }
    )
}
