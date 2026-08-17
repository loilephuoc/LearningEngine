package vn.loi.learning.android.autoplay

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId

class AutoPlayStaleCallbackTest {

    private val item1 = AutoPlayItem(
        contentId = ContentId("item-1"),
        headword = "one",
        vietnameseMeaning = "số một",
        wordAudioPath = "audio/one.mp3",
        meaningAudioPath = "audio/mot.mp3",
        exampleAudioPath = "audio/one_ex.mp3",
        exampleTranslatedAudioPath = "audio/mot_ex.mp3"
    )

    private val item2 = AutoPlayItem(
        contentId = ContentId("item-2"),
        headword = "two",
        vietnameseMeaning = "số hai",
        wordAudioPath = "audio/two.mp3",
        meaningAudioPath = "audio/hai.mp3",
        exampleAudioPath = "audio/two_ex.mp3",
        exampleTranslatedAudioPath = "audio/hai_ex.mp3"
    )

    @Test
    fun `next during ANSWER_AUDIO invalidates stale audio callback`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        engine.start(
            listOf(item1, item2),
            AutoPlayConfig(
                playbackOrder = AutoPlayPlaybackOrder.SOURCE_ORDER,
                frontDelayMs = 1500L,
                playFrontAudio = false,
                playAnswerAudio = true
            )
        )

        // Move to ANSWER_AUDIO for item1
        scheduler.fireNext()
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals("one", running.item.headword)
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)

        // Capture stale audio callback for item1
        val staleCallback = audioPlayer.currentCallback

        // User hits NEXT -> immediately moves to item2 FRONT_WAIT
        engine.next()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals("two", running.item.headword)
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)

        // Stale audio callback from item1 fires now
        staleCallback?.invoke()

        // PROVE: State is still item2 FRONT_WAIT, NOT corrupted into item1 post-answer delay
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals("two", running.item.headword)
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)
    }

    @Test
    fun `stop during EXAMPLE_VI_AUDIO cancels and invalidates stale timer`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        engine.start(listOf(item1), AutoPlayConfig(
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = false,
            playExampleEnglishAudio = false,
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 2000L
        ))

        // Move to EXAMPLE_VI_AUDIO -> complete -> POST_EXAMPLE_VI_DELAY
        scheduler.fireNext()
        audioPlayer.completeAudio()
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_VI_DELAY, running.stage)

        val staleTimerCallback = scheduler.scheduledCallbacks.firstOrNull()

        // User hits STOP -> engine becomes IDLE
        engine.stop()
        assertTrue(engine.state.value is AutoPlayEngineState.Idle)

        // Stale timer callback fires
        staleTimerCallback?.invoke()

        // PROVE: Engine remains IDLE, does not advance or crash
        assertTrue(engine.state.value is AutoPlayEngineState.Idle)
    }
}
