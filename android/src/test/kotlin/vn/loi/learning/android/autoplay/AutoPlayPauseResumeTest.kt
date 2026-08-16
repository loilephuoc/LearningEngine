package vn.loi.learning.android.autoplay

import kotlin.test.assertEquals
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId

class AutoPlayPauseResumeTest {

    private val item = AutoPlayItem(
        contentId = ContentId("item-pause-test"),
        headword = "pause",
        vietnameseMeaning = "tạm dừng",
        wordAudioPath = "audio/pause.mp3",
        meaningAudioPath = "audio/tam_dung.mp3",
        exampleAudioPath = "audio/pause_ex.mp3",
        exampleTranslatedAudioPath = "audio/tam_dung_ex.mp3"
    )

    @Test
    fun `pause and resume during FRONT_WAIT resumes front timer`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        engine.start(listOf(item), AutoPlayConfig(frontDelayMs = 1500L, playFrontAudio = false))

        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)

        // Pause
        engine.pause()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.PAUSED, running.stage)

        // Resume
        engine.resume()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.FRONT_WAIT, running.stage)

        // Expire front timer -> moves to answer stage
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)
    }

    @Test
    fun `pause and resume during ANSWER_AUDIO restarts answer audio without skipping`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        engine.start(listOf(item), AutoPlayConfig(frontDelayMs = 1000L, playFrontAudio = false, playAnswerAudio = true))

        // Move to ANSWER_AUDIO
        scheduler.fireNext()
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)
        assertEquals(1, audioPlayer.playCount)

        // Pause
        val stopsBefore = audioPlayer.stopCount
        engine.pause()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.PAUSED, running.stage)
        assertEquals(stopsBefore + 1, audioPlayer.stopCount)

        // Resume
        engine.resume()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)
        assertEquals(2, audioPlayer.playCount)

        // Audio completes -> moves to post-answer delay
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_ANSWER_DELAY, running.stage)
    }

    @Test
    fun `pause and resume during POST_ANSWER_DELAY resumes decimal delay timer`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        engine.start(listOf(item), AutoPlayConfig(
            frontDelayMs = 500L,
            playFrontAudio = false,
            playAnswerAudio = true,
            postAnswerDelayMs = 1250L // 1.25s
        ))

        scheduler.fireNext() // moves to ANSWER_AUDIO
        audioPlayer.completeAudio() // moves to POST_ANSWER_DELAY

        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_ANSWER_DELAY, running.stage)

        // Pause
        engine.pause()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.PAUSED, running.stage)

        // Resume
        engine.resume()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_ANSWER_DELAY, running.stage)

        // Delay timer completes -> moves to Example EN
        scheduler.fireNext()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_EN_AUDIO, running.stage)
    }

    @Test
    fun `pause and resume during EXAMPLE_VI_AUDIO restarts Vietnamese example audio`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        engine.start(listOf(item), AutoPlayConfig(
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = false,
            playExampleEnglishAudio = false,
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 2500L // 2.5s
        ))

        scheduler.fireNext() // moves to EXAMPLE_VI_AUDIO
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_VI_AUDIO, running.stage)
        assertEquals(1, audioPlayer.playCount)

        // Pause
        engine.pause()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.PAUSED, running.stage)

        // Resume
        engine.resume()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_VI_AUDIO, running.stage)
        assertEquals(2, audioPlayer.playCount)

        // Complete -> POST_EXAMPLE_VI_DELAY
        audioPlayer.completeAudio()
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_VI_DELAY, running.stage)
    }
}
