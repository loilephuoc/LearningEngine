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
        } ?: reload()
    }
    fun reload() = run {
        AndroidStartupTrace.measured("library_initial_query") {
            val root = facade.loadRoot()
            if (root is AndroidLibraryState.Root) facade.searchRoot(
                root, saved[ROOT_QUERY] ?: "",
                saved.get<String>(FILTER)?.let { runCatching { AndroidLibraryFilter.valueOf(it) }.getOrNull() }
                    ?: AndroidLibraryFilter.ALL,
                saved[COLLECTION]
            ) else root
        }
    }
    fun openPackage(id: String) = openPackage(id, null)
    fun openSearchResult(packageId:String,contentId:String) { saved[PACKAGE]=packageId;saved[CONTENT]=contentId;run { facade.openPackage(InstalledPackageId(packageId),criteria(),contentId) } }
    fun search(value: String) {
        if (value == saved.get<String>(PACKAGE_QUERY)) return
        saved[PACKAGE_QUERY] = value
        val current=mutable.value as? AndroidLibraryState.PackageBrowser ?: return
        searchJob?.cancel()
        val generation=++operationGeneration
        searchJob=viewModelScope.launch { delay(250); val updated=withContext(workerDispatcher){facade.applyCriteria(current,criteria())};if(generation==operationGeneration)mutable.value=updated }
    }
    fun globalSearch(value: String) {
        if (value == saved.get<String>(ROOT_QUERY)) return
        saved[ROOT_QUERY]=value; searchJob?.cancel()
        val current=mutable.value as? AndroidLibraryState.Root ?: return
        val generation=++operationGeneration
        searchJob=viewModelScope.launch {
            if (value.isNotEmpty()) delay(250)
            val updated=withContext(workerDispatcher){facade.searchRoot(current, value)}
            if(generation==operationGeneration)mutable.value=updated
        }
    }
    fun filter(filter: AndroidLibraryFilter) {
        val current=mutable.value as? AndroidLibraryState.Root ?: return
        if (current.filter == filter && current.selectedCollectionId == null) return
        saved[FILTER]=filter.name;saved[COLLECTION]=null
        publishRoot(current, filter = filter, collectionId = null)
    }
    fun openCollection(collectionId: String) {
        val current=mutable.value as? AndroidLibraryState.Root ?: return
        saved[FILTER]=AndroidLibraryFilter.PACKAGES.name;saved[COLLECTION]=collectionId
        publishRoot(current, filter = AndroidLibraryFilter.PACKAGES, collectionId = collectionId)
    }
    fun selectLearningPackage(packageId: String) {
        val current = mutable.value
        run {
            when (val selected = facade.selectLearningPackage(InstalledPackageId(packageId))) {
                is AndroidLibraryState.Failed -> selected
                else -> if (current is AndroidLibraryState.PackageBrowser) {
                    facade.openPackage(InstalledPackageId(packageId), current.criteria, current.selectedContentId)
                } else selected
            }
        }
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
    private fun criteria() = AndroidLibraryCriteria(query = saved[PACKAGE_QUERY] ?: "")
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
    private fun publishRoot(current: AndroidLibraryState.Root, filter: AndroidLibraryFilter, collectionId: String?) {
        val generation=++operationGeneration;searchJob?.cancel()
        viewModelScope.launch {
            val updated=withContext(workerDispatcher){facade.searchRoot(current, filter=filter, collectionId=collectionId)}
            if(generation==operationGeneration)mutable.value=updated
        }
    }
    private companion object {
        const val PACKAGE="library.package"; const val ROOT_QUERY="library.root.query"
        const val PACKAGE_QUERY="library.package.query"; const val CONTENT="library.content"
        const val FILTER="library.filter"; const val COLLECTION="library.collection"
    }
}
