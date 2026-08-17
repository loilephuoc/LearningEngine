package vn.loi.learning.android.autoplay

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId

class AutoPlayTimingStateMachineTest {

    class FakeTimerScheduler : AutoPlayTimerScheduler {
        val scheduledDelays = mutableListOf<Long>()
        val scheduledCallbacks = mutableListOf<() -> Unit>()

        override fun schedule(delayMillis: Long, onTrigger: () -> Unit): CancellableTimer {
            scheduledDelays.add(delayMillis)
            scheduledCallbacks.add(onTrigger)
            var cancelled = false
            return object : CancellableTimer {
                override fun cancel() {
                    cancelled = true
                    scheduledCallbacks.remove(onTrigger)
                }
            }
        }

        fun fireNext() {
            if (scheduledCallbacks.isNotEmpty()) {
                val cb = scheduledCallbacks.removeAt(0)
                cb()
            }
        }

        fun fireAll() {
            while (scheduledCallbacks.isNotEmpty()) {
                fireNext()
            }
        }
    }

    class FakeAudioPlayer : AutoPlayAudioPlayer {
        val playedPaths = mutableListOf<String?>()
        var currentCallback: (() -> Unit)? = null
        var playCount = 0
        var stopCount = 0
        var activeAudioCount = 0
        var maxConcurrentAudio = 0
        private var _isMuted = false

        override val isMuted: Boolean
            get() = _isMuted

        override fun setMuted(muted: Boolean) {
            _isMuted = muted
        }

        override fun play(path: String?, onComplete: () -> Unit) {
            playCount++
            playedPaths.add(path)
            activeAudioCount++
            if (activeAudioCount > maxConcurrentAudio) {
                maxConcurrentAudio = activeAudioCount
            }
            currentCallback = {
                activeAudioCount--
                onComplete()
            }
        }

        override fun stop() {
            stopCount++
            if (activeAudioCount > 0) {
                activeAudioCount--
            }
            currentCallback = null
        }

        fun completeAudio() {
            val cb = currentCallback
            currentCallback = null
            cb?.invoke()
        }
    }

    private val sampleItem = AutoPlayItem(
        contentId = ContentId("item-1"),
        headword = "apple",
        ipa = "/ˈæp.əl/",
        partOfSpeech = "noun",
        vietnameseMeaning = "quả táo",
        englishExample = "She ate an apple.",
        vietnameseExample = "Cô ấy đã ăn một quả táo.",
        wordAudioPath = "audio/apple.mp3",
        meaningAudioPath = "audio/qua_tao.mp3",
        exampleAudioPath = "audio/apple_ex.mp3",
        exampleTranslatedAudioPath = "audio/qua_tao_ex.mp3",
        imagePath = "images/apple.png"
    )

    @Test
    fun `TC01 - Full pipeline in Vietnamese to English direction with decimal timing`() {
        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            direction = AutoPlayDirection.VIETNAMESE_TO_ENGLISH,
            frontDelayMs = 1500L, // 1.5s
            playFrontAudio = true,
            playAnswerAudio = true,
            postAnswerDelayMs = 750L, // 0.75s
            playExampleEnglishAudio = true,
            postExampleEnglishDelayMs = 1250L, // 1.25s
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 2500L // 2.5s
        )

        engine.start(listOf(sampleItem), config)

        // 1. FRONT_WAIT with 1500ms
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
        assertEquals(listOf<String?>("audio/qua_tao.mp3"), audioPlayer.playedPaths)
        assertEquals(listOf(1500L), scheduler.scheduledDelays)

