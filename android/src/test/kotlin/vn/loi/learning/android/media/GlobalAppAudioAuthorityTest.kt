package vn.loi.learning.android.media

import android.media.AudioManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.autoplay.AutoPlayAudioPlayer
import vn.loi.learning.android.autoplay.AutoPlayRuntimeCoordinator
import vn.loi.learning.android.controller.ControllerAction
import vn.loi.learning.android.controller.ControllerActionDispatcher
import vn.loi.learning.android.controller.ControllerActionResult
import vn.loi.learning.android.controller.ControllerContext
import vn.loi.learning.android.controller.ControllerDiagnosticsHolder
import vn.loi.learning.android.controller.StudyAudioReason
import vn.loi.learning.android.controller.StudyControllerBridge
import vn.loi.learning.android.controller.StudyControllerTarget

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalAppAudioAuthorityTest {

    private class InMemoryAudioMutePreferenceStore(private var muted: Boolean = false) : AudioMutePreferenceStore {
        override fun loadMuted(): Boolean = muted
        override fun saveMuted(muted: Boolean) {
            this.muted = muted
        }
    }

    private class FakeAutoPlayAudioPlayer : AutoPlayAudioPlayer {
        var appliedMuted: Boolean = false
        private var _isMuted: Boolean = false
        override val isMuted: Boolean get() = _isMuted
        override val exoPlayer = null

        override fun play(path: String?, onComplete: () -> Unit) {}
        override fun stop() {}
        override fun setMuted(muted: Boolean) {
            this._isMuted = muted
            this.appliedMuted = muted
        }
    }

    private class FakeSystemMediaVolumeController(
        var volume: Int = 5,
        val min: Int = 0,
        val max: Int = 15,
        var isFixed: Boolean = false
    ) : SystemMediaVolumeController {
        var lastDirection: Int? = null
        var lastIsForeground: Boolean? = null

        override fun currentStatus(): SystemVolumeStatus {
            return SystemVolumeStatus(volume, min, max, isFixed)
        }

        override fun adjustVolume(direction: Int, isForeground: Boolean): SystemVolumeAdjustmentResult {
            lastDirection = direction
            lastIsForeground = isForeground
            if (isFixed) {
                return SystemVolumeAdjustmentResult.FixedVolume("System media volume is fixed")
            }
            val before = volume
            if (direction == AudioManager.ADJUST_RAISE) {
                if (before >= max) {
                    return SystemVolumeAdjustmentResult.Boundary(before, isMax = true, message = "Already at maximum media volume ($max)")
                }
                volume = before + 1
                return SystemVolumeAdjustmentResult.Success(before, volume, max, min, direction)
            } else {
                if (before <= min) {
                    return SystemVolumeAdjustmentResult.Boundary(before, isMax = false, message = "Already at minimum media volume ($min)")
                }
                volume = before - 1
                return SystemVolumeAdjustmentResult.Success(before, volume, max, min, direction)
            }
        }
    }

    private class FakeMuteablePlayer : MuteableAudioPlayer {
        var isMuted = false
        var applyMuteCount = 0

        override fun applyMute(muted: Boolean) {
            isMuted = muted
            applyMuteCount++
        }
    }

    private class FakeStudyTarget(
        private val primaryEn: String? = "test/primary_en.mp3",
        private val primaryVi: String? = "test/primary_vi.mp3",
        private val exampleEn: String? = "test/example_en.mp3",
        private val exampleVi: String? = "test/example_vi.mp3"
    ) : StudyControllerTarget {
        var revealed = false
        var continued = false

        override fun currentContext(): ControllerContext = ControllerContext.STUDY_QUESTION
        override suspend fun revealAnswer(): Boolean {
            revealed = true
            return true
        }
        override suspend fun continueCurrentMode(): Boolean {
            continued = true
            return true
        }
        override suspend fun rate(rating: vn.loi.learning.domain.study.memory.model.ReviewRating): Boolean = true
        override suspend fun next(): Boolean = true
        override suspend fun previous(): Boolean = true
        override fun replayAudio(): Boolean = true
        override fun primaryEnglishAudio(): String? = primaryEn
        override fun primaryVietnameseAudio(): String? = primaryVi
        override fun exampleEnglishAudio(): String? = exampleEn
        override fun exampleVietnameseAudio(): String? = exampleVi
    }

    @Before
    fun setUp() {
        LearningEngineAudioPolicy.resetForTesting(initialMuted = false)
        StudyControllerBridge.clear()
        ControllerDiagnosticsHolder.clear()
    }

    @Test
    fun `Section 21 - Global study mute immediately silences player and unmute restores audibility`() {
        val player = FakeMuteablePlayer()
        LearningEngineAudioPolicy.registerPlayer(player)
        assertFalse(player.isMuted)

        LearningEngineAudioPolicy.setMuted(true)
        assertTrue(player.isMuted)
        assertTrue(LearningEngineAudioPolicy.isMuted.value)

        LearningEngineAudioPolicy.setMuted(false)
        assertFalse(player.isMuted)
        assertFalse(LearningEngineAudioPolicy.isMuted.value)

        LearningEngineAudioPolicy.unregisterPlayer(player)
    }

    @Test
    fun `Section 22 - Global loop mute preserves active playback owner without restarting`() {
        val player = FakeMuteablePlayer()
        LearningEngineAudioPolicy.registerPlayer(player)

        val controller = object : AndroidAudioController(null) {
            var replayCount = 0
            var stopped = false
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (AndroidAudioPlaybackEvent) -> Unit,
                onState: (AndroidAudioState) -> Unit
            ): AndroidAudioState {
                replayCount++
                return AndroidAudioState.Playing
            }
            override fun stop() {
                stopped = true
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(controller)

        StudyControllerBridge.playAudio(
            itemKey = "item-1",
            path = "audio/loop.mp3",
            role = "EXPECTED_ANSWER",
            isLooping = true,
            reason = StudyAudioReason.MANUAL_LOOP
        )

        assertEquals(1, controller.replayCount)
        assertEquals("item-1", StudyControllerBridge.currentPlayback?.itemKey)
        assertTrue(StudyControllerBridge.currentPlayback?.isLooping == true)

        // Toggle mute
        LearningEngineAudioPolicy.toggleMuted()
        assertTrue(LearningEngineAudioPolicy.isMuted.value)
        assertTrue(player.isMuted)
        // Controller was NOT stopped or restarted
        assertFalse(controller.stopped)
        assertEquals(1, controller.replayCount)

        // Toggle unmute
        LearningEngineAudioPolicy.toggleMuted()
        assertFalse(LearningEngineAudioPolicy.isMuted.value)
        assertFalse(player.isMuted)
        assertFalse(controller.stopped)
        assertEquals(1, controller.replayCount)

        controller.close()
    }

    @Test
    fun `Section 23 - Example audio actions respect global mute`() {
        val target = FakeStudyTarget(exampleEn = "audio/ex_en.mp3", exampleVi = "audio/ex_vi.mp3")
        StudyControllerBridge.register(target)

        var playedPath: String? = null
        var playedReason: StudyAudioReason? = null
        var looping: Boolean? = null

        val audioMock = object : AndroidAudioController(null) {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (AndroidAudioPlaybackEvent) -> Unit,
                onState: (AndroidAudioState) -> Unit
            ): AndroidAudioState {
                playedPath = path
                looping = isLooping
                return AndroidAudioState.Playing
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        LearningEngineAudioPolicy.setMuted(true)

        // PLAY_EXAMPLE_EN
        assertTrue(StudyControllerBridge.playExampleEnglish())
        assertEquals("audio/ex_en.mp3", playedPath)
        assertEquals(false, looping)
        assertTrue(LearningEngineAudioPolicy.isMuted.value)

        // LOOP_EXAMPLE_EN
        assertTrue(StudyControllerBridge.loopExampleEnglish())
        assertEquals("audio/ex_en.mp3", playedPath)
        assertEquals(true, looping)

        // PLAY_EXAMPLE_VI
        assertTrue(StudyControllerBridge.playExampleVietnamese())
        assertEquals("audio/ex_vi.mp3", playedPath)
        assertEquals(false, looping)

        audioMock.close()
    }

    @Test
    fun `Section 24 - Background reveal loop mute silences and unmute restores audibility`() {
        StudyControllerBridge.onActivityForegroundChanged(false)

        var stopped = false
        val audioMock = object : AndroidAudioController(null) {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (AndroidAudioPlaybackEvent) -> Unit,
                onState: (AndroidAudioState) -> Unit
            ): AndroidAudioState = AndroidAudioState.Playing

            override fun stop() {
                stopped = true
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        StudyControllerBridge.playAudio("item-bg", "audio/reveal.mp3", "REVEAL", isLooping = true, reason = StudyAudioReason.REVEAL)

        val playback = StudyControllerBridge.currentPlayback
        assertEquals(false, playback?.startedWhileForeground)
        assertEquals(StudyAudioReason.REVEAL, playback?.reason)

        // Toggle mute
        LearningEngineAudioPolicy.toggleMuted()
        assertTrue(LearningEngineAudioPolicy.isMuted.value)
        assertFalse(stopped) // Reveal loop is muted without being killed

        // Toggle unmute
        LearningEngineAudioPolicy.toggleMuted()
        assertFalse(LearningEngineAudioPolicy.isMuted.value)
        assertFalse(stopped)

        audioMock.close()
    }

    @Test
    fun `Section 25 - AutoPlay mute is bidirectional with global app mute authority`() = runTest {
        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        val audioPlayer = FakeAutoPlayAudioPlayer()
        val coordinator = AutoPlayRuntimeCoordinator(
            audioPlayer = audioPlayer,
            scope = testScope
        )

        assertFalse(coordinator.isMuted.value)
        assertFalse(LearningEngineAudioPolicy.isMuted.value)

        // Mute from global policy
        LearningEngineAudioPolicy.setMuted(true)
        testScheduler.advanceUntilIdle()
        assertTrue(coordinator.isMuted.value)
        assertTrue(audioPlayer.appliedMuted)
        assertTrue(coordinator.config.value.isMuted)

        // Unmute from coordinator UI / notification
        coordinator.toggleMute()
        testScheduler.advanceUntilIdle()
        assertFalse(coordinator.isMuted.value)
        assertFalse(LearningEngineAudioPolicy.isMuted.value)
        assertFalse(audioPlayer.appliedMuted)
        assertFalse(coordinator.config.value.isMuted)
    }

    @Test
    fun `Section 26 - Manual loop started in foreground stops upon Home exit while background pocket learning works`() {
        // App is in foreground
        StudyControllerBridge.onActivityForegroundChanged(true)

        var stoppedReason: StudyAudioReason? = null
        val audioMock = object : AndroidAudioController(null) {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (AndroidAudioPlaybackEvent) -> Unit,
                onState: (AndroidAudioState) -> Unit
            ): AndroidAudioState = AndroidAudioState.Playing

            override fun stop() {
                stoppedReason = StudyControllerBridge.currentPlayback?.reason
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        // User starts manual loop in foreground
        StudyControllerBridge.playAudio("item-fg", "audio/primary.mp3", "EXPECTED_ANSWER", isLooping = true, reason = StudyAudioReason.MANUAL_LOOP)
        val playback = StudyControllerBridge.currentPlayback
        assertTrue(playback?.startedWhileForeground == true)
        assertEquals(StudyAudioReason.MANUAL_LOOP, playback?.reason)

        // User presses Home -> Activity STOPPED
        StudyControllerBridge.onActivityForegroundChanged(false)

        // Manual loop must be stopped
        assertNull(StudyControllerBridge.currentPlayback)

        // Now user presses Reveal on physical controller while still background
        StudyControllerBridge.playAudio("item-bg-2", "audio/reveal.mp3", "REVEAL", isLooping = true, reason = StudyAudioReason.REVEAL)
        val bgPlayback = StudyControllerBridge.currentPlayback
        assertEquals(false, bgPlayback?.startedWhileForeground)
        assertEquals(StudyAudioReason.REVEAL, bgPlayback?.reason)

        // Continue commits and plays entry audio
        StudyControllerBridge.stopAudio(StudyAudioReason.CONTINUE_EXIT)
        StudyControllerBridge.playAudio("item-bg-3", "audio/entry.mp3", "ITEM_ENTRY", isLooping = false, reason = StudyAudioReason.ITEM_ENTRY)
        val entryPlayback = StudyControllerBridge.currentPlayback
        assertEquals(false, entryPlayback?.startedWhileForeground)
        assertEquals(StudyAudioReason.ITEM_ENTRY, entryPlayback?.reason)

        audioMock.close()
    }

    @Test
    fun `Section 27 - Example loop started in foreground stops upon Home exit and does not auto-resume`() {
        StudyControllerBridge.onActivityForegroundChanged(true)

        val target = FakeStudyTarget(exampleEn = "audio/ex.mp3")
        StudyControllerBridge.register(target)

        val audioMock = object : AndroidAudioController(null) {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (AndroidAudioPlaybackEvent) -> Unit,
                onState: (AndroidAudioState) -> Unit
            ): AndroidAudioState = AndroidAudioState.Playing
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        StudyControllerBridge.loopExampleEnglish()
        assertTrue(StudyControllerBridge.currentPlayback?.startedWhileForeground == true)
        assertEquals(StudyAudioReason.MANUAL_LOOP, StudyControllerBridge.currentPlayback?.reason)

        // Press Home
        StudyControllerBridge.onActivityForegroundChanged(false)
        assertNull(StudyControllerBridge.currentPlayback)

        // Return to foreground
        StudyControllerBridge.onActivityForegroundChanged(true)
        assertNull(StudyControllerBridge.currentPlayback) // Old loop does NOT resume automatically

        audioMock.close()
    }

    @Test
    fun `Section 29 - System volume UP increases media volume and returns Executed`() = runTest {
        val volumeCtrl = FakeSystemMediaVolumeController(volume = 5, max = 15)
        val dispatcher = ControllerActionDispatcher(
            appContext = FakeAndroidContext(),
            systemMediaVolumeController = volumeCtrl
        )

        val result = dispatcher.dispatch(ControllerAction.SYSTEM_VOLUME_UP, ControllerContext.GLOBAL)
        assertTrue(result is ControllerActionResult.Executed)
        assertEquals(6, volumeCtrl.volume)
        assertEquals(AudioManager.ADJUST_RAISE, volumeCtrl.lastDirection)
    }

    @Test
    fun `Section 30 - System volume DOWN decreases media volume and returns Executed`() = runTest {
        val volumeCtrl = FakeSystemMediaVolumeController(volume = 5, min = 0)
        val dispatcher = ControllerActionDispatcher(
            appContext = FakeAndroidContext(),
            systemMediaVolumeController = volumeCtrl
        )

        val result = dispatcher.dispatch(ControllerAction.SYSTEM_VOLUME_DOWN, ControllerContext.GLOBAL)
        assertTrue(result is ControllerActionResult.Executed)
        assertEquals(4, volumeCtrl.volume)
        assertEquals(AudioManager.ADJUST_LOWER, volumeCtrl.lastDirection)
    }

    @Test
    fun `Section 31 - System volume at boundary reports truthful result`() = runTest {
        val volumeCtrl = FakeSystemMediaVolumeController(volume = 15, max = 15, min = 0)
        val dispatcher = ControllerActionDispatcher(
            appContext = FakeAndroidContext(),
            systemMediaVolumeController = volumeCtrl
        )

        // At max
        val resultUp = dispatcher.dispatch(ControllerAction.SYSTEM_VOLUME_UP, ControllerContext.GLOBAL)
        assertTrue(resultUp is ControllerActionResult.Executed)
        assertEquals("System: Already at maximum media volume (15)", (resultUp as ControllerActionResult.Executed).target)

        // At min
        volumeCtrl.volume = 0
        val resultDown = dispatcher.dispatch(ControllerAction.SYSTEM_VOLUME_DOWN, ControllerContext.GLOBAL)
        assertTrue(resultDown is ControllerActionResult.Executed)
        assertEquals("System: Already at minimum media volume (0)", (resultDown as ControllerActionResult.Executed).target)
    }

    @Test
    fun `Section 32 - System fixed volume returns UnavailableInContext`() = runTest {
        val volumeCtrl = FakeSystemMediaVolumeController(volume = 8, isFixed = true)
        val dispatcher = ControllerActionDispatcher(
            appContext = FakeAndroidContext(),
            systemMediaVolumeController = volumeCtrl
        )

        val result = dispatcher.dispatch(ControllerAction.SYSTEM_VOLUME_UP, ControllerContext.GLOBAL)
        assertTrue(result is ControllerActionResult.UnavailableInContext)
        assertEquals("System media volume is fixed", (result as ControllerActionResult.UnavailableInContext).reason)
    }

    private class FakeAndroidContext : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): android.content.Context = this
    }
}
