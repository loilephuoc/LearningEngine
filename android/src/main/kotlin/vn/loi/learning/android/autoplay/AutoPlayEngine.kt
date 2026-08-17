package vn.loi.learning.android.autoplay

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface CancellableTimer {
    fun cancel()
}

interface AutoPlayTimerScheduler {
    fun schedule(delayMillis: Long, onTrigger: () -> Unit): CancellableTimer
}

class CoroutineAutoPlayTimerScheduler(
    private val scope: kotlinx.coroutines.CoroutineScope
) : AutoPlayTimerScheduler {
    override fun schedule(delayMillis: Long, onTrigger: () -> Unit): CancellableTimer {
        val job: kotlinx.coroutines.Job = scope.launch {
            kotlinx.coroutines.delay(delayMillis)
            onTrigger()
        }
        return object : CancellableTimer {
            override fun cancel() {
                job.cancel()
            }
        }
    }
}

interface AutoPlayAudioPlayer {
    fun play(path: String?, onComplete: () -> Unit)
    fun stop()
    fun setMuted(muted: Boolean) {}
    val isMuted: Boolean get() = false
    val exoPlayer: androidx.media3.exoplayer.ExoPlayer? get() = null
}

interface AutoPlayShuffleStrategy {
    fun <T> shuffle(items: List<T>): List<T>
}

class DefaultAutoPlayShuffleStrategy(
    private val random: java.util.Random = java.util.Random()
) : AutoPlayShuffleStrategy {
    override fun <T> shuffle(items: List<T>): List<T> {
        val list = items.toMutableList()
        list.shuffle(random)
        return list
    }
}

