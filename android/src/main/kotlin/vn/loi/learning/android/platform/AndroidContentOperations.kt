package vn.loi.learning.android.platform

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.application.contentpackaging.PackageImportBatchResult
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

enum class AndroidOperationKind { IMPORT, BACKUP, RESTORE }

sealed interface AndroidContentFailure {
    val message: String
    data class Permission(override val message: String = "Document permission was denied or lost.") : AndroidContentFailure
    data class Unavailable(override val message: String = "The selected document is unavailable.") : AndroidContentFailure
    data class InvalidPackage(override val message: String) : AndroidContentFailure
    data class Storage(override val message: String = "There is not enough writable storage.") : AndroidContentFailure
    data class Backup(override val message: String = "Backup could not be created.") : AndroidContentFailure
    data class Restore(override val message: String = "Backup validation or restore failed.") : AndroidContentFailure
    data class Interrupted(override val message: String = "The operation was interrupted. You can retry.") : AndroidContentFailure
    data class Persistence(override val message: String = "Learning data could not be updated.") : AndroidContentFailure
}

sealed interface AndroidContentOperationState {
    data object Idle : AndroidContentOperationState
    data class Running(val id: String, val kind: AndroidOperationKind) : AndroidContentOperationState
    data class Succeeded(val id: String, val kind: AndroidOperationKind, val detail: String) : AndroidContentOperationState
    data class Failed(val id: String, val kind: AndroidOperationKind, val failure: AndroidContentFailure) : AndroidContentOperationState
}

/** One-shot Android I/O boundary. Package validation and durable mutation remain in Application. */
class AndroidContentOperations(
    private val graph: AndroidApplicationGraph,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ids: () -> String = { UUID.randomUUID().toString() }
) {
    private val committed = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var active: AndroidContentOperationState.Running? = null

    fun newOperation(kind: AndroidOperationKind): AndroidContentOperationState.Running {
        active?.let { return it }
        return AndroidContentOperationState.Running(ids(), kind).also { active = it }
    }

    fun recoverInterrupted(savedId: String?, kind: AndroidOperationKind?): AndroidContentOperationState =
        if (savedId != null && kind != null && savedId !in committed)
            AndroidContentOperationState.Failed(savedId, kind, AndroidContentFailure.Interrupted())
        else AndroidContentOperationState.Idle

    suspend fun import(operation: AndroidContentOperationState.Running, displayName: String, open: () -> InputStream?): AndroidContentOperationState =
        executeOnce(operation) {
            val staging = Files.createTempDirectory(graph.directories.importDirectory, ".android-import-")
            try {
                val safeName = displayName.substringAfterLast('/').substringAfterLast('\\')
                    .replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "package.opd3" }
                open()?.use { input -> Files.copy(input, staging.resolve(safeName), StandardCopyOption.REPLACE_EXISTING) }
                    ?: return@executeOnce AndroidContentFailure.Unavailable()
                val result: PackageImportBatchResult = graph.engine.packageImporter(staging)
                    .importAllDetailed(PackageCatalogId("android-imports"))
                if (result.successfulImports.isEmpty()) {
                    AndroidContentFailure.InvalidPackage(result.failures.firstOrNull()?.message ?: "No supported package was found.")
                } else "Imported ${result.successfulImports.size} package(s)."
            } finally { staging.deleteTree() }
        }

    suspend fun backup(operation: AndroidContentOperationState.Running, open: () -> OutputStream?): AndroidContentOperationState =
        executeOnce(operation) {
            val staging = Files.createTempFile(graph.directories.rootDirectory, ".android-backup-", ".lebak")
            try {
                graph.recovery.createBackup(staging)
                open()?.use { output -> Files.newInputStream(staging).use { it.copyTo(output) } }
                    ?: return@executeOnce AndroidContentFailure.Unavailable()
                "Backup created."
            } finally { Files.deleteIfExists(staging) }
        }

    suspend fun restore(operation: AndroidContentOperationState.Running, open: () -> InputStream?): AndroidContentOperationState =
        executeOnce(operation) {
            val staging = Files.createTempFile(graph.directories.rootDirectory, ".android-restore-", ".lebak")
            try {
                open()?.use { input -> Files.copy(input, staging, StandardCopyOption.REPLACE_EXISTING) }
                    ?: return@executeOnce AndroidContentFailure.Unavailable()
                graph.recovery.restore(staging, operationActive = false)
                "Backup restored. Reopen Study to reload durable state."
            } finally { Files.deleteIfExists(staging) }
        }

    private suspend fun executeOnce(
        operation: AndroidContentOperationState.Running,
        block: () -> Any
    ): AndroidContentOperationState = withContext(ioDispatcher) {
        if (operation.id in committed || active?.id != operation.id) return@withContext AndroidContentOperationState.Idle
        try {
            when (val result = block()) {
                is AndroidContentFailure -> AndroidContentOperationState.Failed(operation.id, operation.kind, result)
                else -> AndroidContentOperationState.Succeeded(operation.id, operation.kind, result.toString()).also { committed += operation.id }
            }
        } catch (_: SecurityException) {
            AndroidContentOperationState.Failed(operation.id, operation.kind, AndroidContentFailure.Permission())
        } catch (failure: Exception) {
            val typed = when (operation.kind) {
                AndroidOperationKind.IMPORT -> AndroidContentFailure.Persistence(failure.message ?: "Import failed.")
                AndroidOperationKind.BACKUP -> AndroidContentFailure.Backup()
                AndroidOperationKind.RESTORE -> AndroidContentFailure.Restore()
            }
            AndroidContentOperationState.Failed(operation.id, operation.kind, typed)
        } finally { if (active?.id == operation.id) active = null }
    }
}

private fun Path.deleteTree() {
    if (Files.notExists(this)) return
    Files.walk(this).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
}
