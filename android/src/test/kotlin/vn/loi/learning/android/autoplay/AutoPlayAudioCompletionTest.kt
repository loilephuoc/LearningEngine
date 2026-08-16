package vn.loi.learning.android.autoplay

import kotlin.test.assertEquals
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId

class AutoPlayAudioCompletionTest {

    private val sampleItem = AutoPlayItem(
        contentId = ContentId("item-audio-test"),
        headword = "banana",
        vietnameseMeaning = "quả chuối",
        wordAudioPath = "audio/banana.mp3",
        meaningAudioPath = "audio/qua_chuoi.mp3",
        exampleAudioPath = "audio/banana_ex.mp3",
        exampleTranslatedAudioPath = "audio/qua_chuoi_ex.mp3"
    )

    @Test
    fun `delay begins strictly AFTER real audio completion callback`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            frontDelayMs = 1500L,
            playFrontAudio = false,
            playAnswerAudio = true,
            postAnswerDelayMs = 750L
        )

        engine.start(listOf(sampleItem), config)

        // Front delay timer fires -> Moves to ANSWER_AUDIO
        assertEquals(1, scheduler.scheduledDelays.size)
        scheduler.fireNext()

        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.ANSWER_AUDIO, running.stage)

        // PROVE: At this point, post-answer delay is NOT scheduled yet
        assertEquals(1, scheduler.scheduledDelays.size, "Delay must not start while audio is still playing")

        // Trigger real audio completion
        audioPlayer.completeAudio()

        // PROVE: NOW the 750ms delay timer is scheduled
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_ANSWER_DELAY, running.stage)
        assertEquals(listOf(1500L, 750L), scheduler.scheduledDelays)
    }

    @Test
    fun `Vietnamese example audio delay begins strictly after completion`() {
        val audioPlayer = AutoPlayTimingStateMachineTest.FakeAudioPlayer()
        val scheduler = AutoPlayTimingStateMachineTest.FakeTimerScheduler()
        val engine = AutoPlayEngine(audioPlayer, scheduler)

        val config = AutoPlayConfig(
            frontDelayMs = 1000L,
            playFrontAudio = false,
            playAnswerAudio = false,
            playExampleEnglishAudio = false,
            playExampleVietnameseAudio = true,
            postExampleVietnameseDelayMs = 2500L
        )

        engine.start(listOf(sampleItem), config)

        // Front timer fires
        scheduler.fireNext()

        // Moves to EXAMPLE_VI_AUDIO
        var running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.EXAMPLE_VI_AUDIO, running.stage)
        assertEquals(1, scheduler.scheduledDelays.size)

        // Complete audio
        audioPlayer.completeAudio()

        // Now scheduled delay
        running = engine.state.value as AutoPlayEngineState.Running
        assertEquals(AutoPlayStage.POST_EXAMPLE_VI_DELAY, running.stage)
        assertEquals(listOf(1000L, 2500L), scheduler.scheduledDelays)
    }
}
