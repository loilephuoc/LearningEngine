package vn.loi.learning.android.autoplay

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import vn.loi.learning.android.R
import vn.loi.learning.domain.content.model.ContentId

@OptIn(ExperimentalCoroutinesApi::class)
class AutoPlayServiceIntegrationTest {

    private val item1 = AutoPlayItem(
        contentId = ContentId("item-1"),
        headword = "storm",
        partOfSpeech = "noun",
        vietnameseMeaning = "cơn bão",
        englishExample = "A storm is coming.",
        vietnameseExample = "Một cơn bão đang đến.",
        wordAudioPath = "audio/storm.mp3",
        meaningAudioPath = "audio/con_bao.mp3",
        exampleAudioPath = "audio/storm_ex.mp3",
        exampleTranslatedAudioPath = "audio/con_bao_ex.mp3",
        imagePath = "images/storm.png"
    )

    private val item2 = AutoPlayItem(
        contentId = ContentId("item-2"),
        headword = "sunshine",
        partOfSpeech = "noun",
        vietnameseMeaning = "ánh nắng",
        englishExample = "The sunshine is warm.",
        vietnameseExample = "Ánh nắng thật ấm áp.",
        wordAudioPath = "audio/sunshine.mp3",
        meaningAudioPath = "audio/anh_nang.mp3",
        exampleAudioPath = "audio/sunshine_ex.mp3",
        exampleTranslatedAudioPath = "audio/anh_nang_ex.mp3",
        imagePath = "images/sunshine.png"
    )

    @Test
    fun `single coordinator authority across multiple ViewModels and reconnect`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val config = AutoPlayConfig(
            direction = AutoPlayDirection.VIETNAMESE_TO_ENGLISH,
            playbackOrder = AutoPlayPlaybackOrder.SOURCE_ORDER,
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = true,
            postAnswerDelayMs = 500L
        )

        // 1. Start from initial ViewModel
        coordinator.start(listOf(item1, item2), config)

        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("storm", running.item.headword)
        assertEquals(0, running.currentIndex)

        // Advance to item 2
        coordinator.next()
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("sunshine", running.item.headword)
        assertEquals(1, running.currentIndex)

        // 2. Simulate ViewModel reconnection (new ViewModel observing existing coordinator)
        val reconnectedState = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("sunshine", reconnectedState.item.headword)
        assertEquals(1, reconnectedState.currentIndex)
        assertEquals(2, reconnectedState.totalCount)

