package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypingSuccessOrchestrationTest {
    @Test
    fun `realtime success debounce is cancellable before confirmation`() = runBlocking {
        val completed = CompletableDeferred<Unit>()
        val job =
            launch {
                awaitTypingRealtimeSuccessDebounce()
                completed.complete(Unit)
            }
        yield()

        job.cancelAndJoin()

        assertFalse(completed.isCompleted)
    }

    @Test
    fun `answer audio completion is awaited through existing player listener`() = runBlocking {
        val player = ControlledPlayer(completeOnPlay = true)
        val controller = LearningContentAudioController(player)
        val answer = Path.of("answer.mp3")

        awaitTypingAnswerAudio(controller, answer)

        assertEquals(listOf(answer), player.played)
        assertFalse(player.listenerRegistered)
        controller.close()
    }

    @Test
    fun `audio failure releases success flow without retrying`() = runBlocking {
        val player = ControlledPlayer(failOnPlay = true)
        val controller = LearningContentAudioController(player)

        awaitTypingAnswerAudio(controller, Path.of("missing.mp3"))

        assertEquals(1, player.played.size)
        assertFalse(player.listenerRegistered)
        controller.close()
    }

    @Test
    fun `cancellation stops audio and makes a late completion inert`() = runBlocking {
        val player = ControlledPlayer()
        val controller = LearningContentAudioController(player)
        val job = launch { awaitTypingAnswerAudio(controller, Path.of("old.mp3")) }
        yield()

        job.cancelAndJoin()
        player.completeLatest()

        assertTrue(player.stopCount > 0)
        assertFalse(player.listenerRegistered)
        controller.close()
    }

    private class ControlledPlayer(
        private val completeOnPlay: Boolean = false,
        private val failOnPlay: Boolean = false
    ) : LearningContentAudioPlayer {
        private var listener: LearningContentAudioStateListener? = null
        private var latestPath: Path? = null
        val played = mutableListOf<Path>()
        var stopCount = 0
            private set
        val listenerRegistered: Boolean
            get() = listener != null

        override var state: LearningContentAudioState = LearningContentAudioState.Idle
            private set

        override fun play(path: Path) {
            played.add(path)
            latestPath = path.toAbsolutePath().normalize()
            state =
                if (failOnPlay) {
                    LearningContentAudioState.Failed(requireNotNull(latestPath), "unavailable")
                } else {
                    LearningContentAudioState.Playing(requireNotNull(latestPath))
                }
            listener?.onStateChanged(state)
            if (completeOnPlay) completeLatest()
        }

        fun completeLatest() {
            val path = latestPath ?: return
            state = LearningContentAudioState.Completed(path)
            listener?.onStateChanged(state)
        }

        override fun stop() {
            stopCount++
            state = LearningContentAudioState.Idle
            listener?.onStateChanged(state)
        }

        override fun listen(listener: LearningContentAudioStateListener): AutoCloseable {
            this.listener = listener
            listener.onStateChanged(state)
            return AutoCloseable {
                if (this.listener === listener) this.listener = null
            }
        }

        override fun close() {
            stop()
            listener = null
        }
    }
}
