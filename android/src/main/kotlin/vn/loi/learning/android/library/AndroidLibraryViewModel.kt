package vn.loi.learning.android.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import vn.loi.learning.domain.library.model.InstalledPackageId

class AndroidLibraryViewModel(private val facade: AndroidLibraryFacade, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow<AndroidLibraryState>(AndroidLibraryState.Loading)
    val state = mutable.asStateFlow()
    private var searchJob: Job? = null
    init { saved.get<String>(PACKAGE)?.let(::openPackage) ?: reload() }
    fun reload() = run { facade.loadRoot() }
    fun openPackage(id: String) { saved[PACKAGE] = id; run { facade.openPackage(InstalledPackageId(id), criteria()) } }
    fun search(value: String) {
        saved[QUERY] = value
        val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return
        searchJob?.cancel()
        searchJob=viewModelScope.launch { delay(250); mutable.value=withContext(Dispatchers.Default){facade.applyCriteria(current,criteria())} }
    }
    fun back() { saved[PACKAGE]=null; reload() }
    private fun criteria() = AndroidLibraryCriteria(query = saved[QUERY] ?: "")
    private fun run(action: () -> AndroidLibraryState) { viewModelScope.launch { mutable.value=AndroidLibraryState.Loading; mutable.value=withContext(Dispatchers.IO){action()} } }
    private companion object { const val PACKAGE="library.package"; const val QUERY="library.query" }
}