        coordinator.release()
    }

    @Test
    fun `media controls map to vocabulary-level navigation without rating or duplicate commands`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val config = AutoPlayConfig(
            playbackOrder = AutoPlayPlaybackOrder.SOURCE_ORDER,
            frontDelayMs = 2000L,
            playFrontAudio = false,
            playAnswerAudio = true
        )

        coordinator.start(listOf(item1, item2), config)

        // Media Next -> item 2
        coordinator.next()
        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("sunshine", running.item.headword)

        // Media Previous -> item 1
        coordinator.previous()
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("storm", running.item.headword)

        // Media Pause
        coordinator.pause()
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertTrue(running.isPaused)

        // Media Resume
        coordinator.resume()
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertFalse(running.isPaused)

        // Media Stop
        coordinator.stop()
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Idle)

        coordinator.release()
    }

    @Test
    fun `dynamic metadata maintains stable headword while subtitle updates across stages`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val config = AutoPlayConfig(
            playbackOrder = AutoPlayPlaybackOrder.SOURCE_ORDER,
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = true,
            postAnswerDelayMs = 500L,
            playExampleEnglishAudio = true,
            postExampleEnglishDelayMs = 500L,
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 500L
        )

        coordinator.start(listOf(item1, item2), config)

        // Stage 1: FRONT_WAIT -> Subtitle is vietnameseMeaning
        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
        assertEquals("storm", running.item.headword)
        assertEquals("cơn bão", running.item.vietnameseMeaning)

        // Advance front wait
        testScheduler.advanceTimeBy(1001L)
        testScheduler.runCurrent()

        // Stage 2: ANSWER_AUDIO -> Subtitle is vietnameseMeaning
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)
        assertEquals("storm", running.item.headword)
        assertEquals("cơn bão", running.item.vietnameseMeaning)

        // Complete Answer Audio
        audioPlayer.completeAudio()
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(501L) // post answer delay
        testScheduler.runCurrent()

        // Stage 3: EXAMPLE_EN_AUDIO -> Subtitle is englishExample
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_EN_AUDIO, running.stage)
        assertEquals("storm", running.item.headword)
        assertEquals("A storm is coming.", running.item.englishExample)

        // Complete Example EN Audio
        audioPlayer.completeAudio()
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(501L) // post example EN delay
        testScheduler.runCurrent()

        // Stage 4: EXAMPLE_VI_AUDIO -> Subtitle is vietnameseExample
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_VI_AUDIO, running.stage)
        assertEquals("storm", running.item.headword)
        assertEquals("Một cơn bão đang đến.", running.item.vietnameseExample)

        // Complete Example VI Audio
        audioPlayer.completeAudio()
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(501L) // post example VI delay
        testScheduler.runCurrent()

        // Stage 5: NEXT VOCABULARY -> Sunshine
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
        assertEquals("sunshine", running.item.headword)
        assertEquals("ánh nắng", running.item.vietnameseMeaning)

        coordinator.release()
    }

    @Test
    fun `mute toggling mutes audio player and survives item transitions without pausing engine`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val config = AutoPlayConfig(
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = true
        )

        coordinator.start(listOf(item1, item2), config)
        assertFalse(coordinator.isMuted.value)
        assertFalse(audioPlayer.isMuted)

        // Mute Auto Play
        coordinator.toggleMute()
        assertTrue(coordinator.isMuted.value)
        assertTrue(audioPlayer.isMuted)

        // Advance to next item -> should remain muted
        coordinator.next()
        val running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(1, running.currentIndex)
        assertTrue(coordinator.isMuted.value)
        assertTrue(audioPlayer.isMuted)

        // Unmute Auto Play
        coordinator.setMuted(false)
        assertFalse(coordinator.isMuted.value)
        assertFalse(audioPlayer.isMuted)

        coordinator.release()
    }

    @Test
    fun `sleep timer monotonic countdown triggers stop when deadline arrives`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var currentTimeMs = 10_000L
        val fakeClock = { currentTimeMs }

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope, clock = fakeClock)

        val config = AutoPlayConfig(
            frontDelayMs = 5000L,
            playFrontAudio = false,
            playAnswerAudio = false,
            sleepTimerMinutes = 0.5 // 30 seconds
        )

        coordinator.start(listOf(item1, item2), config)
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Running)
        assertEquals(30_000L, coordinator.remainingSleepMillis.value)

        // Advance 10s
        currentTimeMs += 10_000L
        testScheduler.advanceTimeBy(10_000L)
        testScheduler.runCurrent()
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Running)

        // Advance another 20s (reaching 30s deadline)
        currentTimeMs += 20_000L
        testScheduler.advanceTimeBy(20_000L)
        testScheduler.runCurrent()

        // Session must be completely stopped
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Idle)
        assertNull(coordinator.remainingSleepMillis.value)

        coordinator.release()
    }

    @Test
    fun `sleep timer continues counting down while paused and stops session`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var currentTimeMs = 0L
        val fakeClock = { currentTimeMs }

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope, clock = fakeClock)

        coordinator.start(listOf(item1), AutoPlayConfig(sleepTimerMinutes = 1.0)) // 60s
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Running)

        // After 10s, Pause
        currentTimeMs += 10_000L
        testScheduler.advanceTimeBy(10_000L)
        testScheduler.runCurrent()
        coordinator.pause()
        assertTrue((coordinator.engineState.value as AutoPlayEngineState.Running).isPaused)

        // While paused, advance another 50s (total 60s)
        currentTimeMs += 50_000L
        testScheduler.advanceTimeBy(50_000L)
        testScheduler.runCurrent()

        // Session must be stopped even though it was paused
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Idle)

        coordinator.release()
    }

    @Test
    fun `sleep timer replacement updates deadline from now`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var currentTimeMs = 0L
        val fakeClock = { currentTimeMs }

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope, clock = fakeClock)

        coordinator.start(listOf(item1), AutoPlayConfig(sleepTimerMinutes = 1.0)) // 60s

        // Advance 20s
        currentTimeMs += 20_000L
        testScheduler.advanceTimeBy(20_000L)
        testScheduler.runCurrent()

        // Replace sleep timer with 10s (new duration)
        coordinator.setSleepTimerDurationMs(10_000L)
        testScheduler.runCurrent()

        // Advance 10s from replacement (total 30s from start)
        currentTimeMs += 10_000L
        testScheduler.advanceTimeBy(10_000L)
        testScheduler.runCurrent()

        // Stops at 30s instead of original 60s
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Idle)

        coordinator.release()
    }

    @Test
    fun `background playback toggle policy pauses on host stop when disabled`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val store = AutoPlayPreferencesTest.FakePreferenceStore()
        val prefController = AutoPlayPreferencesController(store)

        // 1. Background playback enabled (default)
        val appContext = vn.loi.learning.infrastructure.LearningApplicationFactory.createInMemory()
        prefController.updateBackgroundPlayback(true)
        val viewModel1 = AutoPlayViewModel(
            contentSelector = AutoPlayContentSelector(context = appContext),
            preferencesController = prefController,
            coordinator = coordinator
        )

        coordinator.start(listOf(item1), prefController.current())
        viewModel1.onHostActivityStop()
        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertFalse(running.isPaused, "Should NOT pause when background playback is enabled")

        // 2. Background playback disabled
        prefController.updateBackgroundPlayback(false)
        val viewModel2 = AutoPlayViewModel(
            contentSelector = AutoPlayContentSelector(context = appContext),
            preferencesController = prefController,
            coordinator = coordinator
        )

        coordinator.start(listOf(item1), prefController.current())
        viewModel2.onHostActivityStop()
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertTrue(running.isPaused, "Should pause when background playback is disabled and host stops")

        coordinator.release()
    }

    @Test
    fun `regression test - FRONT_WAIT metadata update does not set metadata-only item on player and real audio receives valid URI`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val config = AutoPlayConfig(
            frontDelayMs = 1500L,
            playFrontAudio = false,
            playAnswerAudio = true
        )

        // 1. Start AutoPlay in FRONT_WAIT (timer-only, no front audio clip)
        coordinator.start(listOf(item1), config)

        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
        // Assert no audio played during timer-only FRONT_WAIT with playFrontAudio = false
        assertTrue(audioPlayer.playedPaths.isEmpty(), "No fake or metadata-only audio should be played on ExoPlayer")

        // 2. Advance to ANSWER_AUDIO (real audio stage)
        coordinator.engine.next() // Advance
        // Start playing item with real audio
        coordinator.start(listOf(item1), config.copy(playFrontAudio = true))
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(listOf<String?>("audio/con_bao.mp3"), audioPlayer.playedPaths)

        // Complete audio safely
        audioPlayer.completeAudio()

        coordinator.release()
    }

    @Test
    fun `AutoPlayForwardingPlayer truthfully exposes vocabulary Next and Previous availability`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val item3 = item2.copy(contentId = ContentId("item-3"), headword = "cloud")
        val items = listOf(item1, item2, item3)

        val config = AutoPlayConfig(
            playbackOrder = AutoPlayPlaybackOrder.SOURCE_ORDER,
            frontDelayMs = 2000L,
            playFrontAudio = false,
            playAnswerAudio = true
        )

        // Base dummy player for ForwardingPlayer
        val dummyPlayer = java.lang.reflect.Proxy.newProxyInstance(
            androidx.media3.common.Player::class.java.classLoader,
            arrayOf(androidx.media3.common.Player::class.java)
        ) { _, method, _ ->
            when {
                method.name == "getAvailableCommands" -> androidx.media3.common.Player.Commands.EMPTY
                method.returnType == java.lang.Boolean.TYPE -> false
                method.returnType == java.lang.Integer.TYPE -> 0
                method.returnType == java.lang.Long.TYPE -> 0L
                method.returnType == java.lang.Float.TYPE -> 1f
                method.name == "getApplicationLooper" -> android.os.Looper.getMainLooper()
                method.name == "toString" -> "DummyPlayer"
                method.name == "hashCode" -> 1
                else -> null
            }
        } as androidx.media3.common.Player

        val forwardingPlayer = AutoPlayForwardingPlayer(dummyPlayer, coordinator)

        // 1. At start (item 1 of 3 - index 0)
        coordinator.start(items, config)
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_PLAY_PAUSE))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_STOP))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertFalse(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS))
        assertFalse(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))

        // 2. Middle item (item 2 of 3 - index 1)
        forwardingPlayer.seekToNextMediaItem()
        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("sunshine", running.item.headword)
        assertEquals(1, running.currentIndex)

        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))

        // 3. Final item (item 3 of 3 - index 2)
        forwardingPlayer.seekToNextMediaItem()
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("cloud", running.item.headword)
        assertEquals(2, running.currentIndex)

        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        // In continuous cycles, Next is always available to advance to next cycle!
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(forwardingPlayer.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))

        // 4. Seek Previous back to middle item
        forwardingPlayer.seekToPreviousMediaItem()
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("sunshine", running.item.headword)
        assertEquals(1, running.currentIndex)

        coordinator.release()
    }

    @Test
    fun `seeking next during stage execution advances to next vocabulary and resets stage pipeline`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        val config = AutoPlayConfig(
            playbackOrder = AutoPlayPlaybackOrder.SOURCE_ORDER,
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = true,
            postAnswerDelayMs = 500L,
            playExampleEnglishAudio = true,
            postExampleEnglishDelayMs = 500L
        )

        val dummyPlayer = java.lang.reflect.Proxy.newProxyInstance(
            androidx.media3.common.Player::class.java.classLoader,
            arrayOf(androidx.media3.common.Player::class.java)
        ) { _, method, _ ->
            when {
                method.name == "getAvailableCommands" -> androidx.media3.common.Player.Commands.EMPTY
                method.returnType == java.lang.Boolean.TYPE -> false
                method.returnType == java.lang.Integer.TYPE -> 0
                method.returnType == java.lang.Long.TYPE -> 0L
                method.returnType == java.lang.Float.TYPE -> 1f
                method.name == "getApplicationLooper" -> android.os.Looper.getMainLooper()
                method.name == "toString" -> "DummyPlayer"
                method.name == "hashCode" -> 1
                else -> null
            }
        } as androidx.media3.common.Player
        val forwardingPlayer = AutoPlayForwardingPlayer(dummyPlayer, coordinator)

        coordinator.start(listOf(item1, item2), config)

        // Advance to ANSWER_AUDIO
        testScheduler.advanceTimeBy(1001L)
        testScheduler.runCurrent()
        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)
        assertEquals("storm", running.item.headword)

        // Trigger system Next while in middle of audio stage
        forwardingPlayer.seekToNextMediaItem()

        // Must transition immediately to next vocabulary item (sunshine) at FRONT_WAIT
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertEquals("sunshine", running.item.headword)
        assertEquals(1, running.currentIndex)
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)

        coordinator.release()
    }

    @Test
    fun `custom commands for mute and stop operate without pausing engine or corrupting state`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(audioPlayer, testScope)

        coordinator.start(listOf(item1, item2), AutoPlayConfig())
        var running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertFalse(running.isPaused)
        assertFalse(coordinator.isMuted.value)

        // 1. Toggle Mute: state remains running and unpaused
        coordinator.toggleMute()
        assertTrue(coordinator.isMuted.value)
        running = coordinator.engineState.value as AutoPlayEngineState.Running
        assertFalse(running.isPaused, "Mute must not pause the engine")

        // 2. Stop command: stops session
        coordinator.stop()
        assertTrue(coordinator.engineState.value is AutoPlayEngineState.Idle)

        coordinator.release()
    }

    @Test
    fun `mute and sleep timer survive continuous cycle boundaries without resetting deadline`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var fakeTime = 100_000L
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(
            audioPlayer = audioPlayer,
            scope = testScope,
            clock = { fakeTime }
        )

        val config = AutoPlayConfig(
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = false,
            playExampleEnglishAudio = false,
            playExampleVietnameseAudio = false,
            sleepTimerMinutes = 10.0 // 600_000 ms
        )

        coordinator.start(listOf(item1, item2), config)
        coordinator.toggleMute()
        assertTrue(coordinator.isMuted.value)
        val initialDeadline = coordinator.sleepDeadlineElapsed.value
        assertEquals(700_000L, initialDeadline)

        // Advance past item 1
        testScheduler.advanceTimeBy(1001L)
        fakeTime += 1001L
        testScheduler.runCurrent()

        // Advance past item 2 (End of Cycle 1) -> enters Cycle 2
        testScheduler.advanceTimeBy(1001L)
        fakeTime += 1001L
        testScheduler.runCurrent()

        assertEquals(2L, coordinator.engine.currentCycleNumber)
        // Mute state preserved
        assertTrue(coordinator.isMuted.value)
        // Sleep timer deadline unchanged
        assertEquals(initialDeadline, coordinator.sleepDeadlineElapsed.value)

        coordinator.release()
    }

    @Test
    fun `MediaButtonPreferences contains exactly TWO custom preference buttons for Mute and Stop`() {
        val customCommandToggleMute = androidx.media3.session.SessionCommand(
            AutoPlayPlaybackService.COMMAND_AUTO_PLAY_TOGGLE_MUTE,
            android.os.Bundle()
        )
        val customCommandStop = androidx.media3.session.SessionCommand(
            AutoPlayPlaybackService.COMMAND_AUTO_PLAY_STOP,
            android.os.Bundle()
        )

        val muteBtn = androidx.media3.session.CommandButton.Builder(R.drawable.ic_autoplay_mute)
            .setDisplayName("Mute")
            .setSessionCommand(customCommandToggleMute)
            .setSlots(androidx.media3.session.CommandButton.SLOT_OVERFLOW)
            .setEnabled(true)
            .build()

        val stopBtn = androidx.media3.session.CommandButton.Builder(R.drawable.ic_autoplay_stop)
            .setDisplayName("Stop")
            .setSessionCommand(customCommandStop)
            .setSlots(androidx.media3.session.CommandButton.SLOT_OVERFLOW)
            .setEnabled(true)
            .build()

        val prefs = listOf(muteBtn, stopBtn)
        assertEquals(2, prefs.size)

        // Slot 1: Mute custom command in SLOT_OVERFLOW
        assertEquals(AutoPlayPlaybackService.COMMAND_AUTO_PLAY_TOGGLE_MUTE, prefs[0].sessionCommand?.customAction)
        assertEquals(androidx.media3.common.Player.COMMAND_INVALID, prefs[0].playerCommand)
        assertTrue(prefs[0].slots.contains(androidx.media3.session.CommandButton.SLOT_OVERFLOW))

        // Slot 2: Stop custom command in SLOT_OVERFLOW
        assertEquals(AutoPlayPlaybackService.COMMAND_AUTO_PLAY_STOP, prefs[1].sessionCommand?.customAction)
        assertEquals(androidx.media3.common.Player.COMMAND_INVALID, prefs[1].playerCommand)
        assertTrue(prefs[1].slots.contains(androidx.media3.session.CommandButton.SLOT_OVERFLOW))
    }
}
