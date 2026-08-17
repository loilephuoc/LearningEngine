package vn.loi.learning.android.autoplay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutoPlayViewModel(
    private val contentSelector: AutoPlayContentSelector,
    private val preferencesController: AutoPlayPreferencesController,
    private val coordinator: AutoPlayRuntimeCoordinator,
    private val appContext: Context? = null,
    externalScope: CoroutineScope? = null
) : ViewModel() {
    private val scope = externalScope ?: viewModelScope

    val engineState: StateFlow<AutoPlayEngineState> = coordinator.engineState
    val config: StateFlow<AutoPlayConfig> = preferencesController.config
    val isMuted: StateFlow<Boolean> = coordinator.isMuted
    val remainingSleepMillis: StateFlow<Long?> = coordinator.remainingSleepMillis

    private val mutableItemCounts = MutableStateFlow<Map<AutoPlaySource, Int>>(emptyMap())
    val itemCounts: StateFlow<Map<AutoPlaySource, Int>> = mutableItemCounts.asStateFlow()

    private val mutableAvailablePackages = MutableStateFlow<List<AutoPlayPackageInfo>>(emptyList())
    val availablePackages: StateFlow<List<AutoPlayPackageInfo>> = mutableAvailablePackages.asStateFlow()

    private val mutablePackageTitle = MutableStateFlow<String?>(null)
    val packageTitle: StateFlow<String?> = mutablePackageTitle.asStateFlow()

    private val mutableIsPackageAvailable = MutableStateFlow(true)
    val isPackageAvailable: StateFlow<Boolean> = mutableIsPackageAvailable.asStateFlow()

    init {
        refreshSourceCounts()
    }

    fun selectPackage(packageId: String) {
        preferencesController.updateSelectedPackageId(packageId)
        refreshSourceCounts()
    }

    fun refreshSourceCounts() {
        scope.launch(Dispatchers.Default) {
            val packages = contentSelector.getAvailablePackages()
            mutableAvailablePackages.value = packages

            val currentConfig = preferencesController.current()
            val targetPkgId = currentConfig.selectedPackageId

            val effectivePkgId = if (targetPkgId != null) {
                val exists = packages.any { it.id == targetPkgId }
                if (exists) {
                    mutableIsPackageAvailable.value = true
                    targetPkgId
                } else {
                    // Package was deleted or unavailable - do not silently substitute
                    mutableIsPackageAvailable.value = false
                    null
                }
            } else {
                // If no package was ever selected, default to the first available package if present
                val defaultPkg = packages.firstOrNull()
                if (defaultPkg != null) {
                    preferencesController.updateSelectedPackageId(defaultPkg.id)
                    mutableIsPackageAvailable.value = true
                    defaultPkg.id
                } else {
                    mutableIsPackageAvailable.value = false
                    null
                }
            }

            if (effectivePkgId != null) {
                mutablePackageTitle.value = contentSelector.getPackageName(effectivePkgId)
                val counts = AutoPlaySource.entries.associateWith { source ->
                    contentSelector.countItemsForSource(source, packageId = effectivePkgId)
                }
                mutableItemCounts.value = counts
            } else {
                mutablePackageTitle.value = null
                val counts = AutoPlaySource.entries.associateWith { 0 }
                mutableItemCounts.value = counts
            }
        }
    }

    fun selectDirection(direction: AutoPlayDirection) {
        preferencesController.updateDirection(direction)
    }

    fun selectSource(source: AutoPlaySource) {
        preferencesController.updateSource(source)
    }

    fun selectPlaybackOrder(playbackOrder: AutoPlayPlaybackOrder) {
        preferencesController.updatePlaybackOrder(playbackOrder)
    }

    fun updateFrontDelayMs(delayMs: Long) {
        preferencesController.updateFrontDelayMs(delayMs)
    }

    fun updateFrontDelaySeconds(seconds: Double) {
        val ms = (seconds * 1000.0).toLong()
        preferencesController.updateFrontDelayMs(ms)
    }

    fun updatePlayFrontAudio(enabled: Boolean) {
        preferencesController.updatePlayFrontAudio(enabled)
    }

    fun updatePlayAnswerAudio(enabled: Boolean) {
        preferencesController.updatePlayAnswerAudio(enabled)
    }

    fun updatePostAnswerDelayMs(delayMs: Long) {
        preferencesController.updatePostAnswerDelayMs(delayMs)
    }

    fun updatePostAnswerDelaySeconds(seconds: Double) {
        val ms = (seconds * 1000.0).toLong()
        preferencesController.updatePostAnswerDelayMs(ms)
    }

    fun updatePlayExampleEnglishAudio(enabled: Boolean) {
        preferencesController.updatePlayExampleEnglishAudio(enabled)
    }

    fun updatePostExampleEnglishDelayMs(delayMs: Long) {
        preferencesController.updatePostExampleEnglishDelayMs(delayMs)
    }

    fun updatePostExampleEnglishDelaySeconds(seconds: Double) {
        val ms = (seconds * 1000.0).toLong()
        preferencesController.updatePostExampleEnglishDelayMs(ms)
    }

    fun updatePlayExampleVietnameseAudio(enabled: Boolean) {
        preferencesController.updatePlayExampleVietnameseAudio(enabled)
    }

    fun updatePostExampleVietnameseDelayMs(delayMs: Long) {
        preferencesController.updatePostExampleVietnameseDelayMs(delayMs)
    }

    fun updatePostExampleVietnameseDelaySeconds(seconds: Double) {
        val ms = (seconds * 1000.0).toLong()
        preferencesController.updatePostExampleVietnameseDelayMs(ms)
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        preferencesController.updateKeepScreenOn(enabled)
    }

    fun updateBackgroundPlayback(enabled: Boolean) {
        preferencesController.updateBackgroundPlayback(enabled)
    }

    fun toggleMute() {
        coordinator.toggleMute()
        preferencesController.updateMuted(coordinator.isMuted.value)
    }

    fun setMuted(muted: Boolean) {
        coordinator.setMuted(muted)
        preferencesController.updateMuted(muted)
    }

    fun updateSleepTimerMinutes(minutes: Double?) {
        preferencesController.updateSleepTimerMinutes(minutes)
        coordinator.setSleepTimerMinutes(minutes)
    }

    fun startAutoPlay() {
        val currentConfig = preferencesController.current()
        val pkgId = currentConfig.selectedPackageId
        if (!mutableIsPackageAvailable.value || pkgId == null) return
        val items = contentSelector.selectItems(currentConfig.source, packageId = pkgId)

        if (appContext != null && items.isNotEmpty()) {
            AutoPlayPlaybackService.startService(appContext)
        }

        coordinator.start(items, currentConfig)
    }

    fun startAutoPlayWithItems(items: List<AutoPlayItem>) {
        if (items.isEmpty()) return
        val currentConfig = preferencesController.current()
        if (appContext != null) {
            AutoPlayPlaybackService.startService(appContext)
        }
        coordinator.start(items, currentConfig)
    }

    fun startAutoPlayForContentIds(contentIds: List<vn.loi.learning.domain.content.model.ContentId>) {
        val items = contentSelector.selectItemsForContentIds(contentIds)
        startAutoPlayWithItems(items)
    }

    fun startAutoPlayForSource(source: AutoPlaySource) {
        preferencesController.updateSource(source)
        val currentConfig = preferencesController.current()
        val pkgId = currentConfig.selectedPackageId
        val items = contentSelector.selectItems(source, packageId = pkgId)
        if (items.isNotEmpty()) {
            if (appContext != null) {
                AutoPlayPlaybackService.startService(appContext)
            }
            coordinator.start(items, currentConfig.copy(source = source))
        }
    }

    fun pause() {
        coordinator.pause()
    }

    fun resume() {
        coordinator.resume()
    }

    fun next() {
        coordinator.next()
    }

    fun previous() {
        coordinator.previous()
    }

    fun replay() {
        val currentConfig = preferencesController.current()
        val pkgId = currentConfig.selectedPackageId
        if (!mutableIsPackageAvailable.value || pkgId == null) return
        val items = contentSelector.selectItems(currentConfig.source, packageId = pkgId)
        if (appContext != null && items.isNotEmpty()) {
            AutoPlayPlaybackService.startService(appContext)
        }
        coordinator.replay()
    }

    fun stop() {
        coordinator.stop()
        if (appContext != null) {
            AutoPlayPlaybackService.stopService(appContext)
        }
        refreshSourceCounts()
    }

    fun onHostActivityStop() {
        val currentConfig = preferencesController.current()
        if (!currentConfig.backgroundPlayback) {
            val state = engineState.value
            if (state is AutoPlayEngineState.Running && !state.isPaused) {
                coordinator.pause()
            }
        }
    }
}
