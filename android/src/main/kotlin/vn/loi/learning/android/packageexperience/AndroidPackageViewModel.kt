package vn.loi.learning.android.packageexperience

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * ViewModel for Package Experience.
 *
 * Invariants:
 * - Generation guard: stale async results are discarded.
 * - Search is debounced 250 ms, cancellable, duplicate-guarded, Unicode-safe, off Main.
 * - No repository call from composable.
 * - No media bytes loaded for list rows.
 * - No graph recreation.
 * - Immutable state objects only (sealed interface, data class).
 */
class AndroidPackageViewModel(
    private val facade: AndroidPackageFacade,
    private val saved: SavedStateHandle,
    private val workerDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val mutable = MutableStateFlow<AndroidPackageContentState>(AndroidPackageContentState.Loading)
    val state: StateFlow<AndroidPackageContentState> = mutable.asStateFlow()

    private var operationGeneration = 0L
    private var searchJob: Job? = null
    private var studyJob: Job? = null

    val operationState: StateFlow<AndroidPackageOperationState>
        get() = mutableOperation.asStateFlow()
    private val mutableOperation = MutableStateFlow<AndroidPackageOperationState>(
        AndroidPackageOperationState.Idle
    )

    init {
        saved.get<String>(KEY_PACKAGE)?.let { open(it) }
    }

    /** Open a package by canonical ID. Cancels any pending search. */
    fun open(packageId: String) {
        saved[KEY_PACKAGE] = packageId
        val generation = ++operationGeneration
        searchJob?.cancel()
        studyJob?.cancel()
        viewModelScope.launch {
            mutable.value = AndroidPackageContentState.Loading
            val result = withContext(workerDispatcher) {
                facade.openPackage(InstalledPackageId(packageId), saved[KEY_QUERY] ?: "")
            }
            if (generation == operationGeneration) mutable.value = result
        }
    }

    /** Reload the current package (after an operation that changes state). */
    fun reload() {
        saved.get<String>(KEY_PACKAGE)?.let { open(it) }
    }

    /**
     * Package-local search.
     * - Debounce 250 ms.
     * - Same-query dedupe.
     * - Cancellable.
     * - Stale guard via generation.
     * - Unicode-safe (delegates to facade).
     * - Off Main thread.
     */
    fun search(query: String) {
        val current = mutable.value as? AndroidPackageContentState.Content ?: return
        if (query == current.query) return
        saved[KEY_QUERY] = query
        searchJob?.cancel()
        val generation = ++operationGeneration
        searchJob = viewModelScope.launch {
            delay(250)
            val updated = withContext(workerDispatcher) {
                facade.applySearch(current, query)
            }
            if (generation == operationGeneration) mutable.value = updated
        }
    }

    /** Clear the search query. */
    fun clearSearch() {
        val current = mutable.value as? AndroidPackageContentState.Content ?: return
        if (current.query.isEmpty()) return
        saved[KEY_QUERY] = ""
        val generation = ++operationGeneration
        searchJob?.cancel()
        viewModelScope.launch {
            val updated = withContext(workerDispatcher) {
                facade.applySearch(current, "")
            }
            if (generation == operationGeneration) mutable.value = updated
        }
    }

    /**
     * Start or continue a Study session for the current package.
     * CTA priority is managed in [AndroidPackageFacade.resolveCta].
     */
    fun studyPackage(): String? {
        val current = mutable.value
        val packageId = saved.get<String>(KEY_PACKAGE) ?: return null
        if (studyJob?.isActive == true) return null
        val id = InstalledPackageId(packageId)
        // Return null for async — caller awaits studySessionStarted state
        val generation = ++operationGeneration
        studyJob = viewModelScope.launch {
            val result = withContext(workerDispatcher) { facade.startPackage(id) }
            if (generation == operationGeneration) {
                result.fold(
                    onSuccess = { sessionId ->
                        mutableOperation.value = AndroidPackageOperationState.Succeeded("Study session started.")
                        // reload to reflect new active session in CTA
                        open(packageId)
                    },
                    onFailure = {
                        mutableOperation.value = AndroidPackageOperationState.Failed(
                            it.message ?: "Study could not start."
                        )
                    }
                )
            }
        }
        return null
    }

    /** The caller should observe [studySessionId] emitted via [mutableOperation] or use the returned value. */
    fun startStudy(onSessionStarted: (String) -> Unit) {
        val packageId = saved.get<String>(KEY_PACKAGE) ?: return
        if (studyJob?.isActive == true) return
        val id = InstalledPackageId(packageId)
        val existingSessionId = when (val current = mutable.value) {
            is AndroidPackageContentState.Content -> (current.cta as? AndroidPackageCta.ContinueLearning)?.sessionId
            is AndroidPackageContentState.Empty -> (current.cta as? AndroidPackageCta.ContinueLearning)?.sessionId
            else -> null
        }
        if (existingSessionId != null) {
            onSessionStarted(existingSessionId)
            return
        }
        val generation = ++operationGeneration
        studyJob = viewModelScope.launch {
            val result = withContext(workerDispatcher) { facade.startPackage(id) }
            if (generation == operationGeneration) {
                result.fold(
                    onSuccess = { sessionId -> onSessionStarted(sessionId) },
                    onFailure = {
                        mutableOperation.value = AndroidPackageOperationState.Failed(
                            it.message ?: "Study could not start."
                        )
                    }
                )
            }
        }
    }

    /** Acknowledge and clear a completed/failed operation message. */
    fun dismissOperation() {
        mutableOperation.value = AndroidPackageOperationState.Idle
    }

    /** Set an operation message (for export/verify/uninstall results from caller). */
    fun setOperationResult(message: String, success: Boolean) {
        mutableOperation.value = if (success)
            AndroidPackageOperationState.Succeeded(message)
        else
            AndroidPackageOperationState.Failed(message)
    }

    private companion object {
        const val KEY_PACKAGE = "package.id"
        const val KEY_QUERY = "package.query"
    }
}
