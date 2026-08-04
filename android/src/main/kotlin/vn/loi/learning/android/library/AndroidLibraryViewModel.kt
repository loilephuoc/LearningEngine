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
import vn.loi.learning.android.platform.AndroidStartupTrace

class AndroidLibraryViewModel(
    private val facade: AndroidLibraryFacade,
    private val saved: SavedStateHandle,
    private val workerDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val mutable = MutableStateFlow<AndroidLibraryState>(AndroidLibraryState.Loading)
    val state = mutable.asStateFlow()
    private var searchJob: Job? = null
    private var studyLaunchJob: Job? = null
    private var operationGeneration = 0L
    init {
        saved.get<String>(PACKAGE)?.let { packageId ->
            restorePackage(packageId, saved[CONTENT])
        } ?: saved.get<String>(QUERY)?.takeIf(String::isNotBlank)?.let { query ->
            run { facade.searchGlobal(query) }
        } ?: reload()
    }
    fun reload() = run { AndroidStartupTrace.measured("library_initial_query") { facade.loadRoot() } }
    fun openPackage(id: String) = openPackage(id, null)
    fun openSearchResult(packageId:String,contentId:String) { saved[PACKAGE]=packageId;saved[CONTENT]=contentId;run { facade.openPackage(InstalledPackageId(packageId),criteria(),contentId) } }
    fun search(value: String) {
        saved[QUERY] = value
        val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return
        searchJob?.cancel()
        val generation=++operationGeneration
        searchJob=viewModelScope.launch { delay(250); val updated=withContext(workerDispatcher){facade.applyCriteria(current,criteria())};if(generation==operationGeneration)mutable.value=updated }
    }
    fun globalSearch(value: String) {
        saved[QUERY]=value; searchJob?.cancel()
        val generation=++operationGeneration
        searchJob=viewModelScope.launch { delay(250);val updated=withContext(workerDispatcher){facade.searchGlobal(value)};if(generation==operationGeneration)mutable.value=updated }
    }
    fun select(id: String) { val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return;invalidatePending();saved[CONTENT]=id; mutable.value=facade.select(current,id) }
    fun beginEdit() { val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return;invalidatePending();mutable.value=facade.beginEdit(current) }
    fun updateDraft(draft: AndroidItemDraft) { val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return;invalidatePending();mutable.value=current.copy(draft=draft) }
    fun saveEdit() { val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return; val draft=current.draft ?: return; run { facade.saveEdit(current,draft) } }
    fun openLessons() { val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return; run { facade.openLessons(InstalledPackageId(current.pkg.id)) } }
    fun startPackage() { val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return; launchStudy { facade.startPackage(InstalledPackageId(current.pkg.id)) } }
    fun startLesson(name:String) { val current=mutable.value as? AndroidLibraryState.Lessons ?: return; launchStudy { facade.startLesson(InstalledPackageId(current.pkg.id),name) } }
    fun startSelected() { val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return; val id=current.selectedContentId ?: return; launchStudy { facade.startSelection(InstalledPackageId(current.pkg.id),setOf(vn.loi.learning.domain.content.model.ContentId(id))) } }
    fun consumeStudyStarted(sessionId:String) {
        val current=mutable.value as? AndroidLibraryState.StudyStarted ?: return
        if(current.sessionId!=sessionId)return
        val packageId=saved.get<String>(PACKAGE)
        if(packageId==null)reload() else restorePackage(packageId,saved[CONTENT])
    }
    fun back() { saved[PACKAGE]=null; reload() }
    private fun criteria() = AndroidLibraryCriteria(query = saved[QUERY] ?: "")
    private fun openPackage(id: String, selectedContentId: String?) {
        saved[PACKAGE] = id
        run { facade.openPackage(InstalledPackageId(id), criteria(), selectedContentId) }
    }
    private fun restorePackage(id: String, selectedContentId: String?) {
        run {
            when (val restored = facade.openPackage(InstalledPackageId(id), criteria(), selectedContentId)) {
                is AndroidLibraryState.Failed -> {
                    saved[PACKAGE] = null
                    saved[CONTENT] = null
                    facade.loadRoot()
                }
                else -> restored
            }
        }
    }
    private fun run(action: () -> AndroidLibraryState) { val generation=++operationGeneration;searchJob?.cancel();viewModelScope.launch { mutable.value=AndroidLibraryState.Loading;val updated=withContext(workerDispatcher){action()};if(generation==operationGeneration)mutable.value=updated } }
    private fun launchStudy(action: () -> AndroidLibraryState) {
        if (studyLaunchJob?.isActive == true || mutable.value is AndroidLibraryState.StudyStarted) return
        val generation=++operationGeneration
        searchJob?.cancel()
        studyLaunchJob=viewModelScope.launch { mutable.value=AndroidLibraryState.Loading;val updated=withContext(workerDispatcher){action()};if(generation==operationGeneration)mutable.value=updated }
    }
    private fun invalidatePending(){operationGeneration+=1;searchJob?.cancel()}
    private companion object { const val PACKAGE="library.package"; const val QUERY="library.query"; const val CONTENT="library.content" }
}