class AutoPlayEngine(
    private val audioPlayer: AutoPlayAudioPlayer,
    private val timerScheduler: AutoPlayTimerScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
    private val shuffleStrategy: AutoPlayShuffleStrategy = DefaultAutoPlayShuffleStrategy()
) {
    private val mutableState = MutableStateFlow<AutoPlayEngineState>(AutoPlayEngineState.Idle(AutoPlayConfig()))
    val state: StateFlow<AutoPlayEngineState> = mutableState.asStateFlow()

    private var sourceItems: List<AutoPlayItem> = emptyList()
    private var currentCycleItems: List<AutoPlayItem> = emptyList()
    private var currentConfig: AutoPlayConfig = AutoPlayConfig()
    private var currentIndex: Int = 0
    private var cycleNumber: Long = 1L

    private val visitHistory = mutableListOf<AutoPlayItem>()
    private var historyIndex: Int = -1

    val hasPrevious: Boolean
        get() = historyIndex > 0

    val hasNext: Boolean
        get() = sourceItems.isNotEmpty()

    val currentItem: AutoPlayItem?
        get() = currentCycleItems.getOrNull(currentIndex)

    val currentCycleNumber: Long
        get() = cycleNumber

    fun start(items: List<AutoPlayItem>, config: AutoPlayConfig, startIndex: Int = 0) {
        cancelCurrentOperations()
        currentConfig = config
        sourceItems = items
        cycleNumber = 1L
        visitHistory.clear()
        historyIndex = -1

        if (items.isEmpty()) {
            currentCycleItems = emptyList()
            mutableState.value = AutoPlayEngineState.Empty(config.source, config)
            return
        }

        currentCycleItems = if (config.playbackOrder == AutoPlayPlaybackOrder.SHUFFLED) {
            shuffleStrategy.shuffle(sourceItems)
        } else {
            sourceItems
        }

        currentIndex = startIndex.coerceIn(0, currentCycleItems.size - 1)
        startCycleItem(currentIndex, pushHistory = true)
    }

    private fun startCycleItem(index: Int, pushHistory: Boolean) {
        val token = generationToken.incrementAndGet()
        currentIndex = index
        val item = currentCycleItems[index]

        if (pushHistory) {
            visitHistory.add(item)
            historyIndex = visitHistory.size - 1
        }

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.FRONT_WAIT,
            item = item,
            currentIndex = index,
            totalCount = currentCycleItems.size,
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
            totalCount = currentCycleItems.size,
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
                totalCount = currentCycleItems.size,
                config = currentConfig,
                isPaused = false
            )

            audioPlayer.play(answerAudio) {
                onAnswerAudioComplete(token, item)
            }
        } else {
            proceedToExampleEnglishAudio(token, item)
        }
    }

    private fun onAnswerAudioComplete(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return
        proceedToPostAnswerDelay(token, item)
    }

    private fun proceedToPostAnswerDelay(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.POST_ANSWER_DELAY,
            item = item,
            currentIndex = currentIndex,
            totalCount = currentCycleItems.size,
            config = currentConfig,
            isPaused = false
        )

        scheduleTimer(currentConfig.postAnswerDelayMs, token) {
            proceedToExampleEnglishAudio(token, item)
        }
    }

    private fun proceedToExampleEnglishAudio(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        val exampleEnAudio = item.exampleAudioPath
        if (currentConfig.playExampleEnglishAudio && !exampleEnAudio.isNullOrBlank()) {
            mutableState.value = AutoPlayEngineState.Running(
                stage = AutoPlayStage.EXAMPLE_EN_AUDIO,
                item = item,
                currentIndex = currentIndex,
                totalCount = currentCycleItems.size,
                config = currentConfig,
                isPaused = false
            )

            audioPlayer.play(exampleEnAudio) {
                onExampleEnglishAudioComplete(token, item)
            }
        } else {
            proceedToExampleVietnameseAudio(token, item)
        }
    }

    private fun onExampleEnglishAudioComplete(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return
        proceedToPostExampleEnglishDelay(token, item)
    }

    private fun proceedToPostExampleEnglishDelay(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.POST_EXAMPLE_EN_DELAY,
            item = item,
            currentIndex = currentIndex,
            totalCount = currentCycleItems.size,
            config = currentConfig,
            isPaused = false
        )

        scheduleTimer(currentConfig.postExampleEnglishDelayMs, token) {
            proceedToExampleVietnameseAudio(token, item)
        }
    }

    private fun proceedToExampleVietnameseAudio(token: Long, item: AutoPlayItem) {
        if (!isTokenValid(token)) return

        val exampleViAudio = item.exampleTranslatedAudioPath
        if (currentConfig.playExampleVietnameseAudio && !exampleViAudio.isNullOrBlank()) {
            mutableState.value = AutoPlayEngineState.Running(
                stage = AutoPlayStage.EXAMPLE_VI_AUDIO,
                item = item,
                currentIndex = currentIndex,
                totalCount = currentCycleItems.size,
                config = currentConfig,
                isPaused = false
            )

            audioPlayer.play(exampleViAudio) {
                onExampleVietnameseAudioComplete(token)
            }
        } else {
            advanceToNextItem(token)
        }
    }

    private fun onExampleVietnameseAudioComplete(token: Long) {
        if (!isTokenValid(token)) return
        proceedToPostExampleVietnameseDelay(token)
    }

    private fun proceedToPostExampleVietnameseDelay(token: Long) {
        if (!isTokenValid(token)) return

        val current = mutableState.value
        if (current is AutoPlayEngineState.Running) {
            mutableState.value = current.copy(stage = AutoPlayStage.POST_EXAMPLE_VI_DELAY)
        }

        scheduleTimer(currentConfig.postExampleVietnameseDelayMs, token) {
            advanceToNextItem(token)
        }
    }

    private fun advanceToNextItem(token: Long) {
        if (!isTokenValid(token)) return
        next()
    }

    private val generationToken = AtomicLong(0L)
    private var activeTimer: CancellableTimer? = null

    fun pause() {
        val current = mutableState.value
        if (current !is AutoPlayEngineState.Running || current.isPaused) return

        generationToken.incrementAndGet()
        activeTimer?.cancel()
        activeTimer = null
        audioPlayer.stop()

        mutableState.value = current.copy(
            stage = AutoPlayStage.PAUSED,
            isPaused = true,
            stageBeforePause = current.stage
        )
    }

    fun resume() {
        val current = mutableState.value
        if (current !is AutoPlayEngineState.Running || !current.isPaused) return

        val resumeStage = current.stageBeforePause ?: current.stage
        val item = current.item
        val token = generationToken.incrementAndGet()

        when (resumeStage) {
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
            AutoPlayStage.REVEAL -> {
                mutableState.value = current.copy(stage = AutoPlayStage.REVEAL, isPaused = false, stageBeforePause = null)
                proceedToAnswerAudio(token, item)
            }
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
                startCycleItem(currentIndex, pushHistory = false)
            }
        }
    }

    fun next() {
        if (sourceItems.isEmpty()) return
        cancelCurrentOperations()

        // If user previously pressed Previous and is navigating forward in visit history
        if (historyIndex >= 0 && historyIndex < visitHistory.size - 1) {
            historyIndex++
            val nextHistoryItem = visitHistory[historyIndex]
            val matchedIndex = currentCycleItems.indexOfFirst { it.contentId == nextHistoryItem.contentId }
            if (matchedIndex >= 0) {
                currentIndex = matchedIndex
            }
            startItemDirect(nextHistoryItem, currentIndex)
            return
        }

        // Moving forward in current cycle
        if (currentIndex + 1 < currentCycleItems.size) {
            startCycleItem(currentIndex + 1, pushHistory = true)
        } else {
            // Reached end of current cycle -> Automatically advance to Next Cycle!
            startNextCycle()
        }
    }

    private fun startNextCycle() {
        cycleNumber++
        val previousLast = visitHistory.lastOrNull()

        currentCycleItems = if (currentConfig.playbackOrder == AutoPlayPlaybackOrder.SHUFFLED) {
            generateNextShuffledCycle(sourceItems, previousLast, currentCycleItems, shuffleStrategy)
        } else {
            sourceItems
        }

        startCycleItem(0, pushHistory = true)
    }

    fun previous() {
        if (sourceItems.isEmpty()) return
        cancelCurrentOperations()

        if (historyIndex > 0) {
            historyIndex--
            val prevItem = visitHistory[historyIndex]
            val matchedIndex = currentCycleItems.indexOfFirst { it.contentId == prevItem.contentId }
            if (matchedIndex >= 0) {
                currentIndex = matchedIndex
            }
            startItemDirect(prevItem, currentIndex)
        } else {
            // Already at earliest history item, restart current item
            val currentItem = currentCycleItems.getOrNull(currentIndex) ?: return
            startItemDirect(currentItem, currentIndex)
        }
    }

    private fun startItemDirect(item: AutoPlayItem, index: Int) {
        val token = generationToken.incrementAndGet()
        mutableState.value = AutoPlayEngineState.Running(
            stage = AutoPlayStage.FRONT_WAIT,
            item = item,
            currentIndex = index,
            totalCount = currentCycleItems.size,
            config = currentConfig,
            isPaused = false
        )

        scheduleTimer(currentConfig.frontDelayMs, token) {
            onFrontWaitComplete(token, item)
        }

        if (currentConfig.playFrontAudio) {
            val frontAudioPath = item.frontAudioPath(currentConfig.direction)
            if (!frontAudioPath.isNullOrBlank()) {
                audioPlayer.play(frontAudioPath) {}
            }
        }
    }

    fun replay() {
        if (sourceItems.isEmpty()) return
        cancelCurrentOperations()
        visitHistory.clear()
        historyIndex = -1
        cycleNumber = 1L

        currentCycleItems = if (currentConfig.playbackOrder == AutoPlayPlaybackOrder.SHUFFLED) {
            shuffleStrategy.shuffle(sourceItems)
        } else {
            sourceItems
        }

        startCycleItem(0, pushHistory = true)
    }

    fun stop() {
        cancelCurrentOperations()
        sourceItems = emptyList()
        currentCycleItems = emptyList()
        visitHistory.clear()
        historyIndex = -1
        cycleNumber = 1L
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

    companion object {
        fun generateNextShuffledCycle(
            source: List<AutoPlayItem>,
            previousLast: AutoPlayItem?,
            previousPermutation: List<AutoPlayItem>,
            strategy: AutoPlayShuffleStrategy
        ): List<AutoPlayItem> {
            if (source.size <= 1) return source
            var attempts = 0
            var shuffled = strategy.shuffle(source)
            while (attempts < 10) {
                val startsWithSame = previousLast != null && shuffled.first().contentId == previousLast.contentId
                val isIdentical = source.size > 2 && shuffled.map { it.contentId } == previousPermutation.map { it.contentId }
                if (!startsWithSame && !isIdentical) {
                    return shuffled
                }
                shuffled = strategy.shuffle(source)
                attempts++
            }
            // Deterministic rotation/swap if random repeats
            if (shuffled.size > 1 && previousLast != null && shuffled.first().contentId == previousLast.contentId) {
                val mutable = shuffled.toMutableList()
                val temp = mutable[0]
                mutable[0] = mutable[mutable.size - 1]
                mutable[mutable.size - 1] = temp
                shuffled = mutable
            }
            return shuffled
        }
    }
}
