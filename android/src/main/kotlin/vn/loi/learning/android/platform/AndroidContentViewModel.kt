package vn.loi.learning.android.platform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class AndroidContentViewModel(
    private val operations: AndroidContentOperations,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        operations.recoverInterrupted(savedState[OPERATION_ID], savedState.get<String>(OPERATION_KIND)?.let(AndroidOperationKind::valueOf))
    )
    val state: StateFlow<AndroidContentOperationState> = mutableState.asStateFlow()
    private var operationJob: Job? = null

    fun begin(kind: AndroidOperationKind) {
        val running = operations.newOperation(kind)
        savedState[OPERATION_ID] = running.id; savedState[OPERATION_KIND] = running.kind.name
        mutableState.value = running
    }

    fun importDocument(displayName: String, open: () -> InputStream?) = run(AndroidOperationKind.IMPORT) {
        operations.import(it, displayName, open)
    }
    fun createBackup(open: () -> OutputStream?) = run(AndroidOperationKind.BACKUP) { operations.backup(it, open) }
    fun restoreBackup(open: () -> InputStream?) = run(AndroidOperationKind.RESTORE) { operations.restore(it, open) }

    fun cancel() { operationJob?.cancel(); operationJob = null; clearIdentity(); mutableState.value = AndroidContentOperationState.Idle }
    fun retry() { (mutableState.value as? AndroidContentOperationState.Failed)?.let { begin(it.kind) } }

    private fun run(kind: AndroidOperationKind, action: suspend (AndroidContentOperationState.Running) -> AndroidContentOperationState) {
        val running = mutableState.value as? AndroidContentOperationState.Running ?: return
        if (running.kind != kind) return
        if (operationJob?.isActive == true) return
        operationJob = viewModelScope.launch {
            mutableState.value = action(running)
            if (mutableState.value !is AndroidContentOperationState.Running) clearIdentity()
        }
    }

    private fun clearIdentity() { savedState[OPERATION_ID] = null; savedState[OPERATION_KIND] = null }
    private companion object { const val OPERATION_ID = "content.operationId"; const val OPERATION_KIND = "content.operationKind" }
}