        // 2. Front delay timer expires -> REVEAL -> ANSWER_AUDIO (English word)
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)
        assertEquals(listOf<String?>("audio/qua_tao.mp3", "audio/apple.mp3"), audioPlayer.playedPaths)

        // 3. Answer audio completes -> POST_ANSWER_DELAY (750ms)
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_ANSWER_DELAY, running.stage)
        assertEquals(listOf(1500L, 750L), scheduler.scheduledDelays)

        // 4. Post-answer delay expires -> EXAMPLE_EN_AUDIO
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_EN_AUDIO, running.stage)
        assertEquals(listOf<String?>("audio/qua_tao.mp3", "audio/apple.mp3", "audio/apple_ex.mp3"), audioPlayer.playedPaths)

        // 5. English example audio completes -> POST_EXAMPLE_EN_DELAY (1250ms)
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_EN_DELAY, running.stage)
        assertEquals(listOf(1500L, 750L, 1250L), scheduler.scheduledDelays)

        // 6. Post-example EN delay expires -> EXAMPLE_VI_AUDIO
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_VI_AUDIO, running.stage)
        assertEquals(listOf<String?>("audio/qua_tao.mp3", "audio/apple.mp3", "audio/apple_ex.mp3", "audio/qua_tao_ex.mp3"), audioPlayer.playedPaths)

        // 7. Vietnamese example audio completes -> POST_EXAMPLE_VI_DELAY (2500ms)
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_VI_DELAY, running.stage)
        assertEquals(listOf(1500L, 750L, 1250L, 2500L), scheduler.scheduledDelays)
        // 8. Post-example VI delay expires -> Continuously advances to Cycle 2
        scheduler.fireNext()
        val runningAfter = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, runningAfter.stage)
        assertEquals(2L, engine.currentCycleNumber)
    }

    @Test
    fun `TC02 - Full pipeline in English to Vietnamese direction`() {
        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            direction = AutoPlayDirection.ENGLISH_TO_VIETNAMESE,
            frontDelayMs = 2000L,
            playFrontAudio = true,
            playAnswerAudio = true,
            postAnswerDelayMs = 1000L,
            playExampleEnglishAudio = true,
            postExampleEnglishDelayMs = 1000L,
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 1000L
        )

        engine.start(listOf(sampleItem), config)

        // 1. FRONT_WAIT with English front audio
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
        assertEquals(listOf<String?>("audio/apple.mp3"), audioPlayer.playedPaths)

        // 2. Front timer expires -> REVEAL -> Vietnamese Answer audio
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)
        assertEquals(listOf<String?>("audio/apple.mp3", "audio/qua_tao.mp3"), audioPlayer.playedPaths)

        // 3. Answer audio completes -> POST_ANSWER_DELAY
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_ANSWER_DELAY, running.stage)

        // 4. Post-answer delay expires -> Example EN Audio
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_EN_AUDIO, running.stage)
        assertEquals(listOf<String?>("audio/apple.mp3", "audio/qua_tao.mp3", "audio/apple_ex.mp3"), audioPlayer.playedPaths)

        // 5. Example EN audio completes -> POST_EXAMPLE_EN_DELAY
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_EN_DELAY, running.stage)

        // 6. Post-example EN delay expires -> Example VI Audio
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_VI_AUDIO, running.stage)
        assertEquals(listOf<String?>("audio/apple.mp3", "audio/qua_tao.mp3", "audio/apple_ex.mp3", "audio/qua_tao_ex.mp3"), audioPlayer.playedPaths)

        // 7. Example VI audio completes -> POST_EXAMPLE_VI_DELAY
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_VI_DELAY, running.stage)

        // 8. Post-example VI delay expires -> Advances to Cycle 2
        scheduler.fireNext()
        val runningCycle2 = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, runningCycle2.stage)
        assertEquals(2L, engine.currentCycleNumber)
    }

    @Test
    fun `TC03 - Optional stages skip safely`() {
        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            direction = AutoPlayDirection.VIETNAMESE_TO_ENGLISH,
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = false,
            postAnswerDelayMs = 0L,
            playExampleEnglishAudio = false,
            postExampleEnglishDelayMs = 0L,
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 1000L
        )

        engine.start(listOf(sampleItem), config)

        // 1. FRONT_WAIT with NO front audio played
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
        assertTrue(audioPlayer.playedPaths.isEmpty())

        // 2. Front timer expires -> Directly jumps to Example VI Audio (since Answer & EN example are OFF)
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_VI_AUDIO, running.stage)
        assertEquals(listOf<String?>("audio/qua_tao_ex.mp3"), audioPlayer.playedPaths)

        // 3. Complete Example VI Audio -> POST_EXAMPLE_VI_DELAY (1000ms)
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_VI_DELAY, running.stage)

        // 4. Timer expires -> Advances to Cycle 2
        scheduler.fireNext()
        val runningCycle2 = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, runningCycle2.stage)
        assertEquals(2L, engine.currentCycleNumber)
    }

    @Test
    fun `TC04 - Missing media skips without stalling and advances to next cycle`() {
        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val itemNoAudio = sampleItem.copy(
            wordAudioPath = null,
            meaningAudioPath = null,
            exampleAudioPath = null,
            exampleTranslatedAudioPath = null
        )

        val config = AutoPlayConfig(
            frontDelayMs = 1000L,
            playFrontAudio = true,
            playAnswerAudio = true,
            postAnswerDelayMs = 500L,
            playExampleEnglishAudio = true,
            postExampleEnglishDelayMs = 500L,
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 500L
        )

        engine.start(listOf(itemNoAudio), config)

        // Front timer fires -> missing audios skip safely without stalling -> continuously enters cycle 2
        scheduler.fireNext()
        val running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
        assertEquals(2L, engine.currentCycleNumber)
        assertTrue(audioPlayer.playedPaths.isEmpty())
    }

    @Test
    fun `TC05 - Single audio owner invariant is maintained`() {
        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            frontDelayMs = 3000L,
            playFrontAudio = true,
            playAnswerAudio = true,
            playExampleEnglishAudio = true,
            playExampleVietnameseAudio = true
        )

        engine.start(listOf(sampleItem), config)

        // Front audio is playing
        assertEquals(1, audioPlayer.activeAudioCount)

        // Front timer expires -> stops front audio before answer audio starts
        scheduler.fireNext()
        assertEquals(1, audioPlayer.activeAudioCount)

        // Answer audio completes
        audioPlayer.completeAudio()
        assertEquals(0, audioPlayer.activeAudioCount)

        // Example EN starts
        scheduler.fireNext()
        assertEquals(1, audioPlayer.activeAudioCount)

        // Example EN completes
        audioPlayer.completeAudio()
        assertEquals(0, audioPlayer.activeAudioCount)

        // Stop cancels everything
        engine.stop()
        assertEquals(0, audioPlayer.activeAudioCount)
        assertEquals(1, audioPlayer.maxConcurrentAudio)
    }

    @Test
    fun `continuous shuffle cycles play every item once per cycle without duplicate boundary items`() {
        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()

        val itemA = sampleItem.copy(contentId = ContentId("item_a"), headword = "apple")
        val itemB = sampleItem.copy(contentId = ContentId("item_b"), headword = "banana")
        val itemC = sampleItem.copy(contentId = ContentId("item_c"), headword = "cherry")
        val itemD = sampleItem.copy(contentId = ContentId("item_d"), headword = "date")
        val itemE = sampleItem.copy(contentId = ContentId("item_e"), headword = "elderberry")
        val items = listOf(itemA, itemB, itemC, itemD, itemE)

        // Deterministic rotation shuffle for test
        var shuffleCount = 0
        val testShuffleStrategy = object : AutoPlayShuffleStrategy {
            override fun <T> shuffle(items: List<T>): List<T> {
                shuffleCount++
                val rot = shuffleCount % items.size
                return items.drop(rot) + items.take(rot)
            }
        }

        val engine = AutoPlayEngine(audioPlayer, scheduler, shuffleStrategy = testShuffleStrategy)
        val config = AutoPlayConfig(
            playbackOrder = AutoPlayPlaybackOrder.SHUFFLED,
            frontDelayMs = 100L,
            playFrontAudio = false,
            playAnswerAudio = false,
            playExampleEnglishAudio = false,
            playExampleVietnameseAudio = false
        )

        engine.start(items, config)

        // Cycle 1: visit all 5 items
        val cycle1Visits = mutableListOf<String>()
        for (i in 0 until 5) {
            val state = engine.state.value as AutoPlayEngineState.Running
            cycle1Visits.add(state.item.headword)
            assertEquals(1L, engine.currentCycleNumber)
            scheduler.fireNext() // fires front timer, then finishes stage
        }

        // Must have all 5 items with no duplicates
        assertEquals(5, cycle1Visits.size)
        assertEquals(items.map { it.headword }.toSet(), cycle1Visits.toSet())

        // Now in Cycle 2 automatically!
        val cycle2FirstState = engine.state.value as AutoPlayEngineState.Running
        assertEquals(2L, engine.currentCycleNumber)
        // Boundary check: first of cycle 2 must not equal last of cycle 1
        val cycle1Last = cycle1Visits.last()
        kotlin.test.assertNotEquals(cycle1Last, cycle2FirstState.item.headword)

        // Previous from first item of cycle 2 must return to last item of cycle 1
        engine.previous()
        val prevRunning = engine.state.value as AutoPlayEngineState.Running
        assertEquals(cycle1Last, prevRunning.item.headword)

        // Next moves forward again into cycle 2
        engine.next()
        val nextRunning = engine.state.value as AutoPlayEngineState.Running
        assertEquals(cycle2FirstState.item.headword, nextRunning.item.headword)

        engine.stop()
    }

    @Test
    fun `single item source cycles continuously without infinite loop`() {
        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            playbackOrder = AutoPlayPlaybackOrder.SHUFFLED,
            playFrontAudio = false,
            playAnswerAudio = false,
            playExampleEnglishAudio = false,
            playExampleVietnameseAudio = false
        )
        engine.start(listOf(sampleItem), config)

        assertEquals(1L, engine.currentCycleNumber)
        scheduler.fireNext()
        assertEquals(2L, engine.currentCycleNumber)
        scheduler.fireNext()
        assertEquals(3L, engine.currentCycleNumber)

        engine.stop()
    }

    @Test
    fun `two item source avoids immediate boundary repeat`() {
        val itemA = sampleItem.copy(contentId = ContentId("item_a"), headword = "alpha")
        val itemB = sampleItem.copy(contentId = ContentId("item_b"), headword = "beta")
        val items = listOf(itemA, itemB)

        val audioPlayer = FakeAudioPlayer()
        val scheduler = FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            playbackOrder = AutoPlayPlaybackOrder.SHUFFLED,
            frontDelayMs = 100L,
            playFrontAudio = false,
            playAnswerAudio = false,
            playExampleEnglishAudio = false,
            playExampleVietnameseAudio = false
        )

        engine.start(items, config)

        // Item 1
        var state = engine.state.value as AutoPlayEngineState.Running
        val item1 = state.item.headword
        scheduler.fireNext()

        // Item 2 (end of Cycle 1)
        state = engine.state.value as AutoPlayEngineState.Running
        val item2 = state.item.headword
        kotlin.test.assertNotEquals(item1, item2)
        scheduler.fireNext()

        // Item 1 of Cycle 2
        state = engine.state.value as AutoPlayEngineState.Running
        assertEquals(2L, engine.currentCycleNumber)
        // Must avoid repeating item2 immediately across cycle boundary
        kotlin.test.assertNotEquals(item2, state.item.headword)
        assertEquals(item1, state.item.headword)

        engine.stop()
    }
}
