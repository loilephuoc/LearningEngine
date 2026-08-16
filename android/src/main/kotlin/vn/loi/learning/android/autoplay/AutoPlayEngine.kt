package vn.loi.learning.android.autoplay

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CancellableTimer {
    fun cancel()
}

interface AutoPlayTimerScheduler {
    fun schedule(delayMillis: Long, onTrigger: () -> Unit): CancellableTimer
}

interface AutoPlayAudioPlayer {
    fun play(path: String?, onComplete: () -> Unit)
    fun stop()
}

class AutoPlayEngine(
    private val audioPlayer: AutoPlayAudioPlayer,
    private val timerScheduler: AutoPlayTimerScheduler,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutableState = MutableStateFlow<AutoPlayEngineState>(AutoPlayEngineState.Idle(AutoPlayConfig()))
    val state: StateFlow<AutoPlayEngineState> = mutableState.asStateFlow()

    private var activeItems: List<AutoPlayItem> = emptyList()
    private var currentConfig: AutoPlayConfig = AutoPlayConfig()
    private var currentIndex: Int = 0

    private val generationToken = AtomicLong(0L)
    private var activeTimer: CancellableTimer? = null

    val currentItem: AutoPlayItem?
        get() = activeItems.getOrNull(currentIndex)

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasNext: Boolean
        get() = currentIndex + 1 < activeItems.size

    fun start(items: List<AutoPlayItem>, config: AutoPlayConfig, startIndex: Int = 0) {
        cancelCurrentOperations()
        currentConfig = config
        activeItems = items

        if (items.isEmpty()) {
            mutableState.value = AutoPlayEngineState.Empty(config.source, config)
            return
        }

        currentIndex = startIndex.coerceIn(0, items.size - 1)
        startItem(currentIndex)
    }

    private fun startItem(index: Int) {
        val token = generationToken.incrementAndGet()
        val item = activeItems[index]

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.FRONT_WAIT,
            item = item,
            currentIndex = index,
            totalCount = activeItems.size,
            config = currentConfig,
            isPaused = false
        )

        // Front display timer is the primary authority
        scheduleTimer(currentConfig.frontDelayMs, token) {
            onFrontWaitComplete(token, item)
        }

        // Optional front audio during front display
        if (currentConfig.playFrontAudio) {
            val frontAudioPath = item.frontAudioPath(currentConfig.direction)
            if (!frontAudioPath.isNullOrBlank()) {
                audioPlayer.play(frontAudioPath) {
                    // Front audio completion simply finishes; front display duration remains in control
                }
            }
        }
    }

    private fun onFrontWaitComplete(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        // Invalidate/stop any active front audio before moving to reveal
        audioPlayer.stop()

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.REVEAL,
            item = item,
            currentIndex = currentIndex,
            totalCount = activeItems.size,
            config = currentConfig,
            isPaused = false
        )

        proceedToAnswerAudio(token, item)
    }

    private fun proceedToAnswerAudio(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        val answerAudio = item.answerAudioPath(currentConfig.direction)
        if (currentConfig.playAnswerAudio && !answerAudio.isNullOrBlank()) {
            mutableState.value = AutoPlayEngineState.Running(
                stage = AutoPlayStage.ANSWER_AUDIO,
                item = item,
                currentIndex = currentIndex,
                totalCount = activeItems.size,
                config = currentConfig,
                isPaused = false
            )

            audioPlayer.play(answerAudio) {
                if (isTokenValid(token)) {
                    enterPostAnswerDelay(token, item)
                }
            }
        } else {
            if (!currentConfig.playExampleEnglishAudio && !currentConfig.playExampleVietnameseAudio) {
                // If all revealed audio is disabled or unavailable, dwell on reveal
                enterPostAnswerDelay(token, item)
            } else {
                proceedToExampleEnglishAudio(token, item)
            }
        }
    }

    private fun enterPostAnswerDelay(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.POST_ANSWER_DELAY,
            item = item,
            currentIndex = currentIndex,
            totalCount = activeItems.size,
            config = currentConfig,
            isPaused = false
        )

        scheduleTimer(currentConfig.postAnswerDelayMs, token) {
            proceedToExampleEnglishAudio(token, item)
        }
    }

    private fun proceedToExampleEnglishAudio(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        if (currentConfig.playExampleEnglishAudio && !item.exampleAudioPath.isNullOrBlank()) {
            mutableState.value = AutoPlayEngineState.Running(
                stage = AutoPlayStage.EXAMPLE_EN_AUDIO,
                item = item,
                currentIndex = currentIndex,
                totalCount = activeItems.size,
                config = currentConfig,
                isPaused = false
            )

            audioPlayer.play(item.exampleAudioPath) {
                if (isTokenValid(token)) {
                    enterPostExampleEnglishDelay(token, item)
                }
            }
        } else {
            proceedToExampleVietnameseAudio(token, item)
        }
    }

    private fun enterPostExampleEnglishDelay(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.POST_EXAMPLE_EN_DELAY,
            item = item,
            currentIndex = currentIndex,
            totalCount = activeItems.size,
            config = currentConfig,
            isPaused = false
        )

        scheduleTimer(currentConfig.postExampleEnglishDelayMs, token) {
            proceedToExampleVietnameseAudio(token, item)
        }
    }

    private fun proceedToExampleVietnameseAudio(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        if (currentConfig.playExampleVietnameseAudio && !item.exampleTranslatedAudioPath.isNullOrBlank()) {
            mutableState.value = AutoPlayEngineState.Running(
                stage = AutoPlayStage.EXAMPLE_VI_AUDIO,
                item = item,
                currentIndex = currentIndex,
                totalCount = activeItems.size,
                config = currentConfig,
                isPaused = false
            )

            audioPlayer.play(item.exampleTranslatedAudioPath) {
                if (isTokenValid(token)) {
                    enterPostExampleVietnameseDelay(token, item)
                }
            }
        } else {
            advanceToNextItem(token)
        }
    }

    private fun enterPostExampleVietnameseDelay(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.POST_EXAMPLE_VI_DELAY,
            item = item,
            currentIndex = currentIndex,
            totalCount = activeItems.size,
            config = currentConfig,
            isPaused = false
        )

        scheduleTimer(currentConfig.postExampleVietnameseDelayMs, token) {
            advanceToNextItem(token)
        }
    }

    private fun advanceToNextItem(token: Long) {
        if (!isTokenValid(token)) return

        if (currentIndex + 1 < activeItems.size) {
            currentIndex++
            startItem(currentIndex)
        } else {
            completeRun()
        }
    }

    private fun completeRun() {
        cancelCurrentOperations()
        mutableState.value = AutoPlayEngineState.Completed(
            totalCount = activeItems.size,
            config = currentConfig
        )
    }

    fun pause() {
        val current = mutableState.value as? AutoPlayEngineState.Running ?: return
        if (current.isPaused) return

        val stageBeforePause = current.stage
        cancelCurrentOperations()

        mutableState.value = current.copy(
            stage = AutoPlayStage.PAUSED,
            isPaused = true,
            stageBeforePause = stageBeforePause
        )
    }

    fun resume() {
        val current = mutableState.value as? AutoPlayEngineState.Running ?: return
        if (!current.isPaused) return

        val targetStage = current.stageBeforePause ?: AutoPlayStage.FRONT_WAIT
        val token = generationToken.incrementAndGet()
        val item = current.item

        when (targetStage) {
            AutoPlayStage.FRONT_WAIT -> {
                mutableState.value = current.copy(stage = AutoPlayStage.FRONT_WAIT, isPaused = false, stageBeforePause = null)
                scheduleTimer(currentConfig.frontDelayMs, token) {
                    onFrontWaitComplete(token, item)
                }
                if (currentConfig.playFrontAudio) {
                    val frontAudio = item.frontAudioPath(currentConfig.direction)
                    if (!frontAudio.isNullOrBlank()) {
                        audioPlayer.play(frontAudio) {}
                    }
                }
            }
            AutoPlayStage.REVEAL,
            AutoPlayStage.ANSWER_AUDIO -> {
                proceedToAnswerAudio(token, item)
            }
            AutoPlayStage.POST_ANSWER_DELAY -> {
                mutableState.value = current.copy(stage = AutoPlayStage.POST_ANSWER_DELAY, isPaused = false, stageBeforePause = null)
                scheduleTimer(currentConfig.postAnswerDelayMs, token) {
                    proceedToExampleEnglishAudio(token, item)
                }
            }
            AutoPlayStage.EXAMPLE_EN_AUDIO -> {
                proceedToExampleEnglishAudio(token, item)
            }
            AutoPlayStage.POST_EXAMPLE_EN_DELAY -> {
                mutableState.value = current.copy(stage = AutoPlayStage.POST_EXAMPLE_EN_DELAY, isPaused = false, stageBeforePause = null)
                scheduleTimer(currentConfig.postExampleEnglishDelayMs, token) {
                    proceedToExampleVietnameseAudio(token, item)
                }
            }
            AutoPlayStage.EXAMPLE_VI_AUDIO -> {
                proceedToExampleVietnameseAudio(token, item)
            }
            AutoPlayStage.POST_EXAMPLE_VI_DELAY -> {
                mutableState.value = current.copy(stage = AutoPlayStage.POST_EXAMPLE_VI_DELAY, isPaused = false, stageBeforePause = null)
                scheduleTimer(currentConfig.postExampleVietnameseDelayMs, token) {
                    advanceToNextItem(token)
                }
            }
            AutoPlayStage.ADVANCE -> {
                advanceToNextItem(token)
            }
            AutoPlayStage.IDLE,
            AutoPlayStage.PAUSED,
            AutoPlayStage.COMPLETED,
            AutoPlayStage.STOPPED -> {
                startItem(currentIndex)
            }
        }
    }

    fun next() {
        if (activeItems.isEmpty()) return
        cancelCurrentOperations()

        if (currentIndex + 1 < activeItems.size) {
            currentIndex++
            startItem(currentIndex)
        } else {
            completeRun()
        }
    }

    fun previous() {
        if (activeItems.isEmpty()) return
        cancelCurrentOperations()

        if (currentIndex > 0) {
            currentIndex--
        }
        startItem(currentIndex)
    }

    fun replay() {
        if (activeItems.isEmpty()) return
        cancelCurrentOperations()
        currentIndex = 0
        startItem(0)
    }

    fun stop() {
        cancelCurrentOperations()
        activeItems = emptyList()
        mutableState.value = AutoPlayEngineState.Idle(currentConfig)
    }

    private fun cancelCurrentOperations() {
        generationToken.incrementAndGet()
        activeTimer?.cancel()
        activeTimer = null
        audioPlayer.stop()
    }

    private fun scheduleTimer(delayMillis: Long, token: Long, onTrigger: () -> Unit) {
        activeTimer?.cancel()
        if (delayMillis <= 0L) {
            if (isTokenValid(token)) {
                onTrigger()
            }
            return
        }
        activeTimer = timerScheduler.schedule(delayMillis) {
            if (isTokenValid(token)) {
                onTrigger()
            }
        }
    }

    private fun isTokenValid(token: Long): Boolean =
        token == generationToken.get()
}
