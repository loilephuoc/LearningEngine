package vn.loi.learning.android.study

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.controller.StudyAudioReason
import vn.loi.learning.android.controller.StudyControllerBridge
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioPlaybackEvent
import vn.loi.learning.android.media.AndroidAudioState

class StudyForegroundAudioLifecycleTest {
    private lateinit var audio: RecordingAudioController

    @Before
    fun setUp() {
        StudyControllerBridge.clear()
        audio = RecordingAudioController()
        StudyControllerBridge.registerBackgroundAudioController(audio)
    }

    @After
    fun tearDown() {
        StudyControllerBridge.clear()
        audio.close()
    }

    @Test
    fun `foreground loss stops word and example loops plus one-shot and autoplay`() {
        listOf(
            Triple("word.mp3", "EXPECTED_ANSWER", StudyAudioReason.MANUAL_LOOP),
            Triple("example.mp3", "EXAMPLE_EN", StudyAudioReason.MANUAL_LOOP),
            Triple("manual.mp3", "EXPECTED_ANSWER", StudyAudioReason.MANUAL_PLAY),
            Triple("autoplay.mp3", "MEANING", StudyAudioReason.COMPOSE_AUTOPLAY)
        ).forEachIndexed { index, (path, role, reason) ->
            StudyControllerBridge.onActivityForegroundChanged(true)
            assertTrue(StudyControllerBridge.playAudio("item-$index", path, role, reason == StudyAudioReason.MANUAL_LOOP, reason))

            StudyControllerBridge.onActivityForegroundChanged(false)

            assertNull(StudyControllerBridge.currentPlayback)
            assertEquals(index + 1, audio.stopCount)
        }
    }

    @Test
    fun `foreground regain does not restart old loop and permits explicit new playback`() {
        assertTrue(StudyControllerBridge.playAudio("item-a", "old.mp3", "EXPECTED_ANSWER", true, StudyAudioReason.MANUAL_LOOP))
        StudyControllerBridge.onActivityForegroundChanged(false)
        val playCountAfterStop = audio.played.size

        StudyControllerBridge.onActivityForegroundChanged(true)
        assertEquals(playCountAfterStop, audio.played.size)
        assertTrue(StudyControllerBridge.playAudio("item-a", "new.mp3", "EXPECTED_ANSWER", false, StudyAudioReason.MANUAL_PLAY))
        assertEquals("new.mp3", audio.played.last())
    }

    @Test
    fun `leaving Study surface stops playback and returning permits a new request`() {
        assertTrue(StudyControllerBridge.playAudio("item-a", "study.mp3", "EXPECTED_ANSWER", true, StudyAudioReason.MANUAL_LOOP))

        StudyControllerBridge.onStudySurfaceChanged(false)

        assertEquals(1, audio.stopCount)
        assertNull(StudyControllerBridge.currentPlayback)

        StudyControllerBridge.onStudySurfaceChanged(true)
        assertTrue(StudyControllerBridge.playAudio("item-b", "fresh.mp3", "EXPECTED_ANSWER", false, StudyAudioReason.MANUAL_PLAY))
        assertEquals("fresh.mp3", audio.played.last())
    }

    @Test
    fun `loop started after live replan surface recreation still stops on Home`() {
        StudyControllerBridge.onStudySurfaceChanged(false)
        StudyControllerBridge.onStudySurfaceChanged(true)
        val stopsBeforePlayback = audio.stopCount
        assertTrue(StudyControllerBridge.playAudio("reentered-new", "after-replan.mp3", "EXPECTED_ANSWER", true, StudyAudioReason.MANUAL_LOOP))

        StudyControllerBridge.onActivityForegroundChanged(false)

        assertEquals(stopsBeforePlayback + 1, audio.stopCount)
        assertNull(StudyControllerBridge.currentPlayback)
    }

    @Test
    fun `failed update Retry recreation invalidates old loop and new Retry playback remains foreground owned`() {
        assertTrue(StudyControllerBridge.playAudio("before-failure", "old.mp3", "EXPECTED_ANSWER", true, StudyAudioReason.MANUAL_LOOP))
        StudyControllerBridge.onStudySurfaceChanged(false)
        assertEquals(1, audio.stopCount)
        assertNull(StudyControllerBridge.currentPlayback)

        StudyControllerBridge.onStudySurfaceChanged(true)
        assertTrue(StudyControllerBridge.playAudio("retry-item", "retry.mp3", "EXAMPLE_EN", true, StudyAudioReason.MANUAL_LOOP))
        StudyControllerBridge.onActivityForegroundChanged(false)

        assertEquals(2, audio.stopCount)
        assertNull(StudyControllerBridge.currentPlayback)
        StudyControllerBridge.onActivityForegroundChanged(true)
        assertEquals(2, audio.played.size, "Foreground regain must not replay the old loop")
        assertTrue(StudyControllerBridge.playAudio("retry-item", "fresh.mp3", "EXPECTED_ANSWER", false, StudyAudioReason.MANUAL_PLAY))
        assertEquals("fresh.mp3", audio.played.last())
    }

    private class RecordingAudioController : AndroidAudioController() {
        val played = mutableListOf<String?>()
        var stopCount = 0

        override fun replay(
            path: String?,
            isLooping: Boolean,
            onPlaybackEvent: (AndroidAudioPlaybackEvent) -> Unit,
            onState: (AndroidAudioState) -> Unit
        ): AndroidAudioState {
            played += path
            return AndroidAudioState.Playing
        }

        override fun stop() {
            stopCount++
        }
    }
}
