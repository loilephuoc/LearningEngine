package vn.loi.learning.android.autoplay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioPlaybackEvent
import vn.loi.learning.android.media.AndroidAudioState

class CoroutineAutoPlayTimerScheduler(
    private val scope: CoroutineScope
) : AutoPlayTimerScheduler {
    override fun schedule(delayMillis: Long, onTrigger: () -> Unit): CancellableTimer {
        val job: Job = scope.launch {
            delay(delayMillis)
            onTrigger()
        }
        return object : CancellableTimer {
            override fun cancel() {
                job.cancel()
            }
        }
    }
}

class AndroidAutoPlayAudioPlayer(
    context: Context? = null
) : AutoPlayAudioPlayer, AutoCloseable {
    private val controller = AndroidAudioController(context)

    override fun play(path: String?, onComplete: () -> Unit) {
        if (path.isNullOrBlank()) {
            onComplete()
            return
        }
        var completed = false
        val state = controller.replay(
            path = path,
            isLooping = false,
            onPlaybackEvent = { event ->
                if (event is AndroidAudioPlaybackEvent.Completed && !completed) {
                    completed = true
                    onComplete()
                }
            },
            onState = { audioState ->
                if ((audioState is AndroidAudioState.Failed || audioState is AndroidAudioState.Unavailable) && !completed) {
                    completed = true
                    onComplete()
                }
            }
        )
        if (state is AndroidAudioState.Unavailable || state is AndroidAudioState.Failed) {
            if (!completed) {
                completed = true
                onComplete()
            }
        }
    }

    override fun stop() {
        controller.stop()
    }

    override fun close() {
        controller.close()
    }
}

class AutoPlayViewModel(
    private val contentSelector: AutoPlayContentSelector,
    private val preferencesController: AutoPlayPreferencesController,
    private val audioPlayer: AutoPlayAudioPlayer,
    externalScope: CoroutineScope? = null
) : ViewModel() {
    private val scope = externalScope ?: viewModelScope
    private val timerScheduler = CoroutineAutoPlayTimerScheduler(scope)
    val engine = AutoPlayEngine(audioPlayer, timerScheduler)

    val engineState: StateFlow<AutoPlayEngineState> = engine.state
    val config: StateFlow<AutoPlayConfig> = preferencesController.config

    private val mutableItemCounts = MutableStateFlow<Map<AutoPlaySource, Int>>(emptyMap())
    val itemCounts: StateFlow<Map<AutoPlaySource, Int>> = mutableItemCounts.asStateFlow()

    private val mutablePackageTitle = MutableStateFlow<String?>(null)
    val packageTitle: StateFlow<String?> = mutablePackageTitle.asStateFlow()

    init {
        refreshSourceCounts()
    }

    fun refreshSourceCounts() {
        scope.launch(Dispatchers.Default) {
            mutablePackageTitle.value = contentSelector.getPackageName()
            val counts = AutoPlaySource.entries.associateWith { source ->
                contentSelector.countItemsForSource(source)
            }
            mutableItemCounts.value = counts
        }
    }

    fun selectDirection(direction: AutoPlayDirection) {
        preferencesController.updateDirection(direction)
    }

    fun selectSource(source: AutoPlaySource) {
        preferencesController.updateSource(source)
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

    fun startAutoPlay() {
        val currentConfig = preferencesController.current()
        val items = contentSelector.selectItems(currentConfig.source)
        engine.start(items, currentConfig)
    }

    fun pause() {
        engine.pause()
    }

    fun resume() {
        engine.resume()
    }

    fun next() {
        engine.next()
    }

    fun previous() {
        engine.previous()
    }

    fun replay() {
        engine.replay()
    }

    fun stop() {
        engine.stop()
        refreshSourceCounts()
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
        if (audioPlayer is AutoCloseable) {
            audioPlayer.close()
        }
    }
}
