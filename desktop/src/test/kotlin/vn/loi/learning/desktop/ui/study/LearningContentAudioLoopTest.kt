package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LearningContentAudioLoopTest {
    @Test
    fun `completion schedules owned replay and current delay is read for every cycle`() {
        val player = ControlledPlayer()
        val scheduler = ControlledScheduler()
        val controller = LearningContentAudioController(player, scheduler)
        val path = Path.of("word.mp3")

        controller.startLoop(path, 0.2)
        assertEquals(listOf(path), player.played)
        player.emit(LearningContentAudioState.Completed(path))
        assertEquals(200, scheduler.delayMillis)
        controller.loopDelaySeconds = 1.5
        scheduler.runPending()
        assertEquals(listOf(path, path), player.played)
        player.emit(LearningContentAudioState.Completed(path))
        assertEquals(1500, scheduler.delayMillis)
    }

    @Test
    fun `stop switching stale completion failure play once and close cannot restart playback`() {
        val player = ControlledPlayer()
        val scheduler = ControlledScheduler()
        val controller = LearningContentAudioController(player, scheduler)
        val first = Path.of("first.mp3")
        val second = Path.of("second.mp3")

        controller.startLoop(first)
        player.emit(LearningContentAudioState.Completed(first))
        controller.startLoop(second)
        scheduler.runPending()
        player.emit(LearningContentAudioState.Completed(first))
        assertEquals(listOf(first, second), player.played)

        player.emit(LearningContentAudioState.Failed(second, "failure"))
        assertNull(controller.activeLoopPath)
        scheduler.runPending()
        assertEquals(listOf(first, second), player.played)

        controller.startLoop(first)
        player.emit(LearningContentAudioState.Completed(first))
        controller.playOnce(second)
        scheduler.runPending()
        assertEquals(second, player.played.last())
        assertNull(controller.activeLoopPath)

        controller.startLoop(first)
        player.emit(LearningContentAudioState.Completed(first))
        controller.close()
        scheduler.runPending()
        assertEquals(5, player.played.size)
    }

    private class ControlledScheduler : LearningAudioReplayScheduler {
        var delayMillis: Long? = null
        private var pending: (() -> Unit)? = null
        override fun schedule(delayMillis: Long, action: () -> Unit): AutoCloseable {
            this.delayMillis = delayMillis
            pending = action
            return AutoCloseable { if (pending === action) pending = null }
        }
        fun runPending() {
            val action = pending
            pending = null
            action?.invoke()
        }
    }

    private class ControlledPlayer : LearningContentAudioPlayer {
        private val listeners = mutableListOf<LearningContentAudioStateListener>()
        val played = mutableListOf<Path>()
        override var state: LearningContentAudioState = LearningContentAudioState.Idle
            private set
        override fun play(path: Path) {
            played.add(path)
            emit(LearningContentAudioState.Playing(path))
        }
        override fun stop() = emit(LearningContentAudioState.Idle)
        fun emit(next: LearningContentAudioState) {
            state = next
            listeners.toList().forEach { it.onStateChanged(next) }
        }
        override fun listen(listener: LearningContentAudioStateListener): AutoCloseable {
            listeners += listener
            return AutoCloseable { listeners -= listener }
        }
        override fun close() = listeners.clear()
    }
}
